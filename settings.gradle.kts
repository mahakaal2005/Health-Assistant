pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // CRITICAL: Add JitPack repository for MPAndroidChart dependency
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Health_Assistant"
include(":app")