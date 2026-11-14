package com.autochattranslator;

import com.autochattranslator.chat.ParsedChat;
import com.autochattranslator.translation.Providers;
import com.autochattranslator.translation.TranslationProvider;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class TranslationService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> CACHE = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > Config.CACHE_SIZE.get();
        }
    });
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
            2, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(128),
            runnable -> {
                Thread thread = new Thread(runnable, "AutoChatTranslator");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy());

    private static volatile boolean runtimeEnabled = true;
    private static final AtomicLong CONFIG_REVISION = new AtomicLong();

    public static void onConfigSaved() {
        CONFIG_REVISION.incrementAndGet();
        CACHE.clear();
        runtimeEnabled = true;
    }

    public static boolean isEnabled() {
        return runtimeEnabled && Config.ENABLED.get();
    }

    public static void toggle() {
        runtimeEnabled = !runtimeEnabled;
    }

    public static Component status() {
        return Component.translatable("autochattranslator.status",
                Component.translatable(isEnabled() ? "autochattranslator.status.on" : "autochattranslator.status.off"),
                Config.PROVIDER.get(),
                Config.SOURCE_LANGUAGE.get(),
                Config.TARGET_LANGUAGE.get(),
                CACHE.size());
    }

    public static void translate(ParsedChat chat) {
        if (!isEnabled()) {
            return;
        }
        String text = chat.content().strip();
        if (!shouldTranslate(text, chat)) {
            return;
        }

        String source = Config.SOURCE_LANGUAGE.get();
        String target = Config.TARGET_LANGUAGE.get();
        long revision = CONFIG_REVISION.get();
        String cacheKey = revision + "|" + source + "|" + target + "|" + text;

        String cached = CACHE.get(cacheKey);
        if (cached != null) {
            Minecraft.getInstance().execute(() -> {
                if (revision == CONFIG_REVISION.get() && isEnabled()) {
                    show(chat, cached);
                }
            });
            return;
        }
        if (!PENDING.add(cacheKey)) {
            return;
        }

        try {
            EXECUTOR.execute(() -> {
                try {
                    if (revision != CONFIG_REVISION.get() || !isEnabled()) {
                        return;
                    }
                    TranslationProvider provider = Providers.create();
                    String translated = provider.translate(text, source, target).strip();
                    if (revision == CONFIG_REVISION.get() && !translated.isEmpty() && !translated.equalsIgnoreCase(text)) {
                        CACHE.put(cacheKey, translated);
                        Minecraft minecraft = Minecraft.getInstance();
                        minecraft.execute(() -> {
                            if (revision == CONFIG_REVISION.get() && isEnabled()) {
                                show(chat, translated);
                            }
                        });
                    }
                } catch (Exception exception) {
                    LOGGER.warn("[AutoChatTranslator] translation failed: {}", exception.toString());
                    if (Config.DEBUG.get()) {
                        LOGGER.warn("[AutoChatTranslator] stack trace", exception);
                    }
                } finally {
                    PENDING.remove(cacheKey);
                }
            });
        } catch (RejectedExecutionException rejected) {
            PENDING.remove(cacheKey);
            LOGGER.warn("[AutoChatTranslator] translation queue is full, message skipped");
        }
    }

    public static void translateTest(String text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.gui.getChat().addMessage(Component.translatable("autochattranslator.test.pending").withStyle(ChatFormatting.GRAY));
        try {
            EXECUTOR.execute(() -> {
                try {
                    String translated = Providers.create()
                            .translate(text, Config.SOURCE_LANGUAGE.get(), Config.TARGET_LANGUAGE.get())
                            .strip();
                    minecraft.execute(() -> minecraft.gui.getChat().addMessage(
                            Component.translatable("autochattranslator.test.result", translated).withStyle(ChatFormatting.GRAY)));
                } catch (Exception exception) {
                    minecraft.execute(() -> minecraft.gui.getChat().addMessage(
                            Component.translatable("autochattranslator.test.failed", String.valueOf(exception.getMessage())).withStyle(ChatFormatting.RED)));
                }
            });
        } catch (RejectedExecutionException rejected) {
            minecraft.gui.getChat().addMessage(Component.translatable("autochattranslator.test.queue_full").withStyle(ChatFormatting.RED));
        }
    }

    private static boolean shouldTranslate(String text, ParsedChat chat) {
        if (text.isEmpty() || text.length() < Config.MIN_LENGTH.get() || text.length() > Config.MAX_LENGTH.get()) {
            return false;
        }
        if (Config.IGNORE_COMMANDS.get() && text.startsWith("/")) {
            return false;
        }
        if (!Config.PREFIX.get().isEmpty() && text.startsWith(Config.PREFIX.get())) {
            return false;
        }
        if (text.codePoints().noneMatch(Character::isLetter)) {
            return false;
        }
        String target = Config.TARGET_LANGUAGE.get().toLowerCase(Locale.ROOT);
        if (target.startsWith("zh") && isMostlyChinese(text)) {
            return false;
        }
        if (Config.TARGET_LANGUAGE.get().equalsIgnoreCase(Config.SOURCE_LANGUAGE.get())) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        String self = minecraft.player != null ? minecraft.player.getGameProfile().getName() : null;
        if (!Config.TRANSLATE_OWN_MESSAGES.get() && self != null && chat.sender() != null && chat.sender().contains(self)) {
            return false;
        }
        return self != null;
    }

    private static boolean isMostlyChinese(String text) {
        int letters = 0;
        int chinese = 0;
        for (int codePoint : text.codePoints().toArray()) {
            if (codePoint >= 0x3040 && codePoint <= 0x30FF) {
                return false;
            }
            if (Character.isLetter(codePoint)) {
                letters++;
                if (codePoint >= 0x4E00 && codePoint <= 0x9FFF) {
                    chinese++;
                }
            }
        }
        return letters > 0 && chinese * 100 / letters >= 60;
    }

    private static void show(ParsedChat chat, String translated) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui == null) {
            return;
        }
        String sender = chat.playerChat() && Config.SHOW_SENDER.get() && chat.sender() != null
                ? "<" + chat.sender() + "> "
                : "";
        MutableComponent line = Component.literal(Config.PREFIX.get() + sender + translated);
        if (Config.HOVER_ORIGINAL.get() && !chat.content().isBlank()) {
            String original = chat.content();
            line = line.copy().withStyle(style -> style
                    .withColor(ChatFormatting.GRAY)
                    .withItalic(true)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.translatable("autochattranslator.hover.original", original)
                                    .append("\n")
                                    .append(Component.translatable("autochattranslator.hover.copy")
                                            .withStyle(ChatFormatting.DARK_GRAY))))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, original)));
        } else {
            line = line.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        }
        minecraft.gui.getChat().addMessage(line);
    }

    private TranslationService() {
    }
}
