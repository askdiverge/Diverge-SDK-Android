# Dokka

The Android library uses **Dokka 1.9.x** (`org.jetbrains.dokka`) to produce HTML and a
`javadoc` classifier JAR for Maven consumers.

## Why not Dokka 2 yet

Dokka 2 changes the Gradle plugin id (`org.jetbrains.dokka`) configuration DSL and
publication helpers. We stay on 1.9 until the first Central publish lands so the
`dokkaJavadocJar` + `maven-publish` path stays boring and reviewable.

## Publishing

See [`../releases/MAVEN_CENTRAL.md`](../releases/MAVEN_CENTRAL.md) for Central Portal
secrets, signing, and the `publish-android` workflow.

## Upgrade checklist (Dokka 2)

1. Bump `dokka` in [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml).
2. Migrate `tasks.dokkaHtml` / `dokkaJavadoc` configuration to the Dokka 2 DSL.
3. Confirm the published AAR still exposes a single `javadoc` classifier (no clash with AGP `withJavadocJar()`).
4. Spot-check generated KDoc for public API types.
