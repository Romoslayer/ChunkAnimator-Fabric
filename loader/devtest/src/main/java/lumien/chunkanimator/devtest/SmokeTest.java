package lumien.chunkanimator.devtest;

import lumien.chunkanimator.ChunkAnimator;
import lumien.chunkanimator.config.AnimationMode;
import lumien.chunkanimator.config.ChunkAnimatorConfig;
import lumien.chunkanimator.platform.Platform;
import lumien.chunkanimator.shader.ShaderPatcher;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.util.FileUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Development-only in-game smoke test for the loaders that have no client game-test framework
 * (NeoForge and Forge). Started with {@code -PsmokeTest}; never part of a release jar.
 *
 * <p>It opens the settings screen through the loader's own mods-list hook, creates a world (or,
 * with {@code -Dchunkanimator.smokeTest.server=host:port}, joins a server), checks that the right
 * terrain shader was patched, then teleports far away for each animation mode and takes
 * screenshots while the new chunks animate in. The result goes to {@code smoketest-result.txt}
 * in the run folder and the game then quits.</p>
 */
public final class SmokeTest implements Consumer<Minecraft> {

    private static final AnimationMode[] MODES = {AnimationMode.BELOW, AnimationMode.ABOVE, AnimationMode.HORIZONTAL_SLIDE};

    private final List<String> failures = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private final String server = System.getProperty("chunkanimator.smokeTest.server");
    private int stage;
    private int wait;
    private int mode;
    private int timeout = 20 * 120;

    public SmokeTest() {
        // Behave like a normal install. Development runs turn on Minecraft's IDE-only draw validation,
        // which rejects Iris's shader-pack pipelines (Iris's vertex bindings array is shorter than it expects).
        SharedConstants.IS_RUNNING_IN_IDE = false;
    }

    @Override
    public void accept(final Minecraft mc) {
        if (stage >= 99) {
            return;
        }
        try {
            if (timeout % 200 == 0) {
                ChunkAnimator.LOGGER.info("Smoke test stage {}: screen={}, overlay={}, loaded={}", stage,
                        mc.gui.screen(), mc.gui.overlay(), mc.isGameLoadFinished());
            }
            if (--timeout <= 0) {
                failures.add("Timed out in stage " + stage);
                finish(mc);
                return;
            }
            tick(mc);
        } catch (Throwable t) {
            failures.add("Exception: " + t);
            ChunkAnimator.LOGGER.error("Smoke test failed", t);
            finish(mc);
        }
    }

