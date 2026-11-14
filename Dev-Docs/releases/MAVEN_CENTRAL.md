# Publishing to Maven Central (Central Portal)

Coordinates: `ai.askdiverge:diverge-sdk` (version from the repo root `VERSION` file).

The Gradle publication, Dokka javadoc jar, and GPG signing are wired in
[`diverge-sdk/build.gradle.kts`](../../diverge-sdk/build.gradle.kts).
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
./gradlew :diverge-sdk:publishToMavenLocal
```

## CI publish

Run **Publish Android** from the release tag, so the upload is exactly the tagged commit:

```bash
gh workflow run publish-android.yml --ref vX.Y.Z
```

That workflow:

1. Runs Android checks (including `:sample:verifyR8PublicApiKeeps`).
2. Fails fast if secrets are missing (`requireMavenCentralCredentials`).
3. Runs `:diverge-sdk:publishReleasePublicationToCentralPortalRepository`, which uploads to the
   OSSRH Staging API.
4. Hands that upload to the Central Portal
   (`POST /manual/upload/defaultRepository/ai.askdiverge?publishing_type=user_managed`). Without
   this call the upload stays in staging and never appears in the Portal.

The deployment then waits as validated in
[Central Portal Deployments](https://central.sonatype.com/publishing/deployments). Check it and
click **Publish**, or **Drop** to discard it. Publishing is permanent: a released version can never
be changed or removed.

### Dropping a staged upload

If step 3 succeeded and step 4 failed, the upload is still in staging. List and drop it with the
Portal user token (`Authorization: Bearer <base64 of username:password>`):

```bash
curl -s -H "Authorization: Bearer $CENTRAL_AUTH" "https://ossrh-staging-api.central.sonatype.com/manual/search/repositories?ip=any&profile_id=ai.askdiverge"
curl -s -X DELETE -H "Authorization: Bearer $CENTRAL_AUTH" "https://ossrh-staging-api.central.sonatype.com/manual/drop/repository/<key>"
```

To hand it to the Portal by hand instead, `POST` to
`https://ossrh-staging-api.central.sonatype.com/manual/upload/repository/<key>`.

## Consumer dependency (after publish)

```kotlin
implementation("ai.askdiverge:diverge-sdk:1.0.0")
```
