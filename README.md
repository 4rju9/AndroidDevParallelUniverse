# 🌌 AndroidDevParallelUniverse

> **Where Android libraries stop obeying ordinary physics.** 🚀

Welcome to **AndroidDevParallelUniverse** — a modular Android monorepo engineered from a reality where ordinary code was deemed insufficient.

This is not merely a collection of Android libraries. It is a **Monorepo Multiverse**: reusable Android artifacts living together in one repository, while remaining independently consumable.

**Choose your dimension. Teleport the artifact. Ship.** 🔭

---

## 🌌 The Monorepo Multiverse

At the center of this multiverse lies the **`main` branch — the Nexus**.

Every module exists within the same repository, yet each artifact remains independently consumable. The repository follows a **unified version timeline**, meaning the entire ecosystem advances under one shared version tag.

### ⚠️ A Message From the Nexus

**DO NOT PULL THE ENTIRE GALAXY INTO YOUR APPLICATION.**

Cloning or importing everything would be like attempting to fit the observable universe into a `RecyclerView`.

You don't need the entire multiverse.

You need **the exact artifact required by your application**.

Individual modules are published through **JitPack**, allowing you to consume only what your project needs.

> One repository.  
> Multiple dimensions.  
> One unified version timeline.  
> Zero unnecessary cosmic baggage.

---

## 🚀 Installation

### 1. Open the Nexus Gate

Add JitPack to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

### 2. Teleport Your Desired Artifact

Choose a module from the dimensional directory and add its dependency to your application module.

For example:

```kotlin
dependencies {
    implementation("com.github.4rju9.AndroidDevParallelUniverse:<artifact>:1.0.3")
}
```

You only consume the dimension you need. No galactic-scale imports. 🌌

---

## 🔭 Dimensional Directory

