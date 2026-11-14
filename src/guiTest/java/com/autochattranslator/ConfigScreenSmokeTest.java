package com.autochattranslator;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mojang.logging.LogUtils;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.gui.OptionListWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = AutoChatTranslator.MODID, value = Dist.CLIENT)
public final class ConfigScreenSmokeTest {
    private static int step;
    private static int ticks;
    private static Screen parent;
    private static YACLScreen screen;
    private static boolean originalEnabled;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.getOverlay() != null || step < 0) {
            return;
        }
        if (step == 0 && !(mc.screen instanceof TitleScreen)) {
            return;
        }
        if (++ticks < 30) {
            return;
        }
        ticks = 0;
        try {
            switch (step++) {
                case 0 -> {
                    parent = mc.screen;
                    originalEnabled = Config.ENABLED.get();
                    open();
                    check(screen.config.categories().size() == 3, "three categories");
                    check(options().size() == 26, "all 26 settings exposed");
                    String language = mc.options.languageCode;
                    String expectedTitle = language.startsWith("zh") ? "自动聊天翻译 · 设置" : "Auto Chat Translator · Settings";
                    check(screen.getTitle().getString().equals(expectedTitle), "localized title for " + language);
                    check(options().stream().noneMatch(o -> o.name().getString().contains("autochattranslator.")), "all option labels localized");
                    check(screen.config.categories().stream().noneMatch(c -> c.name().getString().contains("autochattranslator.")), "all category names localized");
                }
                case 1 -> {
                    capture("config-general.png");
                    clickEnabled();
                    check(Config.ENABLED.get() == originalEnabled, "edits are staged until save");
                    check(ConfigScreenSmokeTest.<Boolean>option("enabled").pendingValue() != originalEnabled, "actual toggle click changes pending value");
                    screen.cancelOrReset();
                    check(Config.ENABLED.get() == originalEnabled, "cancel preserves live setting");
                    open();
                }
                case 2 -> {
                    clickEnabled();
                    screen.finishOrSave();
                    check(Config.ENABLED.get() != originalEnabled, "save applies toggle");
                    try (CommentedFileConfig disk = CommentedFileConfig.of(mc.gameDirectory.toPath().resolve("config/autochattranslator-client.toml"))) {
                        disk.load();
                        check(disk.<Boolean>get("general.enabled") != originalEnabled, "save persists to TOML");
                        check(disk.getComment("general.enabled").contains("总开关"), "Chinese TOML comments");
                    }
                    open();
                    ConfigScreenSmokeTest.<Integer>option("maxLength").requestSet(10);
                    ConfigScreenSmokeTest.<Integer>option("minLength").requestSet(90);
                    check(ConfigScreenSmokeTest.<Integer>option("maxLength").pendingValue() == 90, "minimum raises maximum");
                    ConfigScreenSmokeTest.<Integer>option("maxLength").requestSet(20);
                    check(ConfigScreenSmokeTest.<Integer>option("minLength").pendingValue() == 20, "maximum lowers minimum");
                    Option<String> key = option("openaiApiKey");
                    key.requestSet("test-key-not-a-real-secret");
                    check(key.controller().formatValue().getString().equals("*".repeat(26)), "key masked");
                    check(Config.OPENAI_API_KEY.get().isEmpty(), "unsaved key does not leak into live configuration");
                    ConfigScreenSmokeTest.<OpenAiPreset>option("openaiPreset").requestSet(OpenAiPreset.OPENROUTER);
                    check(ConfigScreenSmokeTest.<ProviderType>option("provider").pendingValue() == ProviderType.OPENAI, "preset selects OpenAI provider");
                    check(ConfigScreenSmokeTest.<String>option("openaiBaseUrl").pendingValue().startsWith("https://openrouter.ai"), "preset fills base URL");
                    check(ConfigScreenSmokeTest.<String>option("openaiModel").pendingValue().equals("openai/gpt-4o-mini"), "preset fills model name");
                    screen.tabNavigationBar.selectTab(1, false);
                }
                case 3 -> {
                    capture("config-provider.png");
                    var keyEntry = optionEntry("openaiApiKey");
                    var keyBounds = keyEntry.widget.getDimension();
                    keyEntry.widget.mouseClicked(keyBounds.xLimit() - 5, keyBounds.y() + keyBounds.height() / 2.0, 0);
                    keyEntry.widget.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_END, 0, 0);
                    check(keyEntry.widget.charTyped('x', 0), "focused key widget consumes keyboard input");
                    Option<String> editedKey = option("openaiApiKey");
                    check(editedKey.pendingValue().endsWith("x"), "key accepts keyboard input");
                    check(editedKey.controller().formatValue().getString().equals("*".repeat(27)), "key remains masked while editing");
                    screen.cancelOrReset();
                    open();
                    check(ConfigScreenSmokeTest.<String>option("openaiApiKey").pendingValue().isEmpty(), "cancel discards key");
                    ButtonOption testButton = buttonOption("test");
                    ConfigScreenSmokeTest.<String>option("openaiBaseUrl").requestSet("");
                    ConfigScreenSmokeTest.<String>option("openaiModel").requestSet("");
                    testButton.action().accept(screen, testButton);
                    check(mc.getToasts().getToast(SystemToast.class, SystemToast.SystemToastIds.PERIODIC_NOTIFICATION) != null,
                            "test connection validates URL and model");
                    for (Option<?> option : options()) {
                        option.requestSetDefault();
                    }
                    screen.finishOrSave();
                    check(Config.ENABLED.get() && Config.MAX_LENGTH.get() == 800 && Config.MIN_LENGTH.get() == 2, "restore defaults and save");
                    TranslationService.toggle();
                    check(!TranslationService.isEnabled(), "temporary pause");
                    TranslationService.onConfigSaved();
                    check(TranslationService.isEnabled(), "saving clears temporary pause");
                    open();
                    screen.tabNavigationBar.selectTab(2, false);
                }
                case 4 -> {
                    capture("config-limits.png");
                    screen.cancelOrReset();
                    ClientConfig.openFromCommand();
                }
                case 5 -> {
                    check(mc.screen instanceof YACLScreen, "deferred command opens settings");
                    mc.screen.onClose();
                    check(mc.screen instanceof TitleScreen, "command screen closes safely outside a world");
                    LogUtils.getLogger().info("[GUI-SMOKE] PASS: registration, Chinese labels, mouse toggle, save, cancel, TOML persistence, defaults, length constraints, masked key, command deferral");
                    step = -1;
                    mc.stop();
                }
            }
        } catch (Throwable error) {
            LogUtils.getLogger().error("[GUI-SMOKE] FAIL at step " + (step - 1), error);
            step = -1;
            mc.stop();
        }
    }

    private static void open() {
        Minecraft mc = Minecraft.getInstance();
        var info = ModList.get().getModContainerById(AutoChatTranslator.MODID).orElseThrow().getModInfo();
        Screen created = ConfigScreenHandler.getScreenFactoryFor(info).orElseThrow().apply(mc, parent);
        check(created instanceof YACLScreen, "Forge Mods configuration factory registered");
        screen = (YACLScreen) created;
        mc.setScreen(screen);
    }

    private static List<Option<?>> options() {
        return screen.config.categories().stream().flatMap(c -> c.groups().stream())
                .flatMap(g -> g.options().stream()).filter(o -> !(o instanceof LabelOption) && !(o instanceof ButtonOption))
                .<Option<?>>map(o -> o).toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> Option<T> option(String key) {
        return (Option<T>) options().stream().filter(o -> o.name().getContents() instanceof TranslatableContents t
                && t.getKey().equals("autochattranslator.option." + key)).findFirst().orElseThrow();
    }

    private static void clickEnabled() {
        clickOption("enabled");
    }

    private static void clickOption(String key) {
        var bounds = optionEntry(key).widget.getDimension();
        screen.mouseClicked(bounds.xLimit() - 5, bounds.y() + bounds.height() / 2.0, 0);
        screen.mouseReleased(bounds.xLimit() - 5, bounds.y() + bounds.height() / 2.0, 0);
    }

    private static ButtonOption buttonOption(String key) {
        return screen.config.categories().stream().flatMap(c -> c.groups().stream())
                .flatMap(g -> g.options().stream())
                .filter(o -> o instanceof ButtonOption button
                        && button.name().getContents() instanceof TranslatableContents t
                        && t.getKey().equals("autochattranslator.button." + key))
                .map(o -> (ButtonOption) o).findFirst().orElseThrow();
    }

    private static OptionListWidget.OptionEntry optionEntry(String key) {        OptionListWidget list = findList(screen);
        check(list != null, "visible option list");
        return list.children().stream().filter(e -> e instanceof OptionListWidget.OptionEntry o
                && o.option == option(key)).map(e -> (OptionListWidget.OptionEntry) e).findFirst().orElseThrow();
    }

    private static OptionListWidget findList(GuiEventListener listener) {
        if (listener instanceof OptionListWidget list) {
            return list;
        }
        if (listener instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                OptionListWidget result = findList(child);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {});
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        LogUtils.getLogger().info("[GUI-SMOKE] OK: {}", description);
    }
}
