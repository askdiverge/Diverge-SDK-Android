plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val sdkVersion: String = rootProject.file("VERSION").readText().trim()

android {
    namespace = "ai.askdiverge.sample"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "ai.askdiverge.sample"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = sdkVersion
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = true
    }
}

dependencies {
    implementation(project(":diverge-sdk"))
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
}

/**
 * Proves consumer ProGuard rules keep public SDK types through the sample's
 * minifyEnabled release build by inspecting R8's mapping.txt.
 */
tasks.register("verifyR8PublicApiKeeps") {
    group = "verification"
    description = "Assert public Diverge SDK types survive sample release R8 minify"
    dependsOn("assembleRelease")

    val mappingFile = layout.buildDirectory.file("outputs/mapping/release/mapping.txt")
    inputs.file(mappingFile)
    outputs.upToDateWhen { false }

    doLast {
        val file = mappingFile.get().asFile
        require(file.exists()) {
            "Missing R8 mapping at ${file.path}. Did assembleRelease run with minifyEnabled?"
        }
        val mapping = file.readText()
        val requiredTypes = listOf(
            "ai.askdiverge.sdk.Diverge",
            "ai.askdiverge.sdk.DivergeConfiguration",
            "ai.askdiverge.sdk.DivergeEnvironment",
            "ai.askdiverge.sdk.DivergeClient",
            "ai.askdiverge.sdk.DivergeException",
            "ai.askdiverge.sdk.DivergeStatusView",
        )
        val missing = requiredTypes.filter { type ->
            // Kept class names appear as identity mappings: FQCN -> FQCN:
            !Regex("""(?m)^${Regex.escape(type)}\s*->\s*${Regex.escape(type)}\s*:""")
                .containsMatchIn(mapping)
        }
        require(missing.isEmpty()) {
            "R8 stripped or renamed public SDK types (consumer-rules.pro failed):\n" +
                missing.joinToString("\n") { "  - $it" } +
                "\nSee ${file.path}"
        }
        logger.lifecycle("verifyR8PublicApiKeeps: all ${requiredTypes.size} public types kept")
    }
}
