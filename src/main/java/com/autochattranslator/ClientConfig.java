package com.autochattranslator;

import com.autochattranslator.translation.OpenAiProvider;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionEventListener;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.string.StringController;
import dev.isxander.yacl3.gui.controllers.string.StringControllerElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Mod.EventBusSubscriber(modid = AutoChatTranslator.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientConfig {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(ClientConfig::createScreen));
    }

    public static void openFromCommand() {
        Minecraft minecraft = Minecraft.getInstance();
        // ChatScreen closes itself after executing a command, so opening must be queued.
        minecraft.tell(() -> minecraft.setScreen(createScreen(null)));
    }

    public static Screen createScreen(Screen parent) {
        return build().generateScreen(parent);
    }

    static YetAnotherConfigLib build() {
        Option<Integer> minLength = integer("minLength", Config.MIN_LENGTH, 1, 100);
        Option<Integer> maxLength = integer("maxLength", Config.MAX_LENGTH, 10, 4000);
        minLength.addEventListener((option, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE && option.pendingValue() > maxLength.pendingValue()) {
                maxLength.requestSet(option.pendingValue());
            }
        });
        maxLength.addEventListener((option, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE && option.pendingValue() < minLength.pendingValue()) {
                minLength.requestSet(option.pendingValue());
            }
        });

        Option<ProviderType> provider = option("provider", Config.PROVIDER)
                .controller(o -> EnumControllerBuilder.create(o).enumClass(ProviderType.class)
                        .valueFormatter(value -> text("provider." + value.name().toLowerCase(Locale.ROOT))))
                .build();
        Option<String> baseUrl = string("openaiBaseUrl", Config.OPENAI_BASE_URL);
        Option<String> model = string("openaiModel", Config.OPENAI_MODEL);
        Option<String> apiKey = secret("openaiApiKey", Config.OPENAI_API_KEY);

        AtomicReference<OpenAiPreset> presetRef = new AtomicReference<>(OpenAiPreset.CUSTOM);
        Option<OpenAiPreset> preset = Option.<OpenAiPreset>createBuilder()
                .name(text("option.openaiPreset"))
                .description(OptionDescription.of(text("option.openaiPreset.description")))
                .binding(OpenAiPreset.CUSTOM, presetRef::get, presetRef::set)
                .controller(o -> EnumControllerBuilder.create(o).enumClass(OpenAiPreset.class)
                        .valueFormatter(value -> text("preset." + value.name().toLowerCase(Locale.ROOT))))
                .build();
        preset.addEventListener((option, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE) {
                applyPreset(option.pendingValue(), provider, baseUrl, model);
            }
        });
        ButtonOption testButton = ButtonOption.createBuilder()
                .name(text("button.test"))
                .description(OptionDescription.of(text("button.test.description")))
                .action(screen -> testConnection(baseUrl, model, apiKey))
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(text("title"))
                .category(ConfigCategory.createBuilder()
                        .name(text("category.general"))
                        .option(LabelOption.create(text("save_hint")))
                        .option(bool("enabled", Config.ENABLED))
                        .option(string("targetLanguage", Config.TARGET_LANGUAGE))
                        .option(string("sourceLanguage", Config.SOURCE_LANGUAGE))
                        .group(group("messages")
                                .option(bool("translatePlayerChat", Config.TRANSLATE_PLAYER_CHAT))
                                .option(bool("translateSystemChat", Config.TRANSLATE_SYSTEM_CHAT))
                                .option(bool("translateOwnMessages", Config.TRANSLATE_OWN_MESSAGES))
                                .option(bool("ignoreCommands", Config.IGNORE_COMMANDS))
                                .build())
                        .group(group("appearance")
                                .option(bool("showSender", Config.SHOW_SENDER))
                                .option(bool("hoverOriginal", Config.HOVER_ORIGINAL))
                                .option(string("translationPrefix", Config.PREFIX))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(text("category.provider"))
                        .option(LabelOption.create(text("privacy")))
                        .option(provider)
                        .group(group("mymemory")
                                .option(string("mymemoryEndpoint", Config.MYMEMORY_ENDPOINT))
                                .option(string("mymemoryEmail", Config.MYMEMORY_EMAIL))
                                .build())
                        .group(group("openai")
                                .option(preset)
                                .option(LabelOption.create(text("preset_hint")))
                                .option(baseUrl)
                                .option(model)
                                .option(apiKey)
                                .option(integer("openaiMaxTokens", Config.OPENAI_MAX_TOKENS, 32, 4096))
                                .option(testButton)
                                .build())
                        .group(group("google").collapsed(true)
                                .option(string("googleEndpoint", Config.GOOGLE_ENDPOINT))
                                .build())
                        .group(group("deepl").collapsed(true)
                                .option(string("deeplEndpoint", Config.DEEPL_ENDPOINT))
                                .option(secret("deeplApiKey", Config.DEEPL_API_KEY))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(text("category.limits"))
                        .option(minLength)
                        .option(maxLength)
                        .option(integer("cacheSize", Config.CACHE_SIZE, 0, 5000))
                        .option(integer("timeoutSeconds", Config.TIMEOUT_SECONDS, 3, 60))
                        .option(bool("debug", Config.DEBUG))
                        .build())
                .save(() -> {
                    Config.SPEC.save();
                    TranslationService.onConfigSaved();
                })
                .build();
    }

    private static void applyPreset(OpenAiPreset preset, Option<ProviderType> provider,
                                    Option<String> baseUrl, Option<String> model) {
        if (preset.baseUrl == null) {
            return;
        }
        provider.requestSet(ProviderType.OPENAI);
        baseUrl.requestSet(preset.baseUrl);
        if (preset.model != null) {
            model.requestSet(preset.model);
        }
    }

    private static void testConnection(Option<String> baseUrl, Option<String> model, Option<String> apiKey) {
        Minecraft minecraft = Minecraft.getInstance();
        String url = baseUrl.pendingValue();
        String name = model.pendingValue();
        String key = apiKey.pendingValue();
        if (url.isBlank() || name.isBlank()) {
            toast(minecraft, text("toast.test.failed.title"), text("toast.test.missing"));
            return;
        }

        String target = Config.TARGET_LANGUAGE.get();
        toast(minecraft, text("toast.test.running.title"), text("toast.test.running", name));
        CompletableFuture.runAsync(() -> {
            Component title;
            Component message;
            try {
                String translated = OpenAiProvider.translate("Hello, how are you?", target, url, name, key, 128);
                title = text("toast.test.success.title");
                message = text("toast.test.success.message", translated);
            } catch (Exception exception) {
                title = text("toast.test.failed.title");
                message = text("toast.test.failed.message", String.valueOf(exception.getMessage()));
            }
            final Component toastTitle = title;
            final Component toastMessage = message;
            minecraft.execute(() -> toast(minecraft, toastTitle, toastMessage));
        });
    }

    private static void toast(Minecraft minecraft, Component title, Component message) {
        minecraft.getToasts().addToast(SystemToast.multiline(
                minecraft, SystemToast.SystemToastIds.PERIODIC_NOTIFICATION, title, message));
    }

    private static <T> Option.Builder<T> option(String key, ForgeConfigSpec.ConfigValue<T> value) {
        return Option.<T>createBuilder()
                .name(text("option." + key))
                .description(OptionDescription.of(text("option." + key + ".description")))
                .binding(value.getDefault(), value::get, value::set);
    }

    private static Option<Boolean> bool(String key, ForgeConfigSpec.BooleanValue value) {
        return option(key, value).controller(o -> BooleanControllerBuilder.create(o).onOffFormatter()).build();
    }

    private static Option<String> string(String key, ForgeConfigSpec.ConfigValue<String> value) {
        return string(key, value, false);
    }

    private static Option<String> secret(String key, ForgeConfigSpec.ConfigValue<String> value) {
        return string(key, value, true);
    }

    private static Option<String> string(String key, ForgeConfigSpec.ConfigValue<String> value, boolean secret) {
        return option(key, value).customController(o -> new StringController(o) {
            @Override
            public Component formatValue() {
                return secret ? Component.literal("*".repeat(getString().length())) : super.formatValue();
            }

            @Override
            public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> dimension) {
                return new StringControllerElement(this, screen, dimension, true) {
                    @Override
                    protected Component getValueText() {
                        if (!inputFieldFocused && inputField.isEmpty()) {
                            return text("empty_field").copy().withStyle(ChatFormatting.GRAY);
                        }
                        return super.getValueText();
                    }
                };
            }
        }).build();
    }

    private static Option<Integer> integer(String key, ForgeConfigSpec.IntValue value, int min, int max) {
        return option(key, value).controller(o -> IntegerFieldControllerBuilder.create(o).range(min, max)).build();
    }

    private static OptionGroup.Builder group(String key) {
        return OptionGroup.createBuilder().name(text("group." + key));
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("autochattranslator." + key, args);
    }

    private ClientConfig() {
    }
}
