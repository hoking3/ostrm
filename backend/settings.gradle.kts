/**
 * Ostrm Backend Settings - Quarkus
 * @author hienao
 * @date 2025-12-31
 */
pluginManagement {
    val quarkusPluginVersion: String by settings
    val quarkusPluginId: String by settings
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
    plugins {
        id(quarkusPluginId) version quarkusPluginVersion
    }
}
rootProject.name = "openlisttostrm"