    private void tick(final Minecraft mc) throws IOException {
        // The test window is usually not focused; never pause the game.
        mc.options.pauseOnLostFocus = false;
        if (mc.gui.screen() instanceof PauseScreen) {
            mc.gui.setScreen(null);
        }
        if (wait > 0) {
            wait--;
            return;
        }
        switch (stage) {
            case 0 -> { // Wait for the title screen, then open the settings the way the mods list does.
                if (!mc.isGameLoadFinished() || mc.gui.overlay() != null || mc.gui.screen() == null) {
                    return;
                }
                if (!(mc.gui.screen() instanceof TitleScreen)) {
                    // First launch shows the accessibility onboarding screen instead.
                    mc.options.onboardAccessibility = false;
                    mc.gui.setScreen(new TitleScreen());
                    return;
                }
                {
                    final Screen config = LoaderTestHooks.configScreenFromModList(mc.gui.screen());
                    if (config == null) {
                        failures.add("No config screen registered with the " + Platform.loaderName() + " mods list");
                    } else {
                        mc.gui.setScreen(config);
                        notes.add("Config screen from mods list: " + config.getClass().getSimpleName());
                    }
                    next(20);
                }
            }
            case 1 -> {
                screenshot(mc, "config-screen");
                mc.gui.setScreen(new TitleScreen());
                next(5);
            }
            case 2 -> {
                if (server != null) {
                    ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(server),
                            new ServerData("Smoke test", server, ServerData.Type.OTHER), false, null);
                    notes.add("Joined server " + server);
                } else {
                    final LevelSettings settings = new LevelSettings("Smoke test", GameType.SPECTATOR,
                            LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
                    final String folder = FileUtil.findAvailableName(mc.getLevelSource().getBaseDir(), "Smoke test", "");
                    mc.createWorldOpenFlows().createFreshLevel(folder, settings, SelectWorldScreen.TEST_OPTIONS,
                            WorldPresets::createNormalWorldDimensions, new TitleScreen());
                }
                next(0);
            }
            case 3 -> { // Wait until in the world.
                if (mc.level != null && mc.player != null && mc.gui.screen() == null) {
                    command(mc, "gamemode spectator @s");
                    command(mc, "time set noon");
                    command(mc, "weather clear");
                    next(200);
                }
            }
            case 4 -> {
                final String renderer = renderer();
                final boolean patched = switch (renderer) {
                    case "iris" -> ShaderPatcher.isIrisPatched();
                    case "sodium" -> ShaderPatcher.isSodiumPatched();
                    default -> ShaderPatcher.isVanillaTerrainPatched();
                };
                notes.add("Renderer: " + renderer + ", terrain shader patched: " + patched);
                if (!patched) {
                    failures.add("Terrain shader was not patched for the " + renderer + " renderer");
                }
                ChunkAnimatorConfig.setAnimationDuration(1000);
                next(0);
            }
            case 5 -> { // Teleport far away so fresh chunks load, once per mode.
                ChunkAnimatorConfig.setMode(MODES[mode]);
                final int offset = 4000 * (mode + 1);
                command(mc, "tp @s " + offset + " 140 " + offset + " 45 25");
                next(40);
            }
            case 6 -> { screenshot(mc, name("1-early")); next(15); }
            case 7 -> { screenshot(mc, name("2-mid")); next(15); }
            case 8 -> { screenshot(mc, name("3-late")); next(120); }
            case 9 -> {
                screenshot(mc, name("4-settled"));
                mode++;
                if (mode < MODES.length) {
                    stage = 5;
                } else {
                    next(20);
                }
            }
            case 10 -> finish(mc);
            default -> {}
        }
    }

    private String name(final String suffix) {
        return (server != null ? "server-" : "") + renderer() + "-" + MODES[mode].name().toLowerCase(Locale.ROOT) + "-" + suffix;
    }

    private void next(final int ticks) {
        stage++;
        wait = ticks;
    }

    private static void command(final Minecraft mc, final String command) {
        mc.player.connection.sendCommand(command);
    }

    private static void screenshot(final Minecraft mc, final String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {});
    }

    private static String renderer() {
        if (Platform.isModLoaded("iris") && irisShaderPackInUse()) {
            return "iris";
        }
        return ChunkAnimator.isSodiumLoaded() ? "sodium" : "vanilla";
    }

    private static boolean irisShaderPackInUse() {
        try {
            final Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            final Object instance = api.getMethod("getInstance").invoke(null);
            return (boolean) api.getMethod("isShaderPackInUse").invoke(instance);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private void finish(final Minecraft mc) {
        stage = 99;
        final StringBuilder out = new StringBuilder(failures.isEmpty() ? "PASS\n" : "FAIL\n");
        out.append("Loader: ").append(Platform.loaderName()).append('\n');
        notes.forEach(note -> out.append(note).append('\n'));
        failures.forEach(failure -> out.append("FAILURE: ").append(failure).append('\n'));
        try {
            Files.writeString(Path.of(mc.gameDirectory.getPath(), "smoketest-result.txt"), out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ChunkAnimator.LOGGER.error("Could not write smoke test result", e);
        }
        ChunkAnimator.LOGGER.info("Smoke test finished:\n{}", out);
        if (server != null && mc.player != null) {
            command(mc, "stop"); // shut the test server down too
        }
        mc.stop();
    }

}
