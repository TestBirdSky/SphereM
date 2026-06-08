pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        flatDir {
            dirs("libs")
        }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle/") }
        maven { url = uri("https://artifact.bytedance.com/repository/Volcengine/") }
        maven { url = uri("https://artifact.byteplus.com/repository/public/") }
        maven { url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") }
        maven {
            url = uri("https://artifact.bytedance.com/repository/Volcengine/")

        }
        maven {
            url = uri("https://artifact.byteplus.com/repository/public/")
        }

        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }

        maven {
            url = uri("https://android-sdk.is.com/")
        }

        //Chartboost
        maven {
            url = uri("https://cboost.jfrog.io/artifactory/chartboost-ads")
        }
        maven {
            url = uri("https://cboost.jfrog.io/artifactory/chartboost-mediation")
        }

        // MAX
        maven { url = uri("https://artifacts.applovin.com/android") }

        maven { url = uri("https://artifactory.bidmachine.io/bidmachine") }
        maven { url = uri("https://cboost.jfrog.io/artifactory/chartboost-ads/") }
        maven { url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
        // MAX

        // topon
//        flatDir {
//            dirs("libs")
//        }
        //TU(Core)
        maven {
            url = uri("https://jfrog.anythinktech.com/artifactory/overseas_sdk")
        }

        //Ironsource
        maven {
            url = uri("https://android-sdk.is.com/")
        }

        //Pangle
        maven {
            url = uri("https://artifact.bytedance.com/repository/pangle")
        }

        //Mintegral
        maven {
            url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
        }

        //Chartboost
        maven {
            url = uri("https://cboost.jfrog.io/artifactory/chartboost-ads")
        }
        maven {
            url = uri("https://cboost.jfrog.io/artifactory/chartboost-mediation")
        }
        maven {
            url = uri("https://cboost.jfrog.io/artifactory/chartboost-core")
        }

        // debug
//        maven {
//            url = uri("https://jfrog.anythinktech.com/artifactory/debugger")
//        }

        maven {
            url =uri("https://artifact.bytedance.com/repository/pangle")
        }
        maven { url = uri("https://cboost.jfrog.io/artifactory/chartboost-ads/") }
        maven { url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
        maven { url = uri("https://repo.pubmatic.com/artifactory/public-repos") }
    }
}

rootProject.name = "Sphere"
include(":app")
