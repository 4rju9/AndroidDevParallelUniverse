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
    implementation("com.github.4rju9.AndroidDevParallelUniverse:wheel-picker:1.0")
}
```

You only consume the dimension you need. No galactic-scale imports. 🌌

---

## 🔭 Dimensional Directory

| Dimension | Artifact | Description | Dependency |
|---|---|---|---|
| 🛡️ **Security Dimension** | `root-detection` | A multi-layered **C++/Kotlin security fortress** designed to detect rooted environments across multiple defensive layers. | `implementation("com.github.4rju9.AndroidDevParallelUniverse:root-detection:1.0")` |
| 🎡 **Motion Dimension** | `wheel-picker` | A **Jetpack Compose wheel picker** designed for smooth, frictionless dimensional scrolling. | `implementation("com.github.4rju9.AndroidDevParallelUniverse:wheel-picker:1.0")` |
| 🌀 **Interface Dimension** | `animated-tab-layout` | A **dynamically calculating, ripple-free tab layout** that adapts its geometry instead of forcing developers to manually negotiate with pixels. | `implementation("com.github.4rju9.AndroidDevParallelUniverse:animated-tab-layout:1.0")` |
| 🧬 **Fifth Dimension** | `[Classified]` | **COMING SOON.** A classified component currently rendering somewhere beyond conventional dimensional space. | `// Classified — access denied` |

---

## 🛡️ Security Dimension — `root-detection`

A multi-layered Android root-detection library combining **Kotlin and native C++/JNI** capabilities.

The goal is simple:

> Never trust a single signal when the environment can lie.

Add the dependency:

```kotlin
dependencies {
    implementation("com.github.4rju9.AndroidDevParallelUniverse:root-detection:1.0")
}
```

### Defensive Philosophy

The module is designed around multiple signals rather than relying on one root-detection technique.

Typical defensive layers may include:

- Root binary / executable detection
- Suspicious filesystem checks
- System property inspection
- Native-level checks
- Environment consistency checks
- Multiple independent detection signals

> **Security note:** Root detection is a defense-in-depth mechanism, not a cryptographic guarantee. A sufficiently privileged or modified environment may bypass client-side checks.

---

## 🎡 Motion Dimension — `wheel-picker`

A reusable **Jetpack Compose wheel picker** designed for smooth, dimensional scrolling.

Add the dependency:

```kotlin
dependencies {
    implementation("com.github.4rju9.AndroidDevParallelUniverse:wheel-picker:1.0")
}
```

The module is intended to provide a reusable Compose-native wheel selection experience without requiring a legacy View-based implementation.

---

## 🌀 Interface Dimension — `animated-tab-layout`

A reusable **Jetpack Compose animated tab layout** with dynamically calculated geometry and a ripple-free interaction model.

Add the dependency:

```kotlin
dependencies {
    implementation("com.github.4rju9.AndroidDevParallelUniverse:animated-tab-layout:1.0")
}
```

The layout is designed to calculate its visual geometry dynamically instead of forcing consumers to manually position indicators and negotiate with pixels.

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
1.0
```

represents version `1.0` across the dimensional ecosystem.

A dependency therefore looks like:

```kotlin
implementation("com.github.4rju9.AndroidDevParallelUniverse:<artifact>:1.0")
```

### Why Unified Versioning?

Because the repository is treated as one evolving ecosystem:

```text
Repository
    │
    ├── root-detection        → 1.0
    ├── wheel-picker          → 1.0
    └── animated-tab-layout   → 1.0
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
