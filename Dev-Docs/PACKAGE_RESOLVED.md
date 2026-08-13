# Package.resolved

This file is **committed** so CI and DocC builds resolve the same
`swift-docc-plugin` (and any future SPM) versions.

After changing `Package.swift` dependencies, run:

```bash
swift package resolve
```

and commit the updated `Package.resolved`.
