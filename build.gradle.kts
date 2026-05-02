plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.jotapem"
version = "1.2.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea("2025.2.4")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)


        // Add plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }

    implementation("org.gurux:gurux.dlms:4.0.85")

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.25557"
        }

        changeNotes = """
            <h3>1.2.0</h3>
            <ul>
                <li>Added "Translate DLMS Frame" editor context menu action</li>
                <li>Select any hex frame in an editor, right-click, and translate it instantly</li>
                <li>Automatic normalization of common log formats (TX:/RX: prefixes, 0x prefixes, various separators)</li>
                <li>Clear IDE notifications for invalid or empty selections</li>
            </ul>
            <h3>1.1.1</h3>
            <ul>
                <li>Added scrolling support in the XML output area (vertical and horizontal)</li>
            </ul>
            <h3>1.1.0</h3>
            <ul>
                <li>Added Base64 input support alongside hexadecimal input</li>
                <li>Added input type selector dropdown (Hex/Base64) in the UI</li>
                <li>Improved error messages and validation</li>
                <li>UI refinements and layout improvements</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
