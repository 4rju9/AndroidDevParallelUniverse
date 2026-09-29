package app.netlify.dev4rju9.rootdetection

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Log
import androidx.annotation.WorkerThread
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Evaluates whether this device/process is a trustworthy environment to run
 * in. Event codes (EVT_1xxx below, EVT_2xxx in DeviceStateBridge's native
 * layer) are deliberately opaque rather than self-describing - see
 * proguard-rules.pro's "Root/tamper detection" section for why: a
 * descriptive string constant is a free hint in a static dex/so dump
 * regardless of what the surrounding class/method got renamed to.
 *
 * EVT_1001 su-style binary found on a known path
 * EVT_1002 su-style binary found via a PATH sweep
 * EVT_1003 superuser-manager artifact found
 * EVT_1004 Magisk/KernelSU/APatch artifact found
 * EVT_1005 hook-framework artifact found
 * EVT_1006 a watched package is installed
 * EVT_1007 a watched module name is mapped into this process
 * EVT_1008 /proc/self/maps unreadable
 * EVT_1009 a watched thread name is present
 * EVT_1010 a protected partition is mounted rw
 * EVT_1011 an overlay/tmpfs mount on a protected partition
 * EVT_1012 verified boot state is orange/red
 * EVT_1013 bootloader unlocked
 * EVT_1014 secure boot unlocked
 * EVT_1015 dm-verity disabled
 * EVT_1016 a path-resolution probe found a hit
 * EVT_1017 an exec probe succeeded as uid 0
 * EVT_1018 build tags include test-keys
 * EVT_1019 non-production build properties
 * EVT_1020 adb root property set
 * EVT_1021 policy enforcement disabled/permissive
 * EVT_1022 a utility binary was found where it shouldn't be
 * EVT_1023 emulator hardware profile or companion package detected
 * EVT_1024 emulator confirmation (bumps past the weak threshold)
 * EVT_1025 a tracer is attached to this process
 * EVT_1026 a watched local port is listening
 * EVT_1027 evaluation exceeded its time budget
 * EVT_1028 signing certificate does not match the expected one
 * EVT_1029 startup pre-check: virtual environment detected
 * EVT_1030 startup pre-check: full evaluation or local port probe flagged
 * EVT_1031 startup pre-check: check chain completed implausibly fast
 */
object DeviceStateUtils {

    private const val TAG = "DeviceStateUtils"

    private const val WEAK_SIGNAL_THRESHOLD = 2

    private val BLOCK_EMULATORS = true

    /**
     * `su -c id` actually requests root.
     * On a Magisk device this pops a Superuser grant dialog and blocks the launch
     * thread until the user answers (or the 20s Magisk timeout expires).
     * The path/which/maps checks already cover su, so this is OFF by default.
     * Turn it on only if your auditor explicitly asks for an execution attempt.
     */
    private const val ENABLE_DEEP_SCAN = false

    /**
     * `which su` duplicates what scanBinaryPaths/scanPathEnv already stat, and is
     * the only remaining fork on a normal launch. Set false for an exec-free path.
     */
    private const val ENABLE_PATH_SCAN = true

    /**
     * Self-ptrace tracer probe (native side, see DeviceStateBridge/native-lib.cpp).
     * A second PTRACE_TRACEME failing with EPERM is strong evidence a tracer
     * is already attached — but some OEM builds run apps under a seccomp
     * policy that denies ptrace outright, which reads identically and would
     * false-positive on a clean, non-rooted device. Off by default; turn on
     * only after confirming on your target OEM/Android-version matrix.
     */
    private const val ENABLE_NATIVE_DEEP_PROBE = false

    /**
     * SHA-256 of the release signing certificate, lowercase hex, no separators.
     * Catches a class of tamper this file otherwise can't see: someone decompiles
     * the APK, patches this file out, and re-signs it with their own key — every
     * other check here can still "pass" because the device may be perfectly
     * clean, but the app is no longer the one you shipped.
     *
     * Get it with:
     *   keytool -list -v -keystore app/bluestarac.jks -alias bluestaracandroidkey
     * then take the "SHA256:" line, strip the colons, lowercase it.
     *
     * Left blank on purpose: unset, this check is skipped rather than guessed —
     * it can only ever add a missed detection, never a false positive.
     */
    private const val EXPECTED_SIGNING_HASH = ""

    private const val EXEC_TIMEOUT_MS = 1500L
    const val EVALUATE_BUDGET_MS = 4000L

    private val execPool: ExecutorService by lazy {
        Executors.newCachedThreadPool { r ->
            // Deliberately NOT the log TAG: SELinux denies some of the reads
            // below on hardened devices, and the kernel's own avc denial
            // audit-log line gets attributed to the calling *thread's name*
            // regardless of anything this file logs itself - so reusing a
            // descriptive TAG here would leak through `adb logcat` on any
            // enforcing device, debug or release, independent of every
            // debugLog{}/BuildConfig.DEBUG guard in this file.
            Thread(r, "app-worker").apply {
                isDaemon = true
                priority = Thread.MIN_PRIORITY
            }
        }
    }

    // ==================================================================
    // Signature data
    // ==================================================================

    private val BINARY_PATHS = arrayOf(
        "/sbin/su",
        "/su/bin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/system/sbin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/system/bin/.ext/.su",
        "/system/usr/we-need-root/su-backup",
        "/vendor/bin/su",
        "/product/bin/su",
        "/apex/com.android.runtime/bin/su",
        "/data/local/su",
        "/data/local/bin/su",
        "/data/local/xbin/su",
        "/debug_ramdisk/su",
        "/magisk/.core/bin/su"
    )

    private val PATH_DIRS = arrayOf(
        "/sbin/",
        "/system/bin/",
        "/system/xbin/",
        "/system/sbin/",
        "/vendor/bin/",
        "/product/bin/",
        "/data/local/bin/",
        "/data/local/xbin/",
        "/su/bin/"
    )

