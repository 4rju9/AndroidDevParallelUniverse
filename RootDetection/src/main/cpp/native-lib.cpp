// Native-side corroboration for DeviceStateUtils.kt.
//
// Why this exists in C++ and not just Kotlin: the previous check was pure
// Kotlin, so a single Frida script could hook the one public method the app
// called and force it to return false - which is exactly how it was
// bypassed. Everything here is a second, independent signal source that
// lives in a compiled, stripped .so instead of the dex. Defeating it
// requires hooking native code / raw syscalls instead of one Java method,
// which is a meaningfully higher bar - it is not "unbeatable" against a
// determined attacker with root and a native debugger, and this file does
// not pretend otherwise.
//
// Every check here fails closed on its own error (returns no signal, never
// throws/crashes) - DeviceStateUtils.kt is responsible for combining
// signals and for the overall time budget.
//
// Event codes returned to Kotlin (EVT_2xxx; EVT_1xxx is DeviceStateUtils'
// own range) are deliberately opaque - see proguard-rules.pro's
// "Root/tamper detection" section for why.
//
// EVT_2001 a watched module name is mapped/loaded
// EVT_2002 a tracer is attached to this process
// EVT_2003 a watched local socket is present
// EVT_2004 a watched module name is in the loaded-module list
// EVT_2005 a watched environment variable is set
// EVT_2006 self-ptrace probe found an existing tracer
// EVT_2007 in-memory code differs from the on-disk original
// EVT_2008 a watched libc symbol resolved outside libc
// EVT_2009 a watched libc symbol's code differs from the on-disk original
// EVT_2010 emulator property, device node or vendor binary present

#include <jni.h>

#include <cerrno>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <ctime>
#include <dlfcn.h>
#include <fcntl.h>
#include <link.h>
#include <pthread.h>
#include <string>
#include <sys/ptrace.h>
#include <sys/system_properties.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <vector>

namespace {

// Substrings looked for in /proc/self/maps and in the list of loaded
// shared objects (dl_iterate_phdr). Mirrors DeviceStateUtils.WATCHED_MODULE_NAMES
// on the Kotlin side - kept as a separate copy on purpose, so patching one
// list does not silently disable the other.
    constexpr const char *kWatchedModuleNeedles[] = {
            "frida", "linjector", "gadget", "gum-js", "gmain", "zygisk", "riru",
            "libriru", "lspd", "lsposed", "xposed", "substrate", "shadowhook",
            "sandhook", "epic.so", "whale.so", "dexposed"};

    std::string ToLower(const std::string &value) {
        std::string out = value;
        for (auto &ch : out) {
            ch = static_cast<char>(tolower(static_cast<unsigned char>(ch)));
        }
        return out;
    }

    bool ContainsAny(const std::string &haystackLower, std::string *outNeedle) {
        for (const char *needle : kWatchedModuleNeedles) {
            if (haystackLower.find(needle) != std::string::npos) {
                if (outNeedle) *outNeedle = needle;
                return true;
            }
        }
        return false;
    }

// Reads a small /proc file via raw syscalls rather than libc's fopen/open,
// so a Frida script that only Interceptor.attach()es the libc wrappers
// (the common, low-effort case) does not also swallow this read. Bounded
// to 1 MiB so a crafted /proc entry can never turn this into an unbounded
// loop.
    std::string ReadProcFileRaw(const char *path) {
        int fd = static_cast<int>(syscall(SYS_openat, AT_FDCWD, path, O_RDONLY));
        if (fd < 0) return {};

        std::string content;
        char buffer[4096];
        ssize_t bytesRead;
        while ((bytesRead = syscall(SYS_read, fd, buffer, sizeof(buffer))) > 0) {
            content.append(buffer, static_cast<size_t>(bytesRead));
            if (content.size() > (1u << 20)) break;
        }
        syscall(SYS_close, fd);
        return content;
    }

// noinline: these are each called from exactly one place, so without this
// an optimizing build is free to inline them into their caller and leave no
// standalone function body at a stable address - which is exactly the
// address VerifyModuleState() below needs to be able to name and read.
    __attribute__((noinline)) void CollectMapsInfo(std::vector<std::string> &strongSignals) {
        std::string maps = ReadProcFileRaw("/proc/self/maps");
        if (maps.empty()) return;
        std::string lower = ToLower(maps);
        for (const char *needle : kWatchedModuleNeedles) {
            if (lower.find(needle) != std::string::npos) {
                strongSignals.emplace_back(std::string("EVT_2001:") + needle);
            }
        }
    }

