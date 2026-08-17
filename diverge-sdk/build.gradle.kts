plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dokka)
    alias(libs.plugins.paparazzi)
    `maven-publish`
    signing
}

val sdkVersion: String =
    rootProject.file("VERSION").readText().trim().also {
        require(it.isNotEmpty()) { "VERSION file is empty" }
    }

group = "ai.askdiverge"
version = sdkVersion

android {
    namespace = "ai.askdiverge.sdk"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SDK_VERSION", "\"$sdkVersion\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
        warningsAsErrors = false
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            // Javadoc/KDoc jar is produced via Dokka (dokkaJavadocJar) to avoid
            // colliding classifiers with AGP's empty withJavadocJar().
        }
    }
}

dependencies {
    testImplementation(libs.junit)
}

tasks.dokkaHtml.configure {
    moduleName.set("diverge-sdk")
}

tasks.dokkaJavadoc.configure {
    moduleName.set("diverge-sdk")
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "ai.askdiverge"
            artifactId = "diverge-sdk"
            version = sdkVersion

            afterEvaluate {
                from(components["release"])
            }

            artifact(dokkaJavadocJar)

            pom {
                name.set("Diverge SDK")
                description.set("Diverge ecommerce SDK for Android")
                url.set("https://github.com/askdiverge/Diverge-SDK-Android")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("diverge")
                        name.set("Diverge")
                        organization.set("Dialog Intelligens")
                        organizationUrl.set("https://askdiverge.ai")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/askdiverge/Diverge-SDK-Android.git")
                    developerConnection.set("scm:git:ssh://github.com/askdiverge/Diverge-SDK-Android.git")
                    url.set("https://github.com/askdiverge/Diverge-SDK-Android")
                }
            }
        }
    }

    repositories {
        // Central Portal OSSRH Staging API (maven-publish compatible).
        // After upload, finalize in https://central.sonatype.com/publishing
        // (or call the manual/upload API — see Dev-Docs/releases/MAVEN_CENTRAL.md).
        maven {
            name = "centralPortal"
            url = uri(
                "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/",
            )
            credentials {
                username = providers.environmentVariable("MAVEN_CENTRAL_USERNAME").orNull
                    ?: (findProperty("mavenCentralUsername") as String?)
                    ?: ""
                password = providers.environmentVariable("MAVEN_CENTRAL_PASSWORD").orNull
                    ?: (findProperty("mavenCentralPassword") as String?)
                    ?: ""
            }
        }
    }
}

val signingKey: String? =
    providers.environmentVariable("SIGNING_KEY").orNull
        ?: (findProperty("signingKey") as String?)
val signingPassword: String? =
    providers.environmentVariable("SIGNING_PASSWORD").orNull
        ?: (findProperty("signingPassword") as String?)
val canSign = !signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()

signing {
    isRequired = canSign
    if (canSign) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["release"])
    }
}

tasks.register("requireMavenCentralCredentials") {
    group = "publishing"
    description = "Fail fast when Central Portal / signing secrets are missing"
    doLast {
        val user = providers.environmentVariable("MAVEN_CENTRAL_USERNAME").orNull
            ?: (findProperty("mavenCentralUsername") as String?)
        val pass = providers.environmentVariable("MAVEN_CENTRAL_PASSWORD").orNull
            ?: (findProperty("mavenCentralPassword") as String?)
        require(!user.isNullOrBlank() && !pass.isNullOrBlank()) {
            """
            Maven Central credentials missing.
            Set MAVEN_CENTRAL_USERNAME / MAVEN_CENTRAL_PASSWORD (Portal user token),
            and SIGNING_KEY / SIGNING_PASSWORD (ASCII-armored PGP key).
            See Dev-Docs/releases/MAVEN_CENTRAL.md
            """.trimIndent()
        }
        require(canSign) {
            """
            GPG signing secrets missing.
            Set SIGNING_KEY (ASCII-armored private key) and SIGNING_PASSWORD.
            See Dev-Docs/releases/MAVEN_CENTRAL.md
            """.trimIndent()
        }
    }
}

tasks.named("publishReleasePublicationToCentralPortalRepository") {
    dependsOn("requireMavenCentralCredentials")
}
