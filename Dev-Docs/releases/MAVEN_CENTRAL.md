# Publishing to Maven Central (Central Portal)

Coordinates: `ai.askdiverge:diverge-sdk` (version from the repo root `VERSION` file).

The Gradle publication, Dokka javadoc jar, and GPG signing are wired in
[`android/diverge-sdk/build.gradle.kts`](../../android/diverge-sdk/build.gradle.kts).
**The artifact is not on Maven Central until a successful Portal publish completes** (namespace `ai.askdiverge` is already verified).

## Human prerequisites (one-time)

1. Create a [Central Portal](https://central.sonatype.com/) account.
2. Verify the `ai.askdiverge` namespace (DNS TXT). **Done** — namespace shows Verified in Central Portal.
3. Generate a [user token](https://central.sonatype.com/usertoken) (username + password pair).
4. Create a PGP signing key and distribute the public key to a keyserver.

## GitHub secrets

| Secret | Purpose |
|--------|---------|
| `MAVEN_CENTRAL_USERNAME` | Portal user-token username |
| `MAVEN_CENTRAL_PASSWORD` | Portal user-token password |
| `SIGNING_KEY` | ASCII-armored private PGP key |
| `SIGNING_PASSWORD` | PGP key passphrase |

## Local dry run

```bash
cd android
./gradlew :diverge-sdk:publishToMavenLocal
```

## CI publish

Use **Actions → Publish Android (Maven Central) → Run workflow**.

That workflow:

1. Runs Android checks (including `:sample:verifyR8PublicApiKeeps`).
2. Fails fast if secrets are missing (`requireMavenCentralCredentials`).
3. Runs `:diverge-sdk:publishReleasePublicationToCentralPortalRepository`.

After a successful upload, open [Central Portal Publishing](https://central.sonatype.com/publishing) and confirm/release the deployment (staging API uploads still need Portal finalization).

## Consumer dependency (after publish)

```kotlin
implementation("ai.askdiverge:diverge-sdk:0.1.0")
```
