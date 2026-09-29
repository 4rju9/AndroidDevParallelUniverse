package app.netlify.dev4rju9.rootdetection

import android.util.Log

/**
 * Thin JNI bridge into libnative-lib.so (see app/src/main/cpp).
 *
 * AGP's default proguard-android-optimize.txt rule keeps the name of any
 * class that declares a native/`external` member, so - unlike the rest of
 * this app - this class keeps its original name through release R8. That is
 * expected of every JNI bridge class in every Android app and is not a
 * meaningful leak on its own: the actual checks live in the compiled,
 * symbol-stripped .so, not in this file.
 *
 * Deliberately five separate `external fun`s (matching five separate JNI
 * exports in native-lib.cpp) instead of one combined entry point. A single
 * native entry point is exactly the choke point that got the original
 * Kotlin-only check bypassed: one hook, every signal gone. Splitting the
 * native surface means a bypass has to find and hook five separately-named
 * exports, not one.
 */
internal object DeviceStateBridge {

    private const val TAG = "DeviceStateBridge"

    private val loaded: Boolean = try {
        System.loadLibrary("native-lib")
        true
    } catch (_: Throwable) {
        if (BuildConfig.DEBUG) Log.d(TAG, "native library unavailable, skipping")
        false
    }

    /** Loaded-module scan (maps text + dl_iterate_phdr). */
    fun collectSystemInfo(): Array<String> = safeCall { nativeCollectSystemInfo() }

    /** TracerPid, plus (opt-in) the self-ptrace probe. */
    fun collectProcessInfo(enableDeepProbe: Boolean): Array<String> =
        safeCall { nativeCollectProcessInfo(enableDeepProbe) }

    /** Local unix-domain-socket scan. */
    fun collectNetworkInfo(): Array<String> = safeCall { nativeCollectNetworkInfo() }

    /** Process environment scan. */
    fun collectEnvironmentInfo(): Array<String> = safeCall { nativeCollectEnvironmentInfo() }

    /** RAM-vs-disk self/libc code-tamper check (inline-hook + GOT/PLT detection). */
    fun collectModuleInfo(): Array<String> = safeCall { nativeCollectModuleInfo() }

    /** Emulator properties, device nodes and vendor binaries. */
    fun collectPlatformInfo(): Array<String> = safeCall { nativeCollectPlatformInfo() }

    /**
     * Every entry point returns signals prefixed "S:" (strong) or "W:"
     * (weak), matching DeviceStateUtils' own strong/weak split. Never
     * throws; an unloaded library or a failing native call both degrade to
     * "no extra signal" rather than crashing the calling evaluation.
     */
    private inline fun safeCall(block: () -> Array<String>): Array<String> {
        if (!loaded) return emptyArray()
        return try {
            block()
        } catch (_: Throwable) {
            emptyArray()
        }
    }

    @JvmStatic
    private external fun nativeCollectSystemInfo(): Array<String>

    @JvmStatic
    private external fun nativeCollectProcessInfo(enableDeepProbe: Boolean): Array<String>

    @JvmStatic
    private external fun nativeCollectNetworkInfo(): Array<String>

    @JvmStatic
    private external fun nativeCollectEnvironmentInfo(): Array<String>

    @JvmStatic
    private external fun nativeCollectModuleInfo(): Array<String>

    @JvmStatic
    private external fun nativeCollectPlatformInfo(): Array<String>
}