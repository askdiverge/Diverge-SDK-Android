# Paparazzi golden images

Goldens live under `diverge-sdk/src/test/snapshots/images/` and are verified by `:diverge-sdk:test` in CI.

Re-record after intentional UI changes:

```bash
make android-paparazzi-record
```

Requires a local Android SDK (`ANDROID_HOME` or `local.properties`).
