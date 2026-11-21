# Chunk Animator

An unofficial continuation of [Chunk Animator](https://github.com/Harleyoc1/ChunkAnimator) for Fabric, NeoForge and
Forge on Minecraft 26.2 and 26.3. It isn't affiliated with or endorsed by the original authors. It's a small
client-side mod that animates chunks as they load, so they don't just pop in.

## Credits

Chunk Animator was created by **Lumien**, and later ported and maintained by **Harley O'Connor**
([Harleyoc1/ChunkAnimator](https://github.com/Harleyoc1/ChunkAnimator)). This project continues their work. The
animation modes, easing options and settings are theirs. The easing equations are by **Robert Penner** (BSD license,
see `src/main/java/penner/easing`).

## What's different from the original

- **Fabric, NeoForge and Forge**, for Minecraft **26.2** and **26.3**.
- **Sodium and Iris support** (Fabric and NeoForge). Chunks animate with Sodium, and with Iris shader packs too.
- **Works with Minecraft's OpenGL and Vulkan renderers.**
- **Settings** live in `config/chunkanimator.json`. You can change them from the mods list (NeoForge, Forge, or
  [Mod Menu](https://modrinth.com/mod/modmenu) on Fabric) or with the `/chunkanimator` command.
- **Small behaviour changes:**
  - Placing the first block in an empty section no longer makes that section animate.
  - With Sodium, `HORIZONTAL_SLIDE` and `HORIZONTAL_SLIDE_ALTERNATE` look the same.
  - Iris can't use shader packs on Vulkan (an Iris limitation), but chunks still animate there.

## Building

Requires Java 25.

```
./gradlew build
```

Jars for every loader and Minecraft version are written to `build/libs/`, for example
`ChunkAnimator-neoforge-1.5.0+mc26.3.jar`.

## License

MIT, same as the original. See [LICENSE](LICENSE).
