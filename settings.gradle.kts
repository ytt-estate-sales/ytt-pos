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
