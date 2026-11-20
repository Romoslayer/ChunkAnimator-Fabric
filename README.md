# Chunk Animator (Fabric, Minecraft 26.2 / 26.3)

A Fabric port of [Chunk Animator](https://github.com/Harleyoc1/ChunkAnimator) by Lumien and Harley O'Connor.
It's a small client-side mod that animates chunks as they appear, so they don't just pop in.

## Downloads / building

```
./gradlew build
```

Jars end up in `build/libs/`:

| Jar | Minecraft | Fabric API | Sodium (optional) |
|---|---|---|---|
| `ChunkAnimator-1.4.0+mc26.2.jar` | 26.2 | 0.161.0+26.2 or newer | 0.9.x |
| `ChunkAnimator-1.4.0+mc26.3.jar` | 26.3 | 0.166.2+26.3 or newer | 0.9.x |

You need Java 25 and Fabric Loader 0.20.1 or newer. The mod is client-only, so it works on any server.

## Configuration

Settings live in `config/chunkanimator.json`. You can change them in game in two ways, and changes apply right away:

* **Settings screen:** click "Configure" next to Chunk Animator in [Mod Menu](https://modrinth.com/mod/modmenu)
  (optional), or run `/chunkanimator config`. Every option has a tooltip.
* **Commands:**

```
/chunkanimator                          show current settings
/chunkanimator config                   open the settings screen
/chunkanimator mode <mode>              BELOW, ABOVE, HYBRID, HORIZONTAL_SLIDE, HORIZONTAL_SLIDE_ALTERNATE
/chunkanimator easing <function>        LINEAR, QUAD, CUBIC, QUART, QUINT, EXPO, SINE, CIRC, BACK, BOUNCE, ELASTIC
/chunkanimator duration <ms>            0 turns animations off
/chunkanimator disableAroundPlayer <true|false>
/chunkanimator reload                   re-read the config file
```

The options and defaults are the same as in the original mod (BELOW, SINE, 1000 ms, false).

## Tested

Each of 26.2 and 26.3 was run through the automated test above on: Minecraft's renderer, Sodium, and Sodium + Iris
with a shader pack, on OpenGL; Minecraft's renderer and Sodium on Vulkan; and Minecraft's renderer and Sodium
connected to a dedicated server; and Sodium + Iris with Vulkan selected. All passed, with screenshots showing chunks
mid-animation.

Iris doesn't support Vulkan. When Vulkan is selected in Video Settings, Iris turns itself off (shader packs can't be
used) and Sodium renders normally, so chunks still animate. If Vulkan is forced with the `--graphicsBackend vulkan`
launch argument instead, Iris doesn't notice and crashes at startup when it makes an OpenGL call with no OpenGL
context. The crash trace is entirely inside Iris's own startup code; no Chunk Animator code is involved.

## How it works

### Minecraft's own renderer
Each frame, Minecraft uploads a small record for each visible chunk section (its block position and its fade-in
"visibility"). While a section is animating, the mod changes that record:

* the whole-block part of the offset is added to the section position;
* the sub-block part is packed into the visibility value, and a small patch to `core/terrain.vsh` / `.fsh`
  unpacks it, so the movement stays smooth.

On 26.3 the same record is used by both the per-draw path and the multi-draw-indirect path, so both are covered.

### Sodium
Sodium draws a whole region of 8×4×8 sections in one draw call, so per-section CPU offsets aren't possible.
Instead the mod patches Sodium's terrain vertex shader, and the animation is computed on the GPU:

* Sodium already stores a "first built at" timestamp for every section (for its own fade-in). The shader reads it,
  so the CPU does no per-section work each frame.
* The settings (duration, mode, easing, camera Y, horizon, world height) are added to the end of Sodium's
  per-frame globals uniform block. There's spare room in it, so no new bindings are needed. It works on both
  the OpenGL and Vulkan backends.
* Sections that are mid-animation are drawn with all faces, because Sodium's block-face culling assumes each
  section is in its real position.
* Sodium never fades sections close to the player; that radius is changed to match `disableAroundPlayer`.

### Safety and compatibility
* Every shader patch is optional. If a resource pack or another mod has changed a shader so a patch doesn't match,
  that shader is left alone. Vanilla then falls back to whole-block steps; under Sodium, animations are turned off.
  Rendering never breaks either way.
* Sodium hooks are only applied when Sodium is installed, and they are written so they can't crash the game if
  a future Sodium version changes.
* Iris (1.11.x, OpenGL only, which is an Iris limitation): works with shader packs on or off. With a pack on, the mod patches the terrain shaders Iris
  generates from the pack (including the shadow pass), so shadows move with the chunks. Tested with
  Complementary Reimagined r5.9.3. If a future Iris generates shaders the mod doesn't recognise, it logs a warning
  and chunks appear without animation while the pack is on.
* Lithium, FerriteCore, ModernFix, ImmediatelyFast, Entity Culling, More Culling, C2ME and similar mods don't
  touch chunk-section rendering, so they work alongside it.
* Canvas and VulkanMod replace the chunk renderer entirely. The mod detects them and stays inactive.

## Differences from the original
* `HORIZONTAL_SLIDE` and `HORIZONTAL_SLIDE_ALTERNATE`: on vanilla, both pick a direction when the chunk is first
  drawn. Under Sodium the direction is worked out on the GPU from the camera position, so the two modes look the
  same.
* Placing the first block in an empty section no longer makes that section animate as if it had just loaded.
* The config is a JSON file plus a `/chunkanimator` command, because NeoForge's config system isn't available on Fabric.

## Project layout
```
src/main/          shared code (config, animation maths, shader patches, Sodium hooks)
mc26.2/src/main/   26.2-only hooks (renderer + shader loading), mixin config
mc26.3/src/main/   26.3-only hooks (renderer + shader loading, include cache), mixin config
```
Run a dev client with `./gradlew :mc26.3:runClient` (add `-PwithSodium` to run it with Sodium).

### Automated in-game test
`./gradlew :mc26.3:runClientGameTest` (or `:mc26.2:`) opens the settings screen through Mod Menu and clicks it,
then loads a singleplayer world and connects to a real dedicated server. In each, it teleports to force fresh
chunks and saves screenshots partway through each animation mode to `mc26.x/build/run/clientGameTest/screenshots/`.
Options:

| Switch | Effect |
|---|---|
| `-PwithSodium` | run with Sodium |
| `-PwithIris -PshaderPack=<path to pack .zip>` | run with Sodium + Iris and that shader pack turned on |
| `-PgraphicsApi=vulkan` | force Minecraft's Vulkan backend (or `opengl`) |

## License
MIT, same as the original. The Penner easing equations are BSD-licensed (see `src/main/java/penner/easing`).