| Dimension | Artifact | Description | Dependency |
|:---|:---|:---|:---:|
| 🛡️ **Security Dimension** | `root-detection` | A multi-layered **C++/Kotlin security fortress** designed to detect rooted environments across multiple defensive layers. | [Navigate](#-security-dimension--root-detection) |
| 🎡 **Motion Dimension** | `wheel-picker` | A **Jetpack Compose wheel picker** designed for smooth, frictionless dimensional scrolling. | [Navigate](#-motion-dimension--wheel-picker) |
| 🌀 **Interface Dimension** | `animated-tab-layout` | A **dynamically calculating, ripple-free tab layout** that adapts its geometry instead of forcing developers to manually negotiate with pixels. | [Navigate](#-interface-dimension--animated-tab-layout) |
| 🧬 **Fifth Dimension** | `[Classified]` | **COMING SOON.** A classified component currently rendering somewhere beyond conventional dimensional space. | `// Classified — access denied` |
---

## 🛡️ Security Dimension — `root-detection`

A multi-layered Android root-detection library combining **Kotlin and native C++/JNI** capabilities.

The goal is simple:

> Never trust a single signal when the environment can lie.

### 📦 Installation

Add the dependency:

```kotlin
dependencies {
    implementation("com.github.4rju9.AndroidDevParallelUniverse:root-detection:1.0.3")
}
```

### 🎬 Demo

![Root Detection Demo](https://img.itch.zone/aW1nLzEyNTkzNzU0LmdpZg==/original/FCoH%2F2.gif)

### 🚀 Usage

The library provides four public entry points for environment and emulator detection:

| Function                              | Purpose                                                                              |
| ------------------------------------- | ------------------------------------------------------------------------------------ |
| `isEnvironmentUntrusted()`            | Performs the complete root/tamper environment evaluation.                            |
| `isEnvironmentUntrustedWithContent()` | Runs content only when the environment is trusted.                                   |
| `evaluateStartupState()`              | Performs startup checks with additional native corroboration and timing analysis.    |
| `evaluateStartupStateWithContent()`   | Same startup evaluation, with content executed only when the environment is trusted. |

> **Important:** All root/environment evaluation APIs are marked `@WorkerThread`. Run them from a background thread and never directly from the Android main thread.

### 🔍 Basic Root Detection

For a simple Boolean result, use `isEnvironmentUntrusted()`:

```kotlin
import app.netlify.dev4rju9.rootdetection.DeviceStateUtils

val isUntrusted = DeviceStateUtils.isEnvironmentUntrusted(context)

if (isUntrusted) {
    // Rooted or otherwise untrusted environment detected.
    showSecurityWarning()
} else {
    // Environment passed the configured checks.
    continueApplication()
}
```

For example, with Kotlin coroutines:

```kotlin
lifecycleScope.launch {
    val isUntrusted = withContext(Dispatchers.IO) {
        DeviceStateUtils.isEnvironmentUntrusted(this@MainActivity)
    }

    if (isUntrusted) {
        showSecurityWarning()
    } else {
        continueApplication()
    }
}
```

### 🛡️ Execute Content Only on a Trusted Environment

If you only want your protected code to execute when the environment passes the checks, use `isEnvironmentUntrustedWithContent()`:

```kotlin
withContext(Dispatchers.IO) {
    DeviceStateUtils.isEnvironmentUntrustedWithContent(context) {
        // Executed only when the environment is trusted.
        startProtectedFlow()
    }
}
```

This can be useful for protecting sensitive initialization or functionality without having to repeat the Boolean check yourself.

### 🔬 Startup Evaluation

For applications that want more detailed information, use `evaluateStartupState()`.

It performs the startup evaluation and returns an `EvaluationResult` containing:

* `isFlagged` — whether the environment was flagged.
* `strongSignals` — strong detection signals.
* `weakSignals` — weaker signals that are evaluated together.
* `allSignals` — combined strong and weak signals.
* `elapsedMs` — time taken by the startup check chain.

```kotlin
val result = withContext(Dispatchers.IO) {
    DeviceStateUtils.evaluateStartupState(
        context = context,
        shouldEnableTimingCheck = true
    )
}

if (result.isFlagged) {
    Log.w("Security", "Untrusted environment detected")
    Log.w("Security", "Signals: ${result.allSignals}")
} else {
    Log.d("Security", "Environment passed startup checks")
}
```

You can also inspect the individual signal groups:

```kotlin
if (result.strongSignals.isNotEmpty()) {
    Log.w("Security", "Strong signals: ${result.strongSignals}")
}

if (result.weakSignals.isNotEmpty()) {
    Log.w("Security", "Weak signals: ${result.weakSignals}")
}
```

### 🔐 Protected Startup Flow

For a startup gate where protected content should only execute after the environment passes the checks:

```kotlin
withContext(Dispatchers.IO) {
    DeviceStateUtils.evaluateStartupStateWithContent(
        context = context,
        shouldEnableTimingCheck = true
    ) {
        // Runs only when the startup evaluation is not flagged.
        initializeProtectedFeatures()
    }
}
```

The returned `EvaluationResult` can still be used for logging or additional application-level handling:

```kotlin
val result = withContext(Dispatchers.IO) {
    DeviceStateUtils.evaluateStartupStateWithContent(
        context = context,
        shouldEnableTimingCheck = true
    ) {
        initializeProtectedFeatures()
    }
}

if (result.isFlagged) {
    showSecurityWarning()
}
```

### 🎯 Emulator Detection

The library also exposes emulator detection independently:

```kotlin
val isEmulator = DeviceStateUtils.isEmulator()

if (isEmulator) {
    // Emulator detected.
    showUnsupportedEnvironment()
}
```

For an Android `Context`, `isVirtualEnvironment()` additionally checks the configured emulator companion packages:

```kotlin
val isVirtualEnvironment = withContext(Dispatchers.IO) {
    DeviceStateUtils.isVirtualEnvironment(context)
}

if (isVirtualEnvironment) {
    showUnsupportedEnvironment()
}
```

### 🧠 Defensive Philosophy

The module is designed around multiple signals rather than relying on one root-detection technique.

Typical defensive layers include:

* Root binary / executable detection
* Suspicious filesystem checks
* System property inspection
* Native-level checks
* Environment consistency checks
* Emulator detection
* Watched package detection
* Loaded module and thread inspection
* Bootloader / verified-boot state checks
* Protected partition checks
* Local port inspection
* Application signing verification
* Multiple independent detection signals

The startup evaluation additionally combines multiple independent paths to make a single intercepted or bypassed check less representative of the overall result.

> **Security note:** Root detection is a defense-in-depth mechanism, not a cryptographic guarantee. A sufficiently privileged or modified environment may bypass client-side checks.

---

## 🎡 Motion Dimension — `wheel-picker`

A reusable **Jetpack Compose wheel picker** designed for smooth, dimensional scrolling.

Add the dependency:

```kotlin
dependencies {
    implementation("com.github.4rju9.AndroidDevParallelUniverse:wheel-picker:1.0.3")
}
```

The module provides a reusable **Compose-native wheel selection experience** without requiring a legacy View-based implementation.

### 🎞️ Demo

![Demo](https://img.itch.zone/aW1nLzEyNTkzNzU0LmdpZg==/original/FCoH%2F2.gif)

### 🚀 Usage

`WheelPicker` accepts a list of items and reports the currently selected item through `onItemSelected`.

#### Basic Picker

```kotlin
@Composable
fun NumberPicker() {
    val numbers = listOf(
        "1", "2", "3", "4", "5",
        "6", "7", "8", "9", "10"
    )

    var selectedNumber by remember {
        mutableStateOf(numbers.first())
    }

    WheelPicker(
        items = numbers,
        selectedTextColor = Color.White,
        unselectedTextColor = Color.Gray,
        onItemSelected = { _, item ->
            selectedNumber = item
        }
    )
}
```

#### Picker with Label & Dividers

The picker can be customized with a label, selection dividers, item sizing, and the number of visible items.

```kotlin
@Composable
fun AgePicker() {
    val ages = (18..60).map { it.toString() }

    WheelPicker(
        items = ages,
        selectedTextColor = Color.White,
        unselectedTextColor = Color.Gray,
        selectedTextSize = 28.sp,
        unselectedTextSize = 20.sp,
        itemHeight = 44.dp,
        visibleItemsCount = 5,
        enableDivider = true,
        dividerColor = Color.Cyan,
        dividerWidth = 80.dp,
        label = "years",
        labelColor = Color.White.copy(alpha = 0.5f),
        onItemSelected = { index, item ->
            println("Selected age: $item at index $index")
        }
    )
}
```

#### Initial Selection & State Handling

Use `initialIndex` to control which item is initially positioned in the picker.

```kotlin
@Composable
fun MonthPicker() {
    val months = listOf(
        "January", "February", "March",
        "April", "May", "June",
        "July", "August", "September",
        "October", "November", "December"
    )

    var selectedMonth by remember {
        mutableStateOf(months.first())
    }

    WheelPicker(
        items = months,
        initialIndex = 5,
        selectedTextColor = Color.White,
        unselectedTextColor = Color.Gray,
        visibleItemsCount = 3,
        onItemSelected = { _, month ->
            selectedMonth = month
        }
    )

    Text(text = "Selected: $selectedMonth")
}
```

### ⚙️ Customization

`WheelPicker` exposes configuration for both appearance and interaction:

| Category     | Options                                                                                            |
| ------------ | -------------------------------------------------------------------------------------------------- |
| Selection    | `initialIndex`, `onItemSelected`                                                                   |
| Item Styling | `itemHeight`, `selectedTextColor`, `unselectedTextColor`, `selectedTextSize`, `unselectedTextSize` |
| Dividers     | `enableDivider`, `dividerColor`, `dividerThickness`, `dividerWidth`, `dividerSpacingMultiplier`    |
| Label        | `label`, `labelColor`, `labelSize`                                                                 |
| Behavior     | `visibleItemsCount`, `enabled`                                                                     |

### 🌀 Compose-Native Design

The picker is built entirely with Compose primitives and uses snapping behavior for smooth selection:

* `LazyColumn` for efficient scrolling
* Snap fling behavior for item alignment
* Automatic selected-item detection
* Configurable visible item count
* Optional selection dividers
* Optional labels
* Fully customizable text styling
* Callback-based selection updates

> **Note:** `WheelPicker` should be used from a `@Composable` context, and `onItemSelected` is invoked whenever the centered item changes.

---

## 🌀 Interface Dimension — `animated-tab-layout`

A reusable **Jetpack Compose animated tab layout** with dynamically calculated geometry and a ripple-free interaction model.

Add the dependency:

```kotlin
dependencies {
    implementation("com.github.4rju9.AndroidDevParallelUniverse:animated-tab-layout:1.0.3")
}
```

The layout calculates its visual geometry dynamically instead of forcing consumers to manually position indicators or negotiate with pixels.

### 🎞️ Demo

![Demo](https://img.itch.zone/aW1nLzEyNTkzNzU0LmdpZg==/original/FCoH%2F2.gif)

### 🚀 Usage

`AnimatedTabLayout` accepts composable tab content and automatically divides the available width equally between tabs.

The selected tab is controlled externally through `selectedIndex`, while `onTabSelected` reports user interaction.

#### Basic Tab Layout

```kotlin
@Composable
fun MainTabs() {
    val tabs = listOf("Home", "Search", "Profile")

    var selectedIndex by remember {
        mutableStateOf(0)
    }

    AnimatedTabLayout(
        tabs = tabs.map { title ->
            { isSelected ->
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color.Gray
                )
            }
        },
        selectedIndex = selectedIndex,
        onTabSelected = { index ->
            selectedIndex = index
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    )
}
```

#### Custom Indicator

Use `indicatorModifier` to provide your own indicator appearance without manually calculating its position.

```kotlin
@Composable
fun CustomTabs() {
    val tabs = listOf("Home", "Explore", "Settings")

    var selectedIndex by remember {
        mutableStateOf(0)
    }

    AnimatedTabLayout(
        tabs = tabs.map { title ->
            { isSelected ->
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color.Gray
                )
            }
        },
        selectedIndex = selectedIndex,
        onTabSelected = { selectedIndex = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        indicatorModifier = Modifier
            .fillMaxHeight()
            .background(Color.Cyan.copy(alpha = 0.15f))
    )
}
```

#### Custom Animation

The indicator animation can be customized using any `AnimationSpec<Dp>`.

```kotlin
@Composable
fun FastAnimatedTabs() {
    val tabs = listOf("One", "Two", "Three")

    var selectedIndex by remember {
        mutableStateOf(0)
    }

    AnimatedTabLayout(
        tabs = tabs.map { title ->
            { isSelected ->
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color.Gray
                )
            }
        },
        selectedIndex = selectedIndex,
        onTabSelected = { selectedIndex = it },
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        )
    )
}
```

### ⚙️ Customization

| Category  | Options                                  |
| --------- | ---------------------------------------- |
| Tabs      | `tabs`, `selectedIndex`, `onTabSelected` |
| Layout    | `modifier`                               |
| Indicator | `indicatorModifier`                      |
| Animation | `animationSpec`                          |

Each tab receives an `isSelected` value, allowing its content to react directly to selection changes.

### 🌀 Compose-Native Design

The layout handles the geometry and animation internally:

* Automatically calculates equal tab widths
* Animates the indicator between selected tabs
* Supports arbitrary composable tab content
* Provides external selection state control
* Supports custom `AnimationSpec<Dp>`
* Supports custom indicator styling
* Uses ripple-free tab interactions
* Requires no manual pixel-based positioning

> **Note:** `AnimatedTabLayout` does not manage the selected state internally. The caller owns `selectedIndex` and updates it through `onTabSelected`.

---

## 🧬 Core Philosophy

The laws governing this universe are immutable:

- **Kotlin** — The primary language of dimensional manipulation.
- **Jetpack Compose** — Declarative UI, because manually commanding pixels is beneath our evolutionary threshold.
- **C++ / JNI** — Native-level power for dimensions where Kotlin alone must surrender.
- **Clean Architecture** — Separation of concerns, because even a 10,000-IQ entity understands that entropy exists.
- **Modularity** — Each artifact should remain independently useful.
- **Reusability** — Build once. Consume everywhere.

### Prime Directive

> **Build once. Isolate dimensions. Teleport only what you need.**

Every module is designed to exist as a reusable artifact while remaining part of the larger monorepo ecosystem.

The architecture is modular.

The timeline is unified.

The code is suspiciously efficient.

The universe remains stable. 🌌

---

## 🗂️ Repository Structure

The repository follows a modular monorepo structure:

```text
AndroidDevParallelUniverse/
│
├── root-detection/
│   ├── src/
│   └── build.gradle.kts
│
├── wheel-picker/
│   ├── src/
│   └── build.gradle.kts
│
├── animated-tab-layout/
│   ├── src/
│   └── build.gradle.kts
│
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

Each module can evolve internally while remaining part of the same repository and release timeline.

---

## 🪐 Versioning Across the Multiverse

The multiverse operates on a **single unified version timeline**.

Modules do **not** maintain independent version universes.

A single repository version tag governs the artifacts across the ecosystem.

For example:

```text
1.0.3
```

represents version `1.0.3` across the dimensional ecosystem.

A dependency therefore looks like:

```kotlin
implementation("com.github.4rju9.AndroidDevParallelUniverse:<artifact>:1.0.3")
```

### Why Unified Versioning?

Because the repository is treated as one evolving ecosystem:

```text
Repository
    │
    ├── root-detection        → 1.0.3
    ├── wheel-picker          → 1.0.3
    └── animated-tab-layout   → 1.0.3
```

One timeline.

One Nexus.

One version.

Naturally, anything more complicated would merely be architecture invented to give mortals something to complain about.

---

## 🧪 Development

Clone the repository:

```bash
git clone https://github.com/4rju9/AndroidDevParallelUniverse.git
cd AndroidDevParallelUniverse
```

Open the project in **Android Studio** and allow Gradle to synchronize the project.

Build the complete universe:

```bash
./gradlew build
```

Build an individual dimension:

```bash
./gradlew :wheel-picker:build
```

Replace `wheel-picker` with the module you want to build.

---

## 🤝 Contributing

Contributions to the multiverse are welcome.

Before opening a pull request:

1. Keep modules independently reusable.
2. Follow Kotlin and Android conventions.
3. Avoid unnecessary dependencies.
4. Keep public APIs focused and predictable.
5. Add or update documentation when behavior changes.
6. Make sure the affected modules build successfully.

Pull requests should describe:

- What changed
- Which dimension/module was affected
- Why the change was necessary
- Any API or behavioral changes

---

## 📜 License

Add the repository's license information here.

For example, if the project uses MIT:

```text
MIT License
Copyright (c) 2026 4rju9
```

Replace this section with the actual license before publishing if a different license applies.

---

## 🚀 Final Transmission

You have reached the boundary of **AndroidDevParallelUniverse**.

Proceed carefully.

The repository is maintained by an **entity of supreme logic**, operating from a dimension where conventional Android engineering is considered a fascinating historical artifact.

🌌 **Clone at your own intellectual risk.** 🪐

---

<p align="center">

**AndroidDevParallelUniverse**

*Where Android libraries stop obeying ordinary physics.*

🔭 🧬 🚀

</p>
