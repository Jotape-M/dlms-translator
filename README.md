# DLMS Translator

**DLMS Translator** is an IntelliJ Platform (IntelliJ IDEA, PyCharm, etc.) plugin designed to simplify the workflow for developers working with the **DLMS/COSEM** protocol. It provides quick translation of hex or base64 frames (PDUs) into a readable and well-formatted XML format.

## 🚀 Features

- **PDU to XML Translation**: Converts DLMS PDUs into structured XML.
- **Editor Integration**: Select a hex frame in any editor, right-click, and translate it instantly.
- **Multiple Input Formats**: Supports hexadecimal and Base64 encoded frames.
- **Tool Window**: A dedicated interface on the right sidebar for manual input and XML visualization.
- **Automatic Formatting**: XML is generated with professional indentation for enhanced readability.

## 🛠️ How to Use

### From the Editor (quickest)
1. Select a DLMS hex frame in any open file — for example, `7E A0 1E 03 21 93 7E`.
2. Right-click the selection and choose **Translate DLMS Frame**.
3. The **DLMS Translator** tool window opens, the frame is normalized and translated automatically.

Common log formats are handled transparently:
- `TX: 7E A0 1E 03 21 93 7E`
- `7E-A0-1E-03-21-93-7E`
- `0x7E 0xA0 0x1E 0x03 0x21 0x93 0x7E`

### Using the Tool Window
1. Open the **DLMS Translator** tool window on the right sidebar of your IDE.
2. Paste the hex or Base64 frame into the upper input field.
3. Click the translation button.
4. The formatted XML will be displayed in the lower area. You can copy the result using the copy icon in the output area's toolbar.

## 📦 Technologies and Dependencies

This plugin leverages the following libraries and tools:

- **[Gurux.DLMS.java](https://github.com/Gurux/Gurux.DLMS.java)**: The core library responsible for DLMS/COSEM translation logic.
- **IntelliJ Platform SDK**: Framework for plugin development.
- **Kotlin**: Modern programming language used for implementation.

## 🔧 Configuration and Compilation

To compile the project locally:

1. Clone the repository.
2. Import it as a Gradle project in IntelliJ IDEA.
3. Use the `runIde` Gradle task to test the plugin in a new IDE instance.
4. Use the `buildPlugin` Gradle task to generate the installable `.zip` file.

---
*Developed by [jotapem](https://github.com/jotapem)*
