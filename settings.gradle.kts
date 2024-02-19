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

rootProject.name="inSENSE"
val opencvsdk = "/home/sahil/Downloads/Applications/OPENCVAND/OpenCV-android-sdk/sdk/"

rootProject.name = "decoy"
include(":app")

include("opencv")
project(":opencv").projectDir = file(opencvsdk)