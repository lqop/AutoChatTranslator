package com.autochattranslator.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatMessageParser {
    private static final Pattern ANGLE_FORMAT = Pattern.compile("^<([^>]{1,32})>\\s*(.*)$", Pattern.DOTALL);

    public static ParsedChat parse(Component message, boolean systemChat) {
        String full = message.getString();
        if (systemChat) {
            return new ParsedChat(null, full, false);
        }

        if (message.getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            Object[] args = translatable.getArgs();
            if (args.length >= 2 && ("chat.type.text".equals(key) || "chat.type.announcement".equals(key))) {
                return new ParsedChat(asString(args[0]), asString(args[1]), true);
            }
            if (args.length >= 2 && "chat.type.emote".equals(key)) {
                return new ParsedChat(asString(args[0]), asString(args[1]), true);
            }
            if (args.length >= 3 && ("chat.type.team.text".equals(key) || "chat.type.team.sent".equals(key))) {
                return new ParsedChat(asString(args[1]), asString(args[2]), true);
            }
        }

        Matcher matcher = ANGLE_FORMAT.matcher(full);
        if (matcher.matches()) {
            return new ParsedChat(matcher.group(1), matcher.group(2), true);
        }
        return new ParsedChat(null, full, true);
    }

    private static String asString(Object value) {
        return value instanceof Component component ? component.getString() : String.valueOf(value);
    }

    private ChatMessageParser() {
    }
}
