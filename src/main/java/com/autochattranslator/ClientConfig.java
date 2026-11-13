package com.autochattranslator;

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
                                .option(string("translationPrefix", Config.PREFIX))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(text("category.provider"))
                        .option(LabelOption.create(text("privacy")))
                        .option(option("provider", Config.PROVIDER)
                                .controller(o -> EnumControllerBuilder.create(o).enumClass(ProviderType.class)
                                        .valueFormatter(value -> text("provider." + value.name().toLowerCase(Locale.ROOT))))
                                .build())
                        .group(group("mymemory")
                                .option(string("mymemoryEndpoint", Config.MYMEMORY_ENDPOINT))
                                .option(string("mymemoryEmail", Config.MYMEMORY_EMAIL))
                                .build())
                        .group(group("openai")
                                .option(string("openaiBaseUrl", Config.OPENAI_BASE_URL))
                                .option(string("openaiModel", Config.OPENAI_MODEL))
                                .option(secret("openaiApiKey", Config.OPENAI_API_KEY))
                                .option(integer("openaiMaxTokens", Config.OPENAI_MAX_TOKENS, 32, 4096))
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

    private static Component text(String key) {
        return Component.translatable("autochattranslator." + key);
    }

    private ClientConfig() {
    }
}
