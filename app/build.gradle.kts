plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.apitest2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.apitest2"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val appcompat_version = "1.7.0"

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation ("com.google.code.gson:gson:2.8.9")
    implementation ("com.spotify.android:auth:1.2.5")
    implementation ("androidx.browser:browser:1.0.0")
    implementation("androidx.appcompat:appcompat:$appcompat_version")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0") // (선택 사항) 네트워크 로그 확인용
    implementation("com.google.android.material:material:1.9.0")
    implementation ("com.google.android.flexbox:flexbox:3.0.0")



    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(project(":libs"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}



allprojects {
    repositories {

        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/spotify/android-auth")
            credentials {
                //본인 id와 토큰으로 바꿀 것. 깃에 올릴때 토큰값 포함된 상태로 커밋하면 계속 보안 에러남. 조심
                username = "dummy" // GitHub 계정 ID
                password = "dummy" // GitHub Personal Access Token
            }
        }
    }
}