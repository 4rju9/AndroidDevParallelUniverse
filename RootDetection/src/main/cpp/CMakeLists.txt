cmake_minimum_required(VERSION 3.22.1)
project(nativelib)

# Single small library, only used by this app - static STL keeps deployment
# to one .so per ABI with no libc++_shared.so to package/version-match.
# Named "native-lib" deliberately: the literal default name Android
# Studio's own "Add C++ support" wizard generates, so it reads as boilerplate
# rather than anything worth a second look in a decompile.
add_library(native-lib SHARED
        native-lib.cpp)

set_target_properties(native-lib PROPERTIES
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED ON)

target_compile_options(native-lib PRIVATE
        -fvisibility=hidden
        -fvisibility-inlines-hidden)
# No manual $<CONFIG:Release> optimization flag: AGP builds the release
# native variant as CMake's RelWithDebInfo, never "Release" (confirmed by
# actually running :app:assembleRelease - Gradle logs
# "configureCMakeRelWithDebInfo[...]"), so a Release-gated generator
# expression here would silently never fire. RelWithDebInfo already
# defaults to -O2 -g, which is what we want anyway: optimized, with debug
# info kept around for the next paragraph.

# --- 16 KB page-size compliance -------------------------------------------
# Google Play requires (from Nov 2025) that native libraries be usable on
# devices with a 16 KB memory page size. NDK r28+ already defaults the
# linker to 16 KB ELF segment alignment, but the flags are set explicitly
# here so this keeps working even if the pinned ndkVersion changes later,
# and so `zipalign`/`apkanalyzer` checks stay green regardless of NDK
# defaults. AGP already packages native libraries uncompressed by default
# for this app's minSdk (29), which is the other half of the requirement.
target_link_options(native-lib PRIVATE
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384")

# Deliberately NOT stripping symbols here (e.g. with a linker -s flag).
# RelWithDebInfo's -g output feeds AGP's own
# extractReleaseNativeSymbolTables/mergeReleaseNativeDebugMetadata pipeline,
# the native-crash-symbolication equivalent of the Crashlytics dex mapping
# upload two sections up - stripping at link time would delete that debug
# info before AGP ever gets to extract it, and every native crash would
# come back as an unreadable address instead of a function name. AGP's own
# stripReleaseDebugSymbols task still strips the copy that actually ships
# inside the APK (verified: the packaged .so's .symtab is gone, only the
# .dynsym required for JNI to resolve the five exported nativeCollect*()
# functions remains) - so the concealment goal is already met without
# touching this file.

find_library(log-lib log)
target_link_libraries(native-lib ${log-lib})