package com.autochattranslator;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AutoChatTranslator.MODID, value = Dist.CLIENT)
public final class ClientCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ctrans")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> TranslationService.status(), false);
                    return 1;
                })
                .then(Commands.literal("status").executes(context -> {
                    context.getSource().sendSuccess(() -> TranslationService.status(), false);
                    return 1;
                }))
                .then(Commands.literal("toggle").executes(context -> {
                    TranslationService.toggle();
                    context.getSource().sendSuccess(() -> TranslationService.status(), false);
                    return 1;
                }))
                .then(Commands.literal("config").executes(context -> {
                    ClientConfig.openFromCommand();
                    return 1;
                }))
                .then(Commands.literal("test")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(context -> {
                                    TranslationService.translateTest(StringArgumentType.getString(context, "text"));
                                    return 1;
                                }))));
    }

    private ClientCommands() {
    }
}
