# DLMS Translator

**DLMS Translator** is an IntelliJ Platform (IntelliJ IDEA, PyCharm, etc.) plugin designed to simplify the workflow for developers working with the **DLMS/COSEM** protocol. It provides quick translation of hexadecimal frames (PDUs) into a readable and well-formatted XML format.

## 🚀 Features

- **PDU to XML Translation**: Converts DLMS PDUs into structured XML.
- **Tool Window**: A dedicated interface on the right sidebar for manual input and XML visualization.
- **Editor Quick Action**: Select any hexadecimal text in the editor and use the **Translate DLMS Code** action from the context menu.
- **Automatic Formatting**: XML is generated with professional indentation for enhanced readability.
- **Integrated Notifications**: Quick translation results displayed via IntelliJ system notifications.

## 🛠️ How to Use

### Using the Tool Window
1. Open the **DLMS Translator** tool window on the right sidebar of your IDE.
2. Paste the hexadecimal frame into the upper input field.
3. Click the translation button.
4. The formatted XML will be displayed in the lower area. You can copy the result using the copy icon in the output area's toolbar.

### Using the Editor Context Menu
1. In any open file, select the hexadecimal text you wish to translate.
2. Right-click and select the **Translate DLMS Code** action.
3. A notification will appear containing the translation result.

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
