This PR addresses Gradle build cache misses by ensuring that all archive tasks produce reproducible outputs.

### Problem
The build was suffering from frequent cache misses for archive tasks (like `jar`, `zip`). This was because the generated archives were not byte-for-byte identical across builds, even when the source code hadn't changed. The primary causes were:
- **Inclusion of Timestamps**: File timestamps were being embedded in the archive, causing the hash of the output to change with every build.
- **Non-Deterministic File Order**: The order of files within the archive was not guaranteed to be consistent, again leading to different output hashes.

These issues prevented Gradle from reusing cached results, which slowed down build times unnecessarily.

### Solution
To fix this, I've updated our build configuration to enforce reproducibility for all tasks that extend `AbstractArchiveTask`. The following settings have been applied globally:

- `isPreserveFileTimestamps = false`: This disables the inclusion of file modification times in the archive.
- `isReproducibleFileOrder = true`: This ensures that files are added to the archive in a consistent, deterministic order.

By making our archives reproducible, we ensure that Gradle can reliably cache and reuse these artifacts, leading to faster and more efficient builds.
