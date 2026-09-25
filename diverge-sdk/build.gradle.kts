import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
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
        compose = true
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

    lint {
        abortOnError = true
        warningsAsErrors = false
    }

    testOptions {
        // Stub android.util.* instead of throwing "not mocked" in JVM unit tests.
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            // The suite uses JUnit 5 (@Nested/@DisplayName) via kotlin.test typealiases.
            it.useJUnitPlatform()
        }
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
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.tooling)
    implementation(libs.compose.material3)
    implementation(libs.coil3.compose)
    implementation(libs.coil3.core)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Coroutines
    implementation(libs.coroutines.core)

    // Network
    implementation(libs.moshi.adapters)
    implementation(libs.moshi.core)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.sse)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.moshi)
    ksp(libs.moshi.codegen)

    // Paging
    implementation(libs.androidx.paging.common)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk.core)
}

dokka {
    moduleName.set("diverge-sdk")
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

/**
 * Proves the published Javadoc jar and the Dokka HTML document exactly the public API: every
 * public declaration has a page, and no `internal` one does. An empty Dokka run fails here.
 */
tasks.register("verifyDokkaPublicApi") {
    group = "verification"
    description = "Assert the Javadoc jar and Dokka HTML document exactly the public SDK API"
    dependsOn(dokkaJavadocJar, tasks.dokkaGeneratePublicationHtml)

    val javadocJar = dokkaJavadocJar.flatMap { it.archiveFile }
    val htmlPackageList = tasks.dokkaGeneratePublicationHtml
        .flatMap { it.outputDirectory.file("diverge-sdk/package-list") }
    // files(), not file(): a missing package list is reported below instead of by Gradle.
    inputs.files(javadocJar, htmlPackageList)

    doLast {
        // Every public top-level declaration in src/main, by fully qualified name. Nested classes,
        // enum entries and members count as part of the type that declares them. Change it only
        // together with the public API.
        val publicApi = setOf(
            "ai.askdiverge.ChatbotCallbacks",
            "ai.askdiverge.domain.exception.ChatbotException",
            "ai.askdiverge.ui.compose.ChatbotBottomSheet",
            "ai.askdiverge.ui.compose.ChatbotScreen",
        )

        val jar = javadocJar.get().asFile
        // One page per class, nested ones as Outer.Inner.html. Top-level functions are documented
        // on their file's facade class: ai/askdiverge/ui/compose/ChatbotScreenKt.html.
        val inJavadoc = ZipFile(jar).use { zip ->
            zip.entries().asSequence()
                .map { it.name }
                .filter { it.startsWith("ai/") && it.endsWith(".html") }
                .filterNot { it.substringAfterLast('/').startsWith("package-") }
                .map { page ->
                    val pkg = page.substringBeforeLast('/').replace('/', '.')
                    val name = page.substringAfterLast('/').substringBefore('.').removeSuffix("Kt")
                    "$pkg.$name"
                }
                .toSet()
        }

        val packageList = htmlPackageList.get().asFile
        // Each location is a DRI, package/Outer.Inner/callable/…. Dokka writes no package list
        // when it documents nothing.
        val inHtml = packageList.takeIf { it.exists() }?.readLines().orEmpty()
            .filter { it.startsWith("\$dokka.location:") }
            .mapNotNull { line ->
                val (pkg, classNames, callable) = line.removePrefix("\$dokka.location:").split('/')
                classNames.substringBefore('.').ifEmpty { callable }
                    .takeIf { it.isNotEmpty() }
                    ?.let { "$pkg.$it" }
            }
            .toSet()

        val problems = listOf("Javadoc jar" to inJavadoc, "Dokka HTML" to inHtml)
            .flatMap { (output, documented) ->
                (publicApi - documented).map { "$output is missing $it" } +
                    (documented - publicApi).map { "$output documents non-public $it" }
            }
        require(problems.isEmpty()) {
            "Dokka output does not match the public API:\n" +
                problems.joinToString("\n") { "  - $it" } +
                "\nSee ${jar.path} and ${packageList.parentFile.path}"
        }
        logger.lifecycle("verifyDokkaPublicApi: all ${publicApi.size} public declarations documented")
    }
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
