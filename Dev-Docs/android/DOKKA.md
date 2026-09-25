# Dokka

The Android library uses **Dokka 2** (Dokka Gradle plugin v2: `org.jetbrains.dokka` and
`org.jetbrains.dokka-javadoc`) to render the KDoc of the public API as HTML and as the `javadoc`
classifier JAR published next to the AAR.

## Why Dokka 2

Dokka 1.9 discovers Kotlin source sets through the `org.jetbrains.kotlin.android` plugin. AGP 9's
built-in Kotlin does not apply that plugin, so under AGP 9 Dokka 1.9 logs "Exiting Generation:
Nothing to document" and `dokkaJavadocJar` packages an empty JAR. Declaring `src/main/java` by hand
brings the pages back, but without a hand-wired Android and AAR classpath, `Modifier`, `FontFamily`
and `kotlin.Result` render as `<Error class: unknown class>`. Dokka 2.2.0 supports AGP 9 built-in
Kotlin and resolves them. Do not go back to 1.9.

## Tasks

| Task | Output |
|------|--------|
| `:diverge-sdk:dokkaGenerateHtml` | `diverge-sdk/build/dokka/html/` |
| `:diverge-sdk:dokkaGenerateJavadoc` | `diverge-sdk/build/dokka/javadoc/` |
| `:diverge-sdk:dokkaJavadocJar` | `diverge-sdk/build/libs/diverge-sdk-<version>-javadoc.jar`, published |
| `:diverge-sdk:verifyDokkaPublicApi` | Builds both and checks them; CI runs it |

The v1 tasks `dokkaHtml` and `dokkaJavadoc` still exist but fail under Dokka 2. Dokka documents the
`release` variant and only `public` declarations.

## Public API check

`verifyDokkaPublicApi` fails unless the Javadoc JAR and the HTML each document exactly the
top-level declarations in `publicApi` in
[`diverge-sdk/build.gradle.kts`](../../diverge-sdk/build.gradle.kts). That catches an empty run, a
public type that lost its page, and an `internal` declaration that became public. Nested classes,
enum entries and members count as part of the type that declares them.

Change `publicApi` in the same PR as the public API itself. Javadoc documents a top-level function
on its file's facade class (`ChatbotScreen.kt` becomes `ChatbotScreenKt`), and the check maps the
facade back to the function, so a public top-level function lives in a file named after it.

## Undocumented API and broken links

Dokka reports every public declaration without KDoc and every KDoc link it cannot resolve, in
`internal` code too, and `failOnWarning` turns each report into a failure of the `dokkaGenerate*`
tasks. Document the declaration, or write a link Dokka can resolve: `[Name][package.Name]` when the
target is not imported into the file, plain text for anything that is not a declaration.

## Publishing

See [`../releases/MAVEN_CENTRAL.md`](../releases/MAVEN_CENTRAL.md) for Central Portal
secrets, signing, and the `publish-android` workflow. The publication attaches `dokkaJavadocJar`
itself; AGP's `withJavadocJar()` stays off so there is one `javadoc` classifier. To see what would
be uploaded without touching `~/.m2`:

```bash
./gradlew :diverge-sdk:publishReleasePublicationToMavenLocal -Dmaven.repo.local="$PWD/build/m2"
```

## Upgrading Dokka

1. Bump `dokka` in [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml).
2. Run `./gradlew :diverge-sdk:verifyDokkaPublicApi` and open `diverge-sdk/build/dokka/html/index.html`.
3. Publish locally as above and confirm one `-javadoc.jar` next to the AAR and sources JAR.
