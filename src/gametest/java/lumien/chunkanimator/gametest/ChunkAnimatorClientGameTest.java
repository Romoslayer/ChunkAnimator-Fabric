package lumien.chunkanimator.gametest;

import lumien.chunkanimator.ChunkAnimator;
import lumien.chunkanimator.config.AnimationMode;
import lumien.chunkanimator.config.ChunkAnimatorConfig;
import lumien.chunkanimator.config.EasingFunction;
import lumien.chunkanimator.shader.ShaderPatcher;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Loads a world, teleports far away so fresh chunks stream in, and screenshots the animation
 * part-way through for several modes. Screenshots land in the game-test run directory.
 */
public final class ChunkAnimatorClientGameTest implements FabricClientGameTest {

    private static final int DURATION_MS = 4000;

    @Override
    public void runTest(ClientGameTestContext context) {
        testConfigScreen(context);

        ChunkAnimatorConfig.setAnimationDuration(DURATION_MS);
        ChunkAnimatorConfig.setEasingFunction(EasingFunction.SINE);

        try (TestSingleplayerContext singleplayer = context.worldBuilder().setUseConsistentSettings(false).create()) {
            runModes(context, singleplayer.getServer(), rendererName());
        }

        // A real dedicated server, reached over the network. Chunk Animator is client-only, so the
        // server knows nothing about it; chunks still animate as they arrive.
        try (TestDedicatedServerContext server = context.worldBuilder().setUseConsistentSettings(false).createServer();
             TestDedicatedServerConnection connection = server.connect()) {
            connection.waitForChunksRender();
            runModes(context, server, "server-" + rendererName());
        }
    }

    private static String rendererName() {
        return irisShaderPackInUse() ? "iris" : ChunkAnimator.isSodiumLoaded() ? "sodium" : "vanilla";
    }

    /** Asks Iris's public API (reflectively, so Iris stays optional) whether a shader pack is active. */
    private static boolean irisShaderPackInUse() {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris")) {
            return false;
        }
        try {
            final Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            final Object instance = api.getMethod("getInstance").invoke(null);
            return (boolean) api.getMethod("isShaderPackInUse").invoke(instance);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not query Iris", e);
        }
    }

    private static void runModes(ClientGameTestContext context, TestServerContext server, String label) {
        server.runCommand("gamerule doDaylightCycle false");
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("gamemode spectator @a");
        context.waitTicks(100);

        final boolean patched = switch (rendererName()) {
            case "iris" -> ShaderPatcher.isIrisPatched();
            case "sodium" -> ShaderPatcher.isSodiumPatched();
            default -> ShaderPatcher.isVanillaTerrainPatched();
        };
        if (!patched) {
            throw new AssertionError("Terrain shader was not patched for the " + rendererName() + " renderer");
        }

        int offset = 4000;
        for (AnimationMode mode : new AnimationMode[]{AnimationMode.BELOW, AnimationMode.ABOVE, AnimationMode.HORIZONTAL_SLIDE}) {
            ChunkAnimatorConfig.setMode(mode);
            server.runCommand("tp @a " + offset + " 140 " + offset + " 45 25");
            offset += 4000;

            final String name = label + "-" + mode.name().toLowerCase();
            context.waitTicks(40);
            context.takeScreenshot(name + "-1-early");
            context.waitTicks(15);
            context.takeScreenshot(name + "-2-mid");
            context.waitTicks(15);
            context.takeScreenshot(name + "-3-late");
            context.waitTicks(120);
            context.takeScreenshot(name + "-4-settled");
        }
    }

    /**
     * Clicks a widget by its label with real mouse input. Fabric's clickScreenButton only looks at
     * a screen's direct children, not inside vanilla's scrolling options list.
     */
    private static void clickWidget(ClientGameTestContext context, String label) {
        final double[] pos = context.computeOnClient(mc -> {
            final net.minecraft.client.gui.components.AbstractWidget widget = findWidget(mc.gui.screen(), label);
            if (widget == null) {
                throw new AssertionError("No widget labelled '" + label + "'");
            }
            final double scale = (double) mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth();
            return new double[]{(widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale};
        });
        context.getInput().setCursorPos(pos[0], pos[1]);
        context.waitTick();
        // 26.3 moved input to SDL, where the left mouse button is 1 instead of GLFW's 0.
        final String mcVersion = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("minecraft")
                .orElseThrow().getMetadata().getVersion().getFriendlyString();
        final int leftButton = mcVersion.startsWith("26.2") ? 0 : 1;
        context.getInput().holdMouse(leftButton);
        context.waitTick();
        context.getInput().releaseMouse(leftButton);
        context.waitTicks(2);
    }

    private static net.minecraft.client.gui.components.AbstractWidget findWidget(Object element, String label) {
        if (element instanceof net.minecraft.client.gui.components.AbstractWidget widget && widget.getMessage().getString().equals(label)) {
            return widget;
        }
        if (element instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container) {
            for (Object child : container.children()) {
                final net.minecraft.client.gui.components.AbstractWidget found = findWidget(child, label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Opens the settings screen the way Mod Menu does, changes a setting and checks it applied. */
    private static void testConfigScreen(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("modmenu")) {
            throw new AssertionError("Mod Menu should be on the dev runtime");
        }
        if (!com.terraformersmc.modmenu.ModMenu.hasConfigScreen(ChunkAnimator.MOD_ID)) {
            throw new AssertionError("Mod Menu did not pick up Chunk Animator's config screen");
        }

        ChunkAnimatorConfig.setMode(AnimationMode.BELOW);
        context.setScreen(() -> com.terraformersmc.modmenu.ModMenu.getConfigScreen(ChunkAnimator.MOD_ID, null));
        context.waitForScreen(lumien.chunkanimator.config.ChunkAnimatorConfigScreen.class);
        context.takeScreenshot("config-screen");

        clickWidget(context, "Animation: From Below");
        if (ChunkAnimatorConfig.mode() != AnimationMode.ABOVE) {
            throw new AssertionError("Clicking the Animation button should switch BELOW -> ABOVE, got " + ChunkAnimatorConfig.mode());
        }
        context.takeScreenshot("config-screen-after-click");

        context.clickScreenButton("Done");
        context.waitTick();
        ChunkAnimatorConfig.setMode(AnimationMode.BELOW);
    }

}