    __attribute__((noinline)) void CollectStatusInfo(std::vector<std::string> &strongSignals) {
        std::string status = ReadProcFileRaw("/proc/self/status");
        if (status.empty()) return;

        size_t pos = status.find("TracerPid:");
        if (pos == std::string::npos) return;
        pos += strlen("TracerPid:");
        while (pos < status.size() && (status[pos] == ' ' || status[pos] == '\t')) pos++;

        int pid = 0;
        bool sawDigit = false;
        while (pos < status.size() && status[pos] >= '0' && status[pos] <= '9') {
            sawDigit = true;
            pid = pid * 10 + (status[pos] - '0');
            pos++;
        }
        if (sawDigit && pid != 0) {
            strongSignals.emplace_back("EVT_2002:" + std::to_string(pid));
        }
    }

// frida-server exposes a unix domain socket for its IPC even when its TCP
// ports (27042/27043) are remapped or firewalled off. Its socket path
// contains "frida" regardless.
    __attribute__((noinline)) void CollectSocketInfo(std::vector<std::string> &strongSignals) {
        std::string unixNet = ReadProcFileRaw("/proc/net/unix");
        if (unixNet.empty()) return;
        if (ToLower(unixNet).find("frida") != std::string::npos) {
            strongSignals.emplace_back("EVT_2003:frida");
        }
    }

    struct ModuleScanState {
        bool found = false;
        std::string needle;
    };

    int ModulePhdrCallback(struct dl_phdr_info *info, size_t, void *data) {
        auto *state = static_cast<ModuleScanState *>(data);
        if (info->dlpi_name == nullptr || info->dlpi_name[0] == '\0') return 0;
        std::string lower = ToLower(info->dlpi_name);
        if (ContainsAny(lower, &state->needle)) {
            state->found = true;
            return 1; // non-zero stops iteration
        }
        return 0;
    }

// Independent of /proc/self/maps text parsing: walks the loaded-module list
// the dynamic linker itself maintains.
    __attribute__((noinline)) void CollectModuleList(std::vector<std::string> &strongSignals) {
        ModuleScanState state;
        dl_iterate_phdr(ModulePhdrCallback, &state);
        if (state.found) {
            strongSignals.emplace_back("EVT_2004:" + state.needle);
        }
    }

// Weak on purpose: some OEM tooling / test harnesses legitimately set
// LD_PRELOAD for unrelated reasons, so this corroborates rather than
// triggers alone.
    void CollectEnvInfo(std::vector<std::string> &weakSignals) {
        const char *preload = getenv("LD_PRELOAD");
        if (preload == nullptr || preload[0] == '\0') return;
        std::string lower = ToLower(preload);
        if (lower.find("frida") != std::string::npos || lower.find("gadget") != std::string::npos) {
            weakSignals.emplace_back("EVT_2005:LD_PRELOAD");
        }
    }

    struct ProcessProbeResult {
        bool tracerAlreadyPresent = false;
    };

// Isolated in its own throwaway thread on purpose: PTRACE_TRACEME hands
// tracing of the *calling thread* to its parent, and if that thread later
// hits a stop condition with no real tracer waiting on it, it can hang.
// Keeping the probe to a dedicated thread that detaches immediately and
// then exits limits any such risk to that one thread rather than the
// caller's.
    void *ProcessProbeThreadMain(void *arg) {
        auto *result = static_cast<ProcessProbeResult *>(arg);
        errno = 0;
        long outcome = ptrace(PTRACE_TRACEME, 0, nullptr, nullptr);
        if (outcome == -1 && errno == EPERM) {
            // Only one tracer is allowed per process - EPERM here means
            // something (frida-server, gdbserver, ...) already attached.
            result->tracerAlreadyPresent = true;
        } else if (outcome == 0) {
            ptrace(PTRACE_DETACH, 0, nullptr, nullptr);
        }
        return nullptr;
    }

// Some OEM builds run apps under a seccomp policy that denies the ptrace
// syscall outright, which also reports EPERM and would otherwise read as a
// false positive on a clean, non-rooted device. Kept weak and behind the
// caller-supplied `enabled` flag for exactly that reason - see
// DeviceStateUtils.ENABLE_NATIVE_DEEP_PROBE.
    void ProbeProcessState(std::vector<std::string> &weakSignals) {
        ProcessProbeResult result;
        pthread_t thread;
        if (pthread_create(&thread, nullptr, ProcessProbeThreadMain, &result) != 0) {
            return;
        }
        pthread_join(thread, nullptr);
        if (result.tracerAlreadyPresent) {
            weakSignals.emplace_back("EVT_2006");
        }
    }

// --- Code-integrity: RAM vs. on-disk comparison ---------------------------
//
// Frida's most common technique against native code is an inline hook:
// overwrite a function's first few instructions in memory with a jump to
// its own trampoline. That patch only ever touches the loaded pages in RAM
// - it does not (and, for an unrooted-style attach, cannot) rewrite the
// backing file on disk. So: find where a given address's executable
// segment is actually backed on disk (resolved live from /proc/self/maps,
// which correctly handles both a plain extracted .so and - AGP's default
// for this app's minSdk - a native lib mapped directly out of the APK's
// zip, since the map's file offset already accounts for either case), read
// the same byte range from that file, and compare. A mismatch means
// something patched this code after it was loaded; a lookup failure (e.g.
// an anonymous mapping) fails open rather than flags anything, consistent
// with every other check in this file.

