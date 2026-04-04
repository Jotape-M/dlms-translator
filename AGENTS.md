# DLMS Translator Plugin

This file provides guidance for AI agents (e.g. Claude, Copilot, Cursor) working on this codebase.

---

## Project Overview

**DLMS Translator** is an IntelliJ IDEA plugin that translates DLMS/COSEM protocol frames (hexadecimal or Base64) into formatted, readable XML — directly inside the IDE, without external tools.

- **Plugin ID:** `com.jotapem.dlms-translator`
- **Current version:** `1.1.0`
- **Language:** Kotlin (JVM 21)
- **Build system:** Gradle with IntelliJ Platform Gradle Plugin `2.10.2`
- **Target IDE:** IntelliJ IDEA `2025.2.4+` (since build `252.25557`)
- **Core library:** [Gurux DLMS Java](https://github.com/Gurux/gurux.dlms.java) `4.0.85`

---

## Architecture

The plugin follows a simple two-layer architecture:

```
UI Layer (Tool Window)
    └── DlmsToolWindowFactory.kt
            │
            ▼
Service Layer
    └── DlmsTranslatorService.kt  (uses Gurux DLMS library)
```

### Source structure

```
src/main/
├── kotlin/com/jotapem/dlmstranslator/
│   ├── MyBundle.kt                          # i18n message bundle accessor
│   ├── services/
│   │   └── DlmsTranslatorService.kt         # Translation business logic
│   └── toolWindow/
│       └── DlmsToolWindowFactory.kt         # UI: Tool Window definition
└── resources/
    ├── DlmsTranslatorIconMappings.json      # New UI icon mapping
    ├── images/                              # SVG icons (light/dark, expui)
    ├── messages/
    │   ├── MyMessageBundle.properties       # Strings (EN)
    │   └── MyMessageBundle_pt.properties   # Strings (PT-BR)
    └── META-INF/
        ├── plugin.xml                       # Plugin registration
        └── pluginIcon.svg
```

---

## Key Components

### `DlmsTranslatorService` (Singleton object)

The sole business logic component. Responsibilities:

- Accepts raw input (`String`) in HEX or Base64 format.
- Decodes input bytes using `GXCommon.hexToBytes()` or `Base64.getDecoder()`.
- Delegates to `GXDLMSTranslator.pduToXml()` (Gurux library) for DLMS parsing.
- Pretty-prints the resulting XML with 4-space indentation via `javax.xml.transform`.
- Returns user-facing error messages from `MyBundle` on invalid input.

**Key method:** `translate(input: String, useHex: Boolean, inputType: InputType): String`

**Input types enum:** `InputType.HEX` | `InputType.BASE64`

---

### `DlmsToolWindowFactory` (Tool Window UI)

Registered in `plugin.xml` as a Tool Window anchored to the right side of the IDE.

UI layout (vertical splitter):

```
┌─────────────────────────────────┐
│  Input label  │ [Hex ▼]         │  ← inputHeader (label + ComboBox)
│  ┌───────────────────────────┐  │
│  │   JBTextArea (inputArea)  │  │  ← JBScrollPane wrapping JBTextArea
│  └───────────────────────────┘  │
├─────────────────────────────────┤  ← JBSplitter divider
│  Output label          [Copy]   │  ← outputHeader (label + toolbar)
│  ┌───────────────────────────┐  │
│  │  EditorTextField (XML)    │  │  ← EditorTextField (viewer, XML type)
│  └───────────────────────────┘  │
├─────────────────────────────────┤
│  [Translate ▶]  [☑ Show Hex]   │  ← bottomPanel
└─────────────────────────────────┘
```

**Reactive triggers:** Translation runs automatically when the input type combo or the hex checkbox changes (in addition to clicking the button).

---

## Internationalisation

All user-facing strings are in `messages/MyMessageBundle.properties` (EN) and `MyMessageBundle_pt.properties` (PT-BR). Access them via `MyBundle.message("key")`.

When adding new strings, always add entries to **both** files.

---

## Build & Run

```bash
# Run the plugin in a sandboxed IDE instance
./gradlew runIde

# Build distributable ZIP
./gradlew buildPlugin

# Run tests
./gradlew test
```

Output artefacts go to `build/distributions/` and `build/libs/`.

---

## Agent Guidelines

- **Do not modify** `build/`, `.gradle/`, `.idea/`, or `.intellijPlatform/` directories.
- **UI strings** must never be hardcoded — always use `MyBundle.message()`.
- **New features** that add user-visible text require entries in both `.properties` files.
- The `DlmsTranslatorService` is a Kotlin `object` (singleton) — keep it stateless.
- The output `EditorTextField` is read-only (`isViewer = true`); write to it via `.text = ...`.
- IntelliJ Platform APIs must be called on the **EDT** when touching UI components.
- Target JVM compatibility is **Java 21**; avoid APIs deprecated before that.