    private val INSTALLED_ARTIFACT_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/system/app/SuperSU/SuperSU.apk",
        "/system/etc/init.d/99SuperSUDaemon",
        "/system/xbin/daemonsu",
        "/system/bin/.ext",
        "/dev/com.koushikdutta.superuser.daemon/"
    )

    private val UTILITY_BINARY_PATHS = arrayOf(
        "/system/bin/busybox",
        "/system/xbin/busybox",
        "/sbin/busybox",
        "/vendor/bin/busybox",
        "/data/local/xbin/busybox"
    )

    private val SYSTEM_ARTIFACT_PATHS = arrayOf(
        "/sbin/magisk",
        "/sbin/.magisk",
        "/sbin/magiskhide",
        "/system/bin/magisk",
        "/data/adb/magisk",
        "/data/adb/magisk.db",
        "/data/adb/modules",
        "/data/adb/magisk_simple",
        "/cache/.disable_magisk",
        "/cache/magisk.log",
        "/dev/.magisk.unblock",
        // KernelSU
        "/data/adb/ksu",
        "/data/adb/ksud",
        // APatch
        "/data/adb/ap",
        "/data/adb/apd"
    )

    private val FRAMEWORK_ARTIFACT_PATHS = arrayOf(
        "/system/framework/XposedBridge.jar",
        "/system/bin/app_process_xposed",
        "/system/lib/libxposed_art.so",
        "/system/lib64/libxposed_art.so",
        "/data/adb/lspd",
        "/data/adb/riru",
        "/data/misc/riru",
        "/data/adb/modules/riru-core",
        "/system/lib/libsubstrate.so",
        "/system/lib/libsubstrate-dvm.so"
    )

    private val WATCHED_PACKAGES = arrayOf(
        // Magisk and forks / repackages
        "com.topjohnwu.magisk",
        "io.github.huskydg.magisk",
        "io.github.vvb2060.magisk",
        // KernelSU / APatch
        "me.weishu.kernelsu",
        "me.bmax.apatch",
        // Classic superuser managers
        "com.koushikdutta.superuser",
        "eu.chainfire.supersu",
        "com.noshufou.android.su",
        "com.noshufou.android.su.elite",
        "com.thirdparty.superuser",
        "com.yellowes.su",
        "com.zachspong.temprootremovejb",
        // One-click rooting tools
        "com.kingroot.kinguser",
        "com.kingo.root",
        "com.smedialink.oneclickroot",
        "com.zhiqupk.root.global",
        "com.alephzain.framaroot",
        "com.baidu.easyroot",
        // Hooking frameworks
        "de.robv.android.xposed.installer",
        "org.lsposed.manager",
        "org.meowcat.edxposed.manager",
        "com.saurik.substrate",
        // Root cloakers / detection evaders
        "com.devadvance.rootcloak",
        "com.devadvance.rootcloakplus",
        "com.formyhm.hideroot",
        "com.formyhm.hiderootPremium",
        "com.amphoras.hidemyroot",
        "com.amphoras.hidemyrootadfree",
        "com.ramdroid.appquarantine"
    )

    /** Substrings searched in /proc/self/maps. */
    private val WATCHED_MODULE_NAMES = arrayOf(
        "frida",
        "frida-agent",
        "frida-gadget",
        "linjector",
        "magisk",
        "zygisk",
        "riru",
        "libriru",
        "lspd",
        "lsposed",
        "xposed",
        "substrate",
        "shadowhook",
        "sandhook",
        "libmemtrack_real",
        "epic.so",
        "whale.so",
        "dexposed"
    )

    /** Thread names created by frida-gadget / frida-server, from /proc/self/task/N/comm. */
    private val WATCHED_THREAD_NAMES = arrayOf(
        "gum-js-loop", "gmain", "gdbus", "pool-frida", "linjector"
    )

    /** Frida's default listening ports. */
    private val WATCHED_PORTS = intArrayOf(27042, 27043)

    // Emulator signatures, all matched lowercase. Split by how much a single
    // hit proves:
    //  - DEFINITIVE: names/artifacts that only ever exist in an emulator
    //    image. One hit is enough.
    //  - HEURISTIC: loose patterns (generic/unknown values, "emulator" in a
    //    name) that a retail ROM can produce by accident. Only counted when
    //    at least EMULATOR_HEURISTIC_THRESHOLD distinct ones agree.
    // Hardware/board values are token-matched (equal, or prefix followed by a
    // separator/digit), never substring-matched: "nox" as a substring hits
    // Samsung's "knox", and "vbox86" would hit any value merely containing it.
    // "qemu.hw.mainkeys" is deliberately absent everywhere - custom ROMs on
    // real phones set it to show a navbar.
    private const val EMULATOR_HEURISTIC_THRESHOLD = 2

    private val EMULATOR_HARDWARE_TOKENS = arrayOf(
        "goldfish", "ranchu", "vbox86", "ttvm_x86", "nox", "cutf_cvm", "android_x86",
        "waydroid", "anbox", "redroid", "genymotion"
    )

    /** Build.MODEL prefixes used only by AOSP/Google emulator images. */
    private val EMULATOR_MODEL_PREFIXES = arrayOf(
        "google_sdk", "android sdk built for", "sdk_gphone", "sdk_google"
    )

    /** Build.PRODUCT prefixes used only by AOSP/Google/Genymotion emulator images. */
    private val EMULATOR_PRODUCT_PREFIXES = arrayOf(
        "google_sdk", "sdk_gphone", "sdk_google", "sdk_x86", "vbox86p", "aosp_cf_"
    )

    /** Build.MANUFACTURER values used only by emulator vendors (exact match). */
    private val EMULATOR_MANUFACTURERS = arrayOf("genymotion", "genymobile")

    private val EMULATOR_HARDWARE_PROPS = arrayOf(
        "ro.hardware", "ro.boot.hardware", "ro.product.board", "ro.board.platform"
    )

    /** Properties whose mere presence means an emulator image. */
    private val EMULATOR_PRESENCE_PROPS = arrayOf(
        "init.svc.qemud",
        "init.svc.qemu-props",
        "ro.kernel.android.qemud",
        "init.svc.vbox86-setup",
        "init.svc.noxd",
        "init.svc.microvirtd",
        "init.svc.ldinit"
    )

    private val EMULATOR_FILE_PATHS = arrayOf(
        // Android Studio / AOSP goldfish & ranchu
        "/dev/socket/qemud",
        "/dev/qemu_pipe",
        "/dev/goldfish_pipe",
        "/sys/qemu_trace",
        "/system/bin/qemu-props",
        // VirtualBox-based (Genymotion, Nox, MEmu, older BlueStacks)
        "/dev/vboxguest",
        "/dev/vboxuser",
        "/sys/module/vboxguest",
        "/system/bin/androVM-prop",
        // Genymotion
        "/dev/socket/genyd",
        "/dev/socket/baseband_genyd",
        "/system/bin/genybaseband",
        // Nox
        "/system/bin/nox-prop",
        "/system/bin/nox-vbox-sf",
        "/system/bin/noxd",
        "/system/lib/libnoxd.so",
        "/system/lib/libnoxspeedup.so",
        // BlueStacks
        "/data/.bluestacks.prop",
        "/system/bin/bstshutdown",
        "/boot/bstmods",
        "/boot/bstsetup.env",
        "/system/xbin/bstk",
        // LDPlayer
        "/system/bin/ldinit",
        "/system/bin/ldmountsf",
        "/system/lib/libldutils.so",
        // MEmu (Microvirt)
        "/system/bin/microvirtd",
        "/system/bin/microvirt-prop",
        // Droid4X / Tiantian / Windroy
        "/system/lib/libdroid4x.so",
        "/system/bin/droid4x-prop",
        "/system/bin/ttVM-prop",
        "/system/bin/windroyed"
    )

    /** Must stay in sync with the EMULATOR_PACKAGES block of <queries> in AndroidManifest.xml. */
    private val EMULATOR_PACKAGES = arrayOf(
        "com.bluestacks.appmart",
        "com.bluestacks.home",
        "com.bluestacks.settings",
        "com.bluestacks.BstCommandProcessor",
        "com.bignox.app",
        "com.bignox.google.installer",
        "com.vphone.launcher",
        "com.microvirt.launcher",
        "com.microvirt.launcher2",
        "com.microvirt.tools",
        "com.microvirt.guide",
        "com.microvirt.market",
        "com.ldmnq.launcher3",
        "com.android.ld.appstore",
        "com.mumu.launcher",
        "com.mumu.store",
        "com.genymotion.superuser",
        "com.genymotion.systempatcher",
        "com.kaopu001.tiantianserver",
        "com.tiantian.ime"
    )

    /** Partitions that must never be mounted read-write on a user build. */
    private val PROTECTED_MOUNTS = arrayOf(
        "/system", "/system_root", "/vendor", "/product", "/odm", "/etc"
    )

    private val PROP_REGEX = Regex("""^\[(.+?)]:\s*\[(.*?)]$""")

    private val PROPS_OF_INTEREST = arrayOf(
        "ro.debuggable",
        "ro.secure",
        "ro.build.type",
        "ro.build.selinux",
        "ro.adb.secure",
        "service.adb.root",
        "ro.boot.verifiedbootstate",
        "ro.boot.flash.locked",
        "ro.secureboot.lockstate",
        "ro.boot.veritymode"
    )

    // ==================================================================
    // Result model
    // ==================================================================

    data class EvaluationResult @JvmOverloads constructor(
        val isFlagged: Boolean,
        val strongSignals: List<String>,
        val weakSignals: List<String>,
        /** Wall-clock time of the check chain; only set by [evaluateStartupState]. */
        val elapsedMs: Long = 0L
    ) {
        val allSignals: List<String> get() = strongSignals + weakSignals
        fun summary(): String = allSignals.joinToString(", ").ifEmpty { "clean" }
    }

    // ==================================================================
    // Public entry points
    // ==================================================================

    @JvmStatic
    @JvmOverloads
    @WorkerThread
    private fun evaluateWithBudget(context: Context, budgetMs: Long = EVALUATE_BUDGET_MS): EvaluationResult {
        val appContext = context.applicationContext
        var future: Future<EvaluationResult>? = null
        return try {
            future = execPool.submit(Callable { evaluate(appContext) })
            future.get(budgetMs, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            debugLog { "evaluate() exceeded ${budgetMs}ms budget — failing open" }
            EvaluationResult(false, emptyList(), listOf("EVT_1027"))
        } catch (t: Throwable) {
            debugLog { "evaluate() failed: ${t.localizedMessage}" }
            EvaluationResult(false, emptyList(), emptyList())
        } finally {
            future?.cancel(true)
        }
    }

    @JvmStatic
    @WorkerThread
    fun isEnvironmentUntrusted(context: Context): Boolean {
        val flagged = evaluateWithBudget(context).isFlagged
        if (flagged) debugLog { "Environment is untrusted" }
        else debugLog { "Environment is trusted" }
        return flagged
    }

    @JvmStatic
    @WorkerThread
    fun isEnvironmentUntrustedWithContent(context: Context, content: () -> Unit = {}): Boolean {
        val flagged = evaluateWithBudget(context).isFlagged
        if (flagged) debugLog { "Environment is untrusted" }
        else {
            content()
            debugLog { "Environment is trusted" }
        }
        return flagged
    }

    /**
     * Startup gate: the full evaluation plus independent, harder-to-hook
     * corroboration. Flagged when any of these fire:
     *
     * - [isVirtualEnvironment] (EVT_1029)
     * - [isEnvironmentUntrusted] or [probeLocalPorts] (EVT_1030)
     * - a direct native scan: one strong signal, or at least
     *   WEAK_SIGNAL_THRESHOLD weak ones - the same rule as [evaluate]
     * - with [shouldEnableTimingCheck], a "clean" check chain finishing in
     *   under [minPlausibleCheckDurationMs] (EVT_1031)
     *
     * The time the check chain took is returned in [EvaluationResult.elapsedMs].
     */
    @JvmStatic
    @JvmOverloads
    @WorkerThread
    fun evaluateStartupState(
        context: Context,
        shouldEnableTimingCheck: Boolean,
        minPlausibleCheckDurationMs: Long = 20L
    ): EvaluationResult {
        val appContext = context.applicationContext
        val strong = mutableListOf<String>()
        val weak = mutableListOf<String>()

        // Runs first and outside evaluateWithBudget(): that budget fails
        // open on timeout, and emulators - slow virtual disk and exec - are
        // exactly where it is likeliest to time out.
        val virtualEnvironment = isVirtualEnvironment(appContext)
        if (virtualEnvironment) strong.add("EVT_1029")

        val start = System.nanoTime()
        val checksFlagged = isEnvironmentUntrusted(appContext) || probeLocalPorts()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        if (checksFlagged) strong.add("EVT_1030")

        // A differently-named entry point into the *same* native library,
        // called directly - not through DeviceStateUtils.evaluate().
        // Hooking only the obviously-named isEnvironmentUntrusted() does
        // not also silence this.
        val nativeStrong = mutableListOf<String>()
        val nativeWeak = mutableListOf<String>()
        safely {
            val signals = DeviceStateBridge.collectSystemInfo() +
                DeviceStateBridge.collectNetworkInfo() +
                DeviceStateBridge.collectEnvironmentInfo() +
                DeviceStateBridge.collectModuleInfo() +
                (if (BLOCK_EMULATORS) DeviceStateBridge.collectPlatformInfo() else emptyArray())
            signals.forEach { tagged ->
                when {
                    tagged.startsWith("S:") -> nativeStrong.add(tagged.removePrefix("S:"))
                    tagged.startsWith("W:") -> nativeWeak.add(tagged.removePrefix("W:"))
                }
            }
        }
        strong += nativeStrong
        weak += nativeWeak
        val nativeDirectFlagged =
            nativeStrong.isNotEmpty() || nativeWeak.size >= WEAK_SIGNAL_THRESHOLD

        // The real check chain above does ~20 file/proc stats plus five JNI
        // calls that each do their own file reads - it cannot complete
        // anywhere near minPlausibleCheckDurationMs. Completing faster than
        // that while reporting "clean" is itself evidence the real check
        // body never ran, i.e. isEnvironmentUntrusted() was short-circuited
        // by a hook that returns a canned value instead of calling through.
        //
        // Gated behind shouldEnableTimingCheck so it can be turned off if it
        // ever misfires on hardware it hasn't been calibrated against -
        // elapsedMs is still measured and returned either way.
        val suspiciouslyFast = shouldEnableTimingCheck && !checksFlagged &&
            elapsedMs < minPlausibleCheckDurationMs
        if (suspiciouslyFast) strong.add("EVT_1031")

        debugLog {
            "startup check timing: elapsedMs=$elapsedMs checks=$checksFlagged " +
                "native=$nativeDirectFlagged fast=$suspiciouslyFast virtual=$virtualEnvironment"
        }

        return EvaluationResult(
            isFlagged = virtualEnvironment || checksFlagged || nativeDirectFlagged || suspiciouslyFast,
            strongSignals = strong,
            weakSignals = weak,
            elapsedMs = elapsedMs
        )
    }

    @JvmStatic
    @JvmOverloads
    @WorkerThread
    fun evaluateStartupStateWithContent(
        context: Context,
        shouldEnableTimingCheck: Boolean,
        minPlausibleCheckDurationMs: Long = 20L,
        content: () -> Unit = {}
    ): EvaluationResult {
        val appContext = context.applicationContext
        val strong = mutableListOf<String>()
        val weak = mutableListOf<String>()

        // Runs first and outside evaluateWithBudget(): that budget fails
        // open on timeout, and emulators - slow virtual disk and exec - are
        // exactly where it is likeliest to time out.
        val virtualEnvironment = isVirtualEnvironment(appContext)
        if (virtualEnvironment) strong.add("EVT_1029")

        val start = System.nanoTime()
        val checksFlagged = isEnvironmentUntrusted(appContext) || probeLocalPorts()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        if (checksFlagged) strong.add("EVT_1030")

        // A differently-named entry point into the *same* native library,
        // called directly - not through DeviceStateUtils.evaluate().
        // Hooking only the obviously-named isEnvironmentUntrusted() does
        // not also silence this.
        val nativeStrong = mutableListOf<String>()
        val nativeWeak = mutableListOf<String>()
        safely {
            val signals = DeviceStateBridge.collectSystemInfo() +
                    DeviceStateBridge.collectNetworkInfo() +
                    DeviceStateBridge.collectEnvironmentInfo() +
                    DeviceStateBridge.collectModuleInfo() +
                    (if (BLOCK_EMULATORS) DeviceStateBridge.collectPlatformInfo() else emptyArray())
            signals.forEach { tagged ->
                when {
                    tagged.startsWith("S:") -> nativeStrong.add(tagged.removePrefix("S:"))
                    tagged.startsWith("W:") -> nativeWeak.add(tagged.removePrefix("W:"))
                }
            }
        }
        strong += nativeStrong
        weak += nativeWeak
        val nativeDirectFlagged =
            nativeStrong.isNotEmpty() || nativeWeak.size >= WEAK_SIGNAL_THRESHOLD

        // The real check chain above does ~20 file/proc stats plus five JNI
        // calls that each do their own file reads - it cannot complete
        // anywhere near minPlausibleCheckDurationMs. Completing faster than
        // that while reporting "clean" is itself evidence the real check
        // body never ran, i.e. isEnvironmentUntrusted() was short-circuited
        // by a hook that returns a canned value instead of calling through.
        //
        // Gated behind shouldEnableTimingCheck so it can be turned off if it
        // ever misfires on hardware it hasn't been calibrated against -
        // elapsedMs is still measured and returned either way.
        val suspiciouslyFast = shouldEnableTimingCheck && !checksFlagged &&
                elapsedMs < minPlausibleCheckDurationMs
        if (suspiciouslyFast) strong.add("EVT_1031")

        debugLog {
            "startup check timing: elapsedMs=$elapsedMs checks=$checksFlagged " +
                    "native=$nativeDirectFlagged fast=$suspiciouslyFast virtual=$virtualEnvironment"
        }

        val result = EvaluationResult(
            isFlagged = virtualEnvironment || checksFlagged || nativeDirectFlagged || suspiciouslyFast,
            strongSignals = strong,
            weakSignals = weak,
            elapsedMs = elapsedMs
        )
        if (result.isFlagged.not()) content()
        return result
    }

    @JvmStatic
    @WorkerThread
    private fun evaluate(context: Context): EvaluationResult {
        val strong = mutableListOf<String>()
        val weak = mutableListOf<String>()

        val props = readSystemProps()

        val skipBuildProps = !BLOCK_EMULATORS && isEmulator()

        safely { scanBinaryPaths(strong) }                                 // 1
        safely { scanPathEnv(strong) }                                     // 2
        safely { scanInstalledArtifacts(strong) }                          // 3
        safely { scanSystemArtifacts(strong) }                             // 4
        safely { scanFrameworkArtifacts(strong) }                          // 5
        safely { scanInstalledPackages(context, strong) }                  // 6
        safely { scanLoadedModules(strong, weak) }                         // 7
        safely { scanThreadNames(strong) }                                 // 8
        safely { scanMountFlags(strong, weak) }                            // 9
        safely { scanBootState(props, strong) }                            // 10
        safely { scanPathResolution(strong) }                              // 11
        safely { scanExecPermission(strong) }                              // 12 (off by default)

        safely { scanBuildTags(weak) }                                     // 13
        safely { scanBuildProps(props, weak, skipBuildProps) }             // 14
        safely { scanPolicyState(props, weak) }                            // 15
        safely { scanUtilityBinaries(weak) }                               // 16
        safely { scanHardwareProfile(weak) }                               // 17
        safely { scanEmulatorPackages(context, strong) }                   // 17b
        if (!BuildConfig.DEBUG) safely { scanProcessState(strong) }        // 18
        safely { probePortState(strong) }                                  // 19
        safely { collectNativeSignals(strong, weak) }                      // 20
        safely { verifySigningState(context, strong) }                     // 21

        val flagged = strong.isNotEmpty() || weak.size >= WEAK_SIGNAL_THRESHOLD

        if (flagged) {
            debugLog {
                String.format(Locale.getDefault(), "Environment flagged. strong=%s weak=%s", strong, weak)
            }
        }
        return EvaluationResult(flagged, strong, weak)
    }

    // ==================================================================
    // 1. su-style binary paths
    // ==================================================================
    private fun scanBinaryPaths(signals: MutableList<String>) {
        BINARY_PATHS.forEach {
            if (fileExists(it)) {
                debugLog { String.format(Locale.getDefault(), "binary found at %s", it) }
                signals.add("EVT_1001:$it")
            }
        }
    }

    // ==================================================================
    // 2. Manual PATH sweep for su (works even if exec is blocked)
    // ==================================================================
    private fun scanPathEnv(signals: MutableList<String>) {
        PATH_DIRS.forEach {
            val path = it + "su"
            if (fileExists(path)) {
                debugLog { String.format(Locale.getDefault(), "binary found on PATH at %s", path) }
                signals.add("EVT_1002:$path")
            }
        }
    }

    // ==================================================================
    // 3. Superuser-manager APKs and daemons
    // ==================================================================
    private fun scanInstalledArtifacts(signals: MutableList<String>) {
        INSTALLED_ARTIFACT_PATHS.forEach {
            if (fileExists(it)) {
                debugLog { String.format(Locale.getDefault(), "artifact found at %s", it) }
                signals.add("EVT_1003:$it")
            }
        }
    }

    // ==================================================================
    // 4. Magisk / KernelSU / APatch
    // ==================================================================
    private fun scanSystemArtifacts(signals: MutableList<String>) {
        SYSTEM_ARTIFACT_PATHS.forEach {
            if (fileExists(it)) {
                debugLog { String.format(Locale.getDefault(), "artifact found at %s", it) }
                signals.add("EVT_1004:$it")
            }
        }
    }

    // ==================================================================
    // 5. Xposed / LSPosed / Substrate
    // ==================================================================
    private fun scanFrameworkArtifacts(signals: MutableList<String>) {
        FRAMEWORK_ARTIFACT_PATHS.forEach {
            if (fileExists(it)) {
                debugLog { String.format(Locale.getDefault(), "artifact found at %s", it) }
                signals.add("EVT_1005:$it")
            }
        }
    }

    // ==================================================================
    // 6. Watched applications
    //    REQUIRES a <queries> block in AndroidManifest.xml on API 30+,
    //    otherwise getPackageInfo() always throws and this check is dead code.
    // ==================================================================
    private fun scanInstalledPackages(context: Context, signals: MutableList<String>) {
        val pm = context.packageManager
        WATCHED_PACKAGES.forEach { pkg ->
            if (isPackageInstalled(pm, pkg)) {
                debugLog { String.format(Locale.getDefault(), "watched package installed: %s", pkg) }
                signals.add("EVT_1006:$pkg")
            }
        }
    }

    private fun isPackageInstalled(pm: PackageManager, pkg: String): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION") pm.getPackageInfo(pkg, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: Throwable) {
        false
    }

    // ==================================================================
    // 7. Watched .so modules mapped into this process
    // ==================================================================
    private fun scanLoadedModules(
        strong: MutableList<String>, weak: MutableList<String>
    ) {
        val maps = File("/proc/self/maps")
        if (!maps.canRead()) {
            debugLog { String.format(Locale.getDefault(), "/proc/self/maps not readable") }
            weak.add("EVT_1008")
            return
        }
        val hits = LinkedHashSet<String>()
        try {
            maps.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val lower = line.lowercase()
                    WATCHED_MODULE_NAMES.forEach { needle ->
                        if (lower.contains(needle)) hits.add(needle)
                    }
                }
            }
        } catch (t: Throwable) {
            weak.add("EVT_1008")
            return
        }
        hits.forEach {
            debugLog { String.format(Locale.getDefault(), "watched module mapped: %s", it) }
            strong.add("EVT_1007:$it")
        }
    }

    // ==================================================================
    // 8. Watched thread names
    //    Replaces the old `ps` based check — since Android 7 `ps` with no args
    //    only lists your own process, so grepping it for a name never fired.
    // ==================================================================
    private fun scanThreadNames(signals: MutableList<String>) {
        try {
            val taskDir = File("/proc/self/task")
            val tasks = taskDir.listFiles() ?: return
            for (task in tasks) {
                val comm = File(task, "comm")
                if (!comm.canRead()) continue
                val name = comm.readText().trim().lowercase()
                WATCHED_THREAD_NAMES.forEach { needle ->
                    if (name == needle) {
                        debugLog { String.format(Locale.getDefault(), "watched thread detected: %s", name) }
                        signals.add("EVT_1009:$name")
                    }
                }
            }
        } catch (t: Throwable) {
            // ignore
        }
    }

    /**
     * Optional extra: probe Frida's default listening ports.
     * MUST be called from a background thread — a socket connect on the main
     * thread raises NetworkOnMainThreadException under StrictMode.
     */
    @JvmStatic
    @WorkerThread
    fun probeLocalPorts(): Boolean {
        for (port in WATCHED_PORTS) {
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress("127.0.0.1", port), 200)
                    debugLog { String.format(
                        Locale.getDefault(), "listener found on port %d", port
                    ) }
                    return true
                }
            } catch (_: Throwable) {
                // port closed — expected on a clean device
            }
        }
        return false
    }

    // ==================================================================
    // 9. Writable system partitions
    // ==================================================================
    private fun scanMountFlags(
        strong: MutableList<String>, weak: MutableList<String>
    ) {
        try {
            File("/proc/mounts").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split(" ")
                    if (parts.size < 4) return@forEach
                    val mountPoint = parts[1]
                    if (mountPoint !in PROTECTED_MOUNTS) return@forEach

                    val options = parts[3].split(",")
                    if (options.contains("rw")) {
                        debugLog { String.format(
                            Locale.getDefault(), "protected partition mounted rw: %s", mountPoint
                        ) }
                        strong.add("EVT_1010:$mountPoint")
                    }
                    // Magisk systemless overlays. Some stock ROMs legitimately use
                    // overlayfs on /product or /vendor, so this is a weak signal.
                    val fsType = parts[2]
                    if (fsType == "overlay" || fsType == "tmpfs") {
                        debugLog { String.format(
                            Locale.getDefault(), "overlay mount on %s (%s)", mountPoint, fsType
                        ) }
                        weak.add("EVT_1011:$mountPoint")
                    }
                }
            }
        } catch (t: Throwable) {
            // ignore
        }
    }

    // ==================================================================
    // 10. Verified boot / bootloader lock state
    // ==================================================================
    private fun scanBootState(props: Map<String, String>, signals: MutableList<String>) {
        // green = locked + OEM key, yellow = locked + user key,
        // orange = unlocked, red = verification failed
        when (props["ro.boot.verifiedbootstate"]?.lowercase()) {
            "orange", "red" -> {
                debugLog { String.format(Locale.getDefault(), "verifiedbootstate = orange/red") }
                signals.add("EVT_1012:unlocked")
            }
        }
        if (props["ro.boot.flash.locked"] == "0") {
            debugLog { String.format(Locale.getDefault(), "ro.boot.flash.locked = 0") }
            signals.add("EVT_1013:unlocked")
        }
        if (props["ro.secureboot.lockstate"]?.lowercase() == "unlocked") {
            signals.add("EVT_1014:unlocked")
        }
        if (props["ro.boot.veritymode"]?.lowercase() == "disabled") {
            signals.add("EVT_1015:disabled")
        }
    }

    // ==================================================================
    // 11. which su
    // ==================================================================
    private fun scanPathResolution(signals: MutableList<String>) {
        if (!ENABLE_PATH_SCAN) return
        val out = execAndRead(arrayOf("which", "su")) ?: return
        if (out.isNotBlank()) {
            debugLog { String.format(Locale.getDefault(), "path resolution -> %s", out) }
            signals.add("EVT_1016:${out.trim()}")
        }
    }

    // ==================================================================
    // 12. Execute su (disabled by default — see ENABLE_DEEP_SCAN)
    // ==================================================================
    private fun scanExecPermission(signals: MutableList<String>) {
        if (!ENABLE_DEEP_SCAN) return
        val out = execAndRead(arrayOf("su", "-c", "id"), timeoutMs = 3000L) ?: return
        if (out.contains("uid=0")) {
            debugLog { String.format(Locale.getDefault(), "exec probe succeeded: %s", out) }
            signals.add("EVT_1017:uid=0")
        }
    }

    // ==================================================================
    // 13. Build tags
    // ==================================================================
    private fun scanBuildTags(signals: MutableList<String>) {
        val tags = Build.TAGS
        if (tags != null && tags.contains("test-keys")) {
            debugLog { String.format(Locale.getDefault(), "device has test-keys") }
            signals.add("EVT_1018:test-keys")
        }
    }

    // ==================================================================
    // 14. Dangerous system properties
    // ==================================================================
    private fun scanBuildProps(
        props: Map<String, String>, signals: MutableList<String>, skipBuildProps: Boolean
    ) {
        if (skipBuildProps) return
        val debuggable = props["ro.debuggable"] == "1"
        val insecure = props["ro.secure"] == "0"
        val adbOpen = props["ro.adb.secure"] == "0"
        val type = props["ro.build.type"]?.lowercase()
        val devBuild = type == "userdebug" || type == "eng"

        if (debuggable || insecure || adbOpen || devBuild) {
            val detail = buildList {
                if (debuggable) add("ro.debuggable=1")
                if (insecure) add("ro.secure=0")
                if (adbOpen) add("ro.adb.secure=0")
                if (devBuild) add("ro.build.type=$type")
            }.joinToString("|")
            debugLog { String.format(Locale.getDefault(), "non-production build: %s", detail) }
            signals.add("EVT_1019:$detail")
        }

        if (props["service.adb.root"] == "1") {
            signals.add("EVT_1020:service.adb.root=1")
        }
    }

    // ==================================================================
    // 15. SELinux
    // ==================================================================
    private fun scanPolicyState(props: Map<String, String>, signals: MutableList<String>) {
        if (props["ro.build.selinux"] == "0") {
            signals.add("EVT_1021:disabled")
            return
        }
        try {
            val enforce = File("/sys/fs/selinux/enforce")
            if (enforce.canRead() && enforce.readText().trim() == "0") {
                debugLog { String.format(Locale.getDefault(), "policy is permissive") }
                signals.add("EVT_1021:permissive")
            }
        } catch (_: Throwable) {
            // unreadable on most devices — not a signal
        }
    }

    // ==================================================================
    // 16. utility binaries (weak — some OEM ROMs ship them legitimately)
    // ==================================================================
    private fun scanUtilityBinaries(signals: MutableList<String>) {
        UTILITY_BINARY_PATHS.forEach {
            if (fileExists(it)) {
                debugLog { String.format(Locale.getDefault(), "utility binary found at %s", it) }
                signals.add("EVT_1022:$it")
            }
        }
    }

    // ==================================================================
    // 17. Emulator
    // ==================================================================
    private fun scanHardwareProfile(signals: MutableList<String>) {
        if (!BLOCK_EMULATORS) return
        val hits = emulatorSignals()
        if (hits.isNotEmpty()) {
            debugLog { String.format(
                Locale.getDefault(), "emulator detected: %s / %s %s", Build.FINGERPRINT, Build.MODEL, hits
            ) }
            signals.add("EVT_1023:${Build.MODEL}")
            // Emulator alone is enough — bump it past the weak threshold.
            signals.add("EVT_1024")
        }
    }

    // Emulator companion apps (launchers/app stores/helpers the emulator
    // image ships with). Checked separately from WATCHED_PACKAGES so a
    // package hit here is attributed to the emulator signal. Every entry
    // must also be listed in AndroidManifest.xml's <queries>.
    private fun scanEmulatorPackages(context: Context, signals: MutableList<String>) {
        if (!BLOCK_EMULATORS) return
        val pm = context.packageManager
        EMULATOR_PACKAGES.forEach { pkg ->
            if (isPackageInstalled(pm, pkg)) {
                debugLog { String.format(Locale.getDefault(), "emulator package installed: %s", pkg) }
                signals.add("EVT_1023:$pkg")
            }
        }
    }

    // ==================================================================
    // 18. ptrace tracer attached
    // ==================================================================
    private fun scanProcessState(signals: MutableList<String>) {
        try {
            val status = File("/proc/self/status")
            if (!status.canRead()) return
            status.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (!line.startsWith("TracerPid:")) return@forEach
                    val pid = line.substringAfter(":").trim().toIntOrNull() ?: return@forEach
                    if (pid != 0) {
                        val tracer = readProcessName(pid)
                        debugLog { String.format(
                            Locale.getDefault(), "tracer attached: pid=%d name=%s", pid, tracer
                        ) }
                        signals.add("EVT_1025:$pid:$tracer")
                    }
                }
            }
        } catch (_: Throwable) {
            // ignore
        }
    }

    // ==================================================================
    // 19. Watched local ports
    // ==================================================================
    private fun probePortState(signals: MutableList<String>) {
        WATCHED_PORTS.forEach { port ->
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress("127.0.0.1", port), 200)
                    debugLog { "listener found on port $port" }
                    signals.add("EVT_1026:$port")
                }
            } catch (_: Throwable) {
                // closed — expected
            }
        }
    }

    private fun readProcessName(pid: Int): String = try {
        File("/proc/$pid/cmdline").readText().trim(' ').trim().ifEmpty { "unknown" }
    } catch (_: Throwable) {
        "unreadable"
    }

    // ==================================================================
    // 20. Native corroboration (see app/src/main/cpp/native-lib.cpp)
    //     Independent code path from everything above: raw syscalls instead
    //     of java.io.File, a dedicated /proc/net/unix scan, dl_iterate_phdr
    //     over the linker's own loaded-module list, a RAM-vs-on-disk code
    //     tamper check (catches inline hooks on our own functions and on
    //     the libc calls we depend on), and (opt-in) a self-ptrace tracer
    //     probe.
    //
    //     Deliberately five separate native calls rather than one combined
    //     entry point: a single native entry point would just move the old
    //     "one hook kills everything" problem down a layer instead of
    //     fixing it. Each of these is its own exported JNI symbol (see
    //     DeviceStateBridge.kt) - bypassing all of native corroboration now
    //     means finding and hooking five separately-named exports, not one.
    // ==================================================================
    private fun collectNativeSignals(strong: MutableList<String>, weak: MutableList<String>) {
        val signals = DeviceStateBridge.collectSystemInfo() +
            DeviceStateBridge.collectProcessInfo(ENABLE_NATIVE_DEEP_PROBE) +
            DeviceStateBridge.collectNetworkInfo() +
            DeviceStateBridge.collectEnvironmentInfo() +
            DeviceStateBridge.collectModuleInfo() +
            (if (BLOCK_EMULATORS) DeviceStateBridge.collectPlatformInfo() else emptyArray())

        signals.forEach { tagged ->
            when {
                tagged.startsWith("S:") -> {
                    val signal = tagged.removePrefix("S:")
                    debugLog { "native signal (strong): $signal" }
                    strong.add(signal)
                }

                tagged.startsWith("W:") -> {
                    val signal = tagged.removePrefix("W:")
                    debugLog { "native signal (weak): $signal" }
                    weak.add(signal)
                }
            }
        }
    }

    // ==================================================================
    // 21. Signing certificate (see EXPECTED_SIGNING_HASH)
    //     Catches a repackaged/re-signed APK even when the device it's
    //     running on is completely clean - a case none of the above checks
    //     can see, since they only ever look at the device, not the app.
    // ==================================================================
    private fun verifySigningState(context: Context, signals: MutableList<String>) {
        if (EXPECTED_SIGNING_HASH.isBlank()) return
        if (BuildConfig.DEBUG) return // debug builds are signed with the debug key on purpose
        val hashes = signingHashes(context)
        if (hashes.isEmpty()) return
        if (hashes.none { it.equals(EXPECTED_SIGNING_HASH, ignoreCase = true) }) {
            debugLog { "signing certificate does not match the expected one" }
            signals.add("EVT_1028")
        }
    }

    private fun signingHashes(context: Context): List<String> = try {
        val pm = context.packageManager
        val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = info.signingInfo
            when {
                signingInfo == null -> null
                signingInfo.hasMultipleSigners() -> signingInfo.apkContentsSigners
                else -> signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        }
        signatures?.map { signature ->
            val digest = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } ?: emptyList()
    } catch (_: Throwable) {
        emptyList()
    }

    /**
     * Only ever looked at Build.* fields and three qemu files, so it was
     * blind to every emulator that spoofs a real handset's Build.* values -
     * BlueStacks, Nox, LDPlayer, MEmu, MuMu - and to Genymotion builds that
     * report "Genymobile" rather than "Genymotion". Now also checks the
     * kernel/boot properties, device nodes, vendor binaries and companion
     * packages those emulators cannot hide without patching their own image.
     */
    @JvmStatic
    fun isEmulator(): Boolean = emulatorSignals().isNotEmpty()

    /**
     * Emulator check that runs outside evaluateWithBudget(). Only stats and
     * property reads, so it finishes in milliseconds - and it cannot be lost
     * to the budget's fail-open timeout, which emulators (slow virtual disk
     * I/O and exec) are the likeliest environment to hit.
     */
    @JvmStatic
    @WorkerThread
    fun isVirtualEnvironment(context: Context): Boolean {
        if (!BLOCK_EMULATORS) return false
        if (isEmulator()) return true
        val pm = context.applicationContext.packageManager
        return EMULATOR_PACKAGES.any { isPackageInstalled(pm, it) }
    }

    /**
     * Returns the emulator evidence found, or an empty list when the device
     * is not an emulator. Non-empty means either one DEFINITIVE hit or at
     * least EMULATOR_HEURISTIC_THRESHOLD distinct HEURISTIC hits - a single
     * loose pattern on its own never flags a device.
     */
    @JvmStatic
    private fun emulatorSignals(): List<String> {
        val definitive = LinkedHashSet<String>()
        val heuristic = LinkedHashSet<String>()

        val fingerprint = Build.FINGERPRINT.orEmpty().lowercase()
        val model = Build.MODEL.orEmpty().lowercase()
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val brand = Build.BRAND.orEmpty().lowercase()
        val device = Build.DEVICE.orEmpty().lowercase()
        val product = Build.PRODUCT.orEmpty().lowercase()

        // ---- definitive ----
        if (EMULATOR_MODEL_PREFIXES.any { model.startsWith(it) }) definitive.add("model")
        if (EMULATOR_PRODUCT_PREFIXES.any { product.startsWith(it) }) definitive.add("product")
        if (manufacturer in EMULATOR_MANUFACTURERS) definitive.add("mfr")
        if (brand == "waydroid" || device.startsWith("waydroid")) definitive.add("brand")
        if (isEmulatorHardwareToken(Build.HARDWARE) || isEmulatorHardwareToken(Build.BOARD)) {
            definitive.add("hw")
        }
        if (getPropViaReflection("ro.kernel.qemu") == "1") definitive.add("kq")
        if (getPropViaReflection("ro.boot.qemu") == "1") definitive.add("bq")
        EMULATOR_HARDWARE_PROPS.forEach { key ->
            if (isEmulatorHardwareToken(getPropViaReflection(key))) definitive.add("hp:$key")
        }
        EMULATOR_PRESENCE_PROPS.forEach { key ->
            if (!getPropViaReflection(key).isNullOrEmpty()) definitive.add("pp:$key")
        }
        EMULATOR_FILE_PATHS.forEach { if (fileExists(it)) definitive.add("f:$it") }

        // ---- heuristic ----
        if (fingerprint.startsWith("generic") || fingerprint.startsWith("unknown") ||
            fingerprint.contains("vbox") || fingerprint.contains("test-keys") && fingerprint.contains("sdk")
        ) heuristic.add("fp")
        if (model.contains("emulator") || model.contains("simulator")) heuristic.add("model~")
        if (product.contains("emulator") || product.contains("simulator")) heuristic.add("product~")
        if (manufacturer == "unknown") heuristic.add("mfr~")
        if (brand.startsWith("generic") && device.startsWith("generic")) heuristic.add("brand~")
        if (!getPropViaReflection("ro.kernel.qemu.gles").isNullOrEmpty()) heuristic.add("gles~")
        if (fileExists("/system/lib/libc_malloc_debug_qemu.so")) heuristic.add("malloc~")

        if (definitive.isEmpty() && heuristic.size < EMULATOR_HEURISTIC_THRESHOLD) {
            if (heuristic.isNotEmpty()) {
                debugLog { "emulator heuristic below threshold, not flagged: $heuristic" }
            }
            return emptyList()
        }
        return (definitive + heuristic).toList()
    }

    /**
     * True when [value] is one of EMULATOR_HARDWARE_TOKENS, or starts with one
     * followed by a non-letter ("ranchu64", "vbox86_x", "cutf_cvm-1"). Never a
     * bare substring match - see EMULATOR_HARDWARE_TOKENS.
     */
    private fun isEmulatorHardwareToken(value: String?): Boolean {
        val v = value?.trim()?.lowercase().orEmpty()
        if (v.isEmpty()) return false
        return EMULATOR_HARDWARE_TOKENS.any { token ->
            v == token || v.startsWith(token) && !v[token.length].isLetter()
        }
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /**
     * exists() alone is commonly hooked and also returns false for paths under
     * /data/adb that an unprivileged app cannot stat — canRead()/canExecute()
     * give a second opinion.
     */
    private fun fileExists(path: String): Boolean = try {
        val f = File(path)
        f.exists() || f.canRead() || f.canExecute()
    } catch (_: Throwable) {
        false
    }

    /** Reads all system properties once. Falls back to reflection if exec is blocked. */
    private fun readSystemProps(): Map<String, String> {
        val props = HashMap<String, String>()

        PROPS_OF_INTEREST.forEach { key ->
            getPropViaReflection(key)?.takeIf { it.isNotEmpty() }?.let { props[key] = it }
        }
        if (props.isNotEmpty()) return props

        debugLog { "SystemProperties reflection yielded nothing, falling back to getprop" }
        execAndRead(arrayOf("getprop"))?.lineSequence()?.forEach { line ->
            PROP_REGEX.find(line.trim())?.let { m ->
                props[m.groupValues[1]] = m.groupValues[2]
            }
        }
        return props
    }

    private fun getPropViaReflection(key: String): String? = try {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getMethod("get", String::class.java)
        method.invoke(null, key) as? String
    } catch (_: Throwable) {
        null
    }

    /** Runs a command with a timeout and returns stdout, or null on failure. */
    private fun execAndRead(command: Array<String>, timeoutMs: Long = EXEC_TIMEOUT_MS): String? {
        var process: Process? = null
        var reader: Future<String>? = null
        return try {
            process = ProcessBuilder(*command).redirectErrorStream(true).start()
            val stream = process.inputStream
            reader = execPool.submit(Callable { stream.bufferedReader().use { it.readText() } })
            try {
                reader.get(timeoutMs, TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
                debugLog { "exec timed out: ${command.joinToString(" ")}" }
                null
            } catch (t: Throwable) {
                debugLog { "exec read failed: ${t.localizedMessage}" }
                null
            }
        } catch (t: Throwable) {
            debugLog { "exec failed: ${t.localizedMessage}" }
            null
        } finally {
            process?.let { p ->
                runCatching { p.outputStream.close() }
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) p.destroyForcibly() else p.destroy()
                }
            }
            reader?.cancel(true)
        }
    }

    /** A failing check must never crash app launch. */
    private inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            debugLog { "scan threw: ${t.localizedMessage}" }
        }
    }

    /**
     * Release builds of this app used to plant Timber.DebugTree()
     * unconditionally (see BluestarApplication), so anything logged with
     * Log/Timber shipped to logcat in production too - `adb logcat -s
     * RootUtils` on a rooted test device would otherwise have handed an
     * attacker a plain-English list of every check that just fired, tagged
     * by name, with zero decompilation required. Every message in this
     * file goes through here so it compiles out of release entirely:
     * BuildConfig.DEBUG is a compile-time constant, so R8 dead-code-
     * eliminates both the `if` and the string(s) it guards.
     */
    private inline fun debugLog(message: () -> String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message())
    }
}