    struct MappedRegion {
        std::string path;
        unsigned long fileOffsetForAddress = 0;
    };

    bool ResolveBackingFile(uintptr_t address, MappedRegion *out) {
        std::string maps = ReadProcFileRaw("/proc/self/maps");
        if (maps.empty()) return false;

        size_t lineStart = 0;
        while (lineStart < maps.size()) {
            size_t lineEnd = maps.find('\n', lineStart);
            std::string line = maps.substr(lineStart, lineEnd == std::string::npos
                                                      ? std::string::npos : lineEnd - lineStart);
            lineStart = (lineEnd == std::string::npos) ? maps.size() : lineEnd + 1;

            unsigned long start = 0, end = 0, offset = 0;
            char perms[8] = {0};
            char pathBuf[512] = {0};
            // Format: "start-end perms offset dev:dev inode                path"
            int matched = sscanf(line.c_str(), "%lx-%lx %7s %lx %*x:%*x %*lu %511[^\n]",
                                 &start, &end, perms, &offset, pathBuf);
            if (matched < 4) continue;
            if (address < start || address >= end) continue;
            if (strlen(perms) < 3 || perms[2] != 'x') continue; // only executable segments
            if (matched < 5 || pathBuf[0] != '/') return false; // anon/other - can't verify

            out->path = pathBuf;
            out->fileOffsetForAddress = offset + (address - start);
            return true;
        }
        return false;
    }

// Deliberately uses plain libc open()/pread() rather than the raw-syscall
// style above: the goal of this specific check is detecting *code*
// patching, not evading a hook on the file-read path, and a hooked
// open()/pread() here can only make this one check fail open (no false
// positive), never fabricate a mismatch.
    std::string ReadFileRangeAt(const std::string &path, unsigned long fileOffset, size_t length) {
        int fd = open(path.c_str(), O_RDONLY);
        if (fd < 0) return {};
        std::string content;
        content.resize(length);
        ssize_t n = pread(fd, content.data(), length, static_cast<off_t>(fileOffset));
        close(fd);
        if (n <= 0) return {};
        content.resize(static_cast<size_t>(n));
        return content;
    }

    bool CodeBytesMatchDisk(const void *address, size_t length) {
        MappedRegion region;
        if (!ResolveBackingFile(reinterpret_cast<uintptr_t>(address), &region)) {
            return true; // can't verify -> fail open, not "tampered"
        }
        std::string onDisk = ReadFileRangeAt(region.path, region.fileOffsetForAddress, length);
        if (onDisk.size() != length) return true; // read failed -> fail open
        return memcmp(onDisk.data(), address, length) == 0;
    }

// Checks our own detection functions' prologues for in-memory tampering.
// Deliberately does NOT check itself or the JNI entry points that call
// into this file (see the header comment above / DeviceStateUtils'
// comment on collectNativeSignals for why that would be circular) - it
// checks the functions that actually gather signals, which are what an
// attacker patching this file's logic in place would have to touch.
    void VerifyModuleState(std::vector<std::string> &strongSignals) {
        struct Target {
            const char *name;
            const void *address;
        };
        const Target targets[] = {
                {"CollectMapsInfo",   reinterpret_cast<const void *>(&CollectMapsInfo)},
                {"CollectStatusInfo", reinterpret_cast<const void *>(&CollectStatusInfo)},
                {"CollectSocketInfo", reinterpret_cast<const void *>(&CollectSocketInfo)},
                {"CollectModuleList", reinterpret_cast<const void *>(&CollectModuleList)},
        };
        for (const auto &target : targets) {
            if (!CodeBytesMatchDisk(target.address, 64)) {
                strongSignals.emplace_back("EVT_2007");
            }
        }
    }

// GOT/PLT-style check on the handful of libc entry points this file
// actually depends on. Covers the two ways a hook can sit on an imported
// symbol: (a) resolved somewhere other than libc.so entirely (a preloaded
// shim shadowing the symbol), or (b) still inside libc.so but patched in
// place there (what Interceptor.attach(Module.findExportByName("libc.so",
// ...)) actually does - it patches libc's own mapped pages, not just our
// import table, which is why this reuses the same disk-comparison helper
// as VerifyModuleState rather than only comparing pointer values).
    void VerifyRuntimeState(std::vector<std::string> &strongSignals) {
        void *libc = dlopen("libc.so", RTLD_NOLOAD | RTLD_LAZY);
        if (libc == nullptr) return; // libc is always already loaded; treat failure as unverifiable

        const char *symbols[] = {"ptrace", "pthread_create", "getenv"};
        for (const char *symbol : symbols) {
            void *resolved = dlsym(libc, symbol);
            if (resolved == nullptr) continue;

            MappedRegion region;
            if (ResolveBackingFile(reinterpret_cast<uintptr_t>(resolved), &region)) {
                if (region.path.find("libc.so") == std::string::npos) {
                    strongSignals.emplace_back("EVT_2008");
                    continue; // redirected entirely - no point disk-comparing bytes at this address
                }
            }

            if (!CodeBytesMatchDisk(resolved, 32)) {
                strongSignals.emplace_back("EVT_2009");
            }
        }

        dlclose(libc);
    }

// Emulator probe. Mirrors DeviceStateUtils.emulatorSignals() through an
// independent path: __system_property_get() and raw faccessat() instead of
// android.os.SystemProperties reflection and java.io.File, so hooking the
// Kotlin isEmulator() does not also silence this.
    constexpr const char *kEmulatorHardwareNeedles[] = {
            "goldfish", "ranchu", "vbox86", "ttvm_x86", "nox", "cutf_cvm", "android_x86",
            "waydroid", "anbox", "redroid", "genymotion"};

    constexpr const char *kEmulatorHardwareProps[] = {
            "ro.hardware", "ro.boot.hardware", "ro.product.board", "ro.board.platform"};

    constexpr const char *kEmulatorPresenceProps[] = {
            "init.svc.qemud", "init.svc.qemu-props", "ro.kernel.android.qemud",
            "init.svc.vbox86-setup", "init.svc.noxd", "init.svc.microvirtd", "init.svc.ldinit"};

    constexpr const char *kEmulatorPaths[] = {
            "/dev/socket/qemud", "/dev/qemu_pipe", "/dev/goldfish_pipe", "/sys/qemu_trace",
            "/system/bin/qemu-props", "/dev/vboxguest", "/dev/vboxuser", "/sys/module/vboxguest",
            "/dev/socket/genyd", "/dev/socket/baseband_genyd", "/system/bin/genybaseband",
            "/system/bin/nox-prop", "/system/bin/noxd", "/system/lib/libnoxd.so",
            "/data/.bluestacks.prop", "/system/bin/bstshutdown", "/boot/bstmods",
            "/system/bin/ldinit", "/system/bin/ldmountsf", "/system/bin/microvirtd",
            "/system/bin/microvirt-prop", "/system/bin/droid4x-prop", "/system/bin/ttVM-prop"};

    std::string ReadProp(const char *key) {
        char value[PROP_VALUE_MAX] = {0};
        __system_property_get(key, value);
        return ToLower(value);
    }

// Token match, never substring: "nox" as a substring hits Samsung's
// "knox". Equal to a token, or the token followed by a non-letter
// ("ranchu64", "vbox86_x"). Mirrors DeviceStateUtils.isEmulatorHardwareToken.
    bool IsEmulatorHardwareToken(const std::string &value) {
        if (value.empty()) return false;
        for (const char *token : kEmulatorHardwareNeedles) {
            const size_t len = strlen(token);
            if (value.compare(0, len, token) != 0) continue;
            if (value.size() == len || !isalpha(static_cast<unsigned char>(value[len]))) return true;
        }
        return false;
    }

    bool PathPresent(const char *path) {
        return syscall(SYS_faccessat, AT_FDCWD, path, F_OK, 0) == 0;
    }

    void CollectPlatformInfo(std::vector<std::string> &strongSignals) {
        if (ReadProp("ro.kernel.qemu") == "1" || ReadProp("ro.boot.qemu") == "1") {
            strongSignals.emplace_back("EVT_2010:k");
        }
        for (const char *key : kEmulatorHardwareProps) {
            if (IsEmulatorHardwareToken(ReadProp(key))) strongSignals.emplace_back("EVT_2010:h");
        }
        for (const char *key : kEmulatorPresenceProps) {
            if (!ReadProp(key).empty()) strongSignals.emplace_back("EVT_2010:p");
        }
        for (const char *path : kEmulatorPaths) {
            if (PathPresent(path)) strongSignals.emplace_back(std::string("EVT_2010:") + path);
        }
    }

