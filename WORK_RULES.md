# Working on Forever Recipe Editor

- Verify the actual screen, handler, and event registrations before changing UI behavior. Do not infer runtime paths from class names alone.
- Keep the Minecraft 1.21.1, NeoForge, and JEI versions in `gradle.properties`, `build.gradle`, and `neoforge.mods.toml` consistent.
- Keep changes to the recovered 1.4.1 baseline separate from compatibility fixes. Rebuild with Java 21 and test JEI hover, ghost drag, recipe save, rename, delete, and datapack reload in game before publishing a release.
- Do not commit generated build output, instance worlds, or local runtime files.

The source tree in the first import comes from the original working project. Its 1.4.1 build produced the exact same SHA-256 as the archived 1.4.1 JAR supplied by Timo.
