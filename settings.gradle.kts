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
        maven { url = uri("https://star-m.jp/products/s_sdk/StarIO10/Android/maven") }
    }
}

rootProject.name = "ytt-pos"

include(
    ":app",
    ":domain",
    ":data",
    ":reporting",
    ":hardware-printer-star",
    ":hardware-cashdrawer",
    ":hardware-payments",
    ":hardware-payments:paypal",
    ":hardware-payments:mock",
)
