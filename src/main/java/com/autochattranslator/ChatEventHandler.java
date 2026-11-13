package com.autochattranslator;

import com.autochattranslator.chat.ChatMessageParser;
import com.autochattranslator.chat.ParsedChat;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AutoChatTranslator.MODID, value = Dist.CLIENT)
public final class ChatEventHandler {
    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent event) {
        if (!TranslationService.isEnabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        boolean systemChat = event.isSystem();
        if (systemChat ? !Config.TRANSLATE_SYSTEM_CHAT.get() : !Config.TRANSLATE_PLAYER_CHAT.get()) {
            return;
        }
        if (!systemChat && !Config.TRANSLATE_OWN_MESSAGES.get()
                && event.getSender() != null
                && event.getSender().equals(minecraft.player.getUUID())) {
            return;
        }

        ParsedChat parsed = ChatMessageParser.parse(event.getMessage(), systemChat);
        TranslationService.translate(parsed);
    }

    private ChatEventHandler() {
    }
}