    jobjectArray ToJavaStringArray(JNIEnv *env, const std::vector<std::string> &values) {
        jclass stringClass = env->FindClass("java/lang/String");
        jobjectArray result = env->NewObjectArray(static_cast<jsize>(values.size()), stringClass, nullptr);
        for (size_t i = 0; i < values.size(); ++i) {
            jstring element = env->NewStringUTF(values[i].c_str());
            env->SetObjectArrayElement(result, static_cast<jsize>(i), element);
            env->DeleteLocalRef(element);
        }
        env->DeleteLocalRef(stringClass);
        return result;
    }

    jobjectArray PackSignals(JNIEnv *env, const std::vector<std::string> &strong,
                             const std::vector<std::string> &weak) {
        std::vector<std::string> combined;
        combined.reserve(strong.size() + weak.size());
        for (const auto &s : strong) combined.push_back("S:" + s);
        for (const auto &w : weak) combined.push_back("W:" + w);
        return ToJavaStringArray(env, combined);
    }

} // namespace

// --- JNI exports -----------------------------------------------------------
//
// Deliberately five separate exported functions instead of one combined
// entry point. A single combined entry point was exactly the kind of choke
// point that got the original Kotlin-only check bypassed: one hook, one
// method, every signal gone. Splitting the native side across five
// independent symbols means bypassing all of it means finding and hooking
// five separately-named exports instead of one - each hookable on its own,
// but nobody's generic "patch the check" script does that by accident. See
// DeviceStateBridge.kt / DeviceStateUtils.collectNativeSignals for how
// these get called and merged on the Kotlin side.
//
// Every export wraps its body in try/catch: a C++ exception must never
// cross back over a JNI boundary (ART aborts the process if one does), and
// every check below is written not to throw, but this is the backstop -
// on any unexpected failure, fail closed with no signal rather than crash
// the calling evaluation.

extern "C" JNIEXPORT jobjectArray JNICALL
Java_app_netlify_dev4rju9_rootdetection_DeviceStateBridge_nativeCollectSystemInfo(
        JNIEnv *env, jclass /*clazz*/) {
    try {
        std::vector<std::string> strong;
        CollectMapsInfo(strong);
        CollectModuleList(strong);
        return PackSignals(env, strong, {});
    } catch (...) {
        return PackSignals(env, {}, {});
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_app_netlify_dev4rju9_rootdetection_DeviceStateBridge_nativeCollectProcessInfo(
        JNIEnv *env, jclass /*clazz*/, jboolean enableDeepProbe) {
    try {
        std::vector<std::string> strong;
        std::vector<std::string> weak;
        CollectStatusInfo(strong);
        if (enableDeepProbe) {
            ProbeProcessState(weak);
        }
        return PackSignals(env, strong, weak);
    } catch (...) {
        return PackSignals(env, {}, {});
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_app_netlify_dev4rju9_rootdetection_DeviceStateBridge_nativeCollectNetworkInfo(
        JNIEnv *env, jclass /*clazz*/) {
    try {
        std::vector<std::string> strong;
        CollectSocketInfo(strong);
        return PackSignals(env, strong, {});
    } catch (...) {
        return PackSignals(env, {}, {});
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_app_netlify_dev4rju9_rootdetection_DeviceStateBridge_nativeCollectEnvironmentInfo(
        JNIEnv *env, jclass /*clazz*/) {
    try {
        std::vector<std::string> weak;
        CollectEnvInfo(weak);
        return PackSignals(env, {}, weak);
    } catch (...) {
        return PackSignals(env, {}, {});
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_app_netlify_dev4rju9_rootdetection_DeviceStateBridge_nativeCollectModuleInfo(
        JNIEnv *env, jclass /*clazz*/) {
    try {
        std::vector<std::string> strong;
        VerifyModuleState(strong);
        VerifyRuntimeState(strong);
        return PackSignals(env, strong, {});
    } catch (...) {
        return PackSignals(env, {}, {});
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_app_netlify_dev4rju9_rootdetection_DeviceStateBridge_nativeCollectPlatformInfo(
        JNIEnv *env, jclass /*clazz*/) {
    try {
        std::vector<std::string> strong;
        CollectPlatformInfo(strong);
        return PackSignals(env, strong, {});
    } catch (...) {
        return PackSignals(env, {}, {});
    }
}