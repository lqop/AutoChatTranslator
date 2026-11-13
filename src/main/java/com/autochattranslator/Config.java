package com.autochattranslator;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> TARGET_LANGUAGE;
    public static final ForgeConfigSpec.ConfigValue<String> SOURCE_LANGUAGE;
    public static final ForgeConfigSpec.BooleanValue TRANSLATE_PLAYER_CHAT;
    public static final ForgeConfigSpec.BooleanValue TRANSLATE_SYSTEM_CHAT;
    public static final ForgeConfigSpec.BooleanValue TRANSLATE_OWN_MESSAGES;
    public static final ForgeConfigSpec.BooleanValue IGNORE_COMMANDS;
    public static final ForgeConfigSpec.BooleanValue SHOW_SENDER;
    public static final ForgeConfigSpec.ConfigValue<String> PREFIX;

    public static final ForgeConfigSpec.EnumValue<ProviderType> PROVIDER;
    public static final ForgeConfigSpec.ConfigValue<String> MYMEMORY_ENDPOINT;
    public static final ForgeConfigSpec.ConfigValue<String> MYMEMORY_EMAIL;
    public static final ForgeConfigSpec.ConfigValue<String> GOOGLE_ENDPOINT;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_BASE_URL;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_MODEL;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY;
    public static final ForgeConfigSpec.IntValue OPENAI_MAX_TOKENS;
    public static final ForgeConfigSpec.ConfigValue<String> DEEPL_ENDPOINT;
    public static final ForgeConfigSpec.ConfigValue<String> DEEPL_API_KEY;

    public static final ForgeConfigSpec.IntValue MIN_LENGTH;
    public static final ForgeConfigSpec.IntValue MAX_LENGTH;
    public static final ForgeConfigSpec.IntValue CACHE_SIZE;
    public static final ForgeConfigSpec.IntValue TIMEOUT_SECONDS;
    public static final ForgeConfigSpec.BooleanValue DEBUG;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("自动聊天翻译：常规设置").push("general");
        ENABLED = b.comment("自动聊天翻译总开关。")
                .define("enabled", true);
        TARGET_LANGUAGE = b.comment("目标语言代码，例如 zh-CN（简体中文）或 en（英语）。")
                .define("targetLanguage", "zh-CN");
        SOURCE_LANGUAGE = b.comment("源语言代码，例如 zh-CN（简体中文）、en（英语）或 auto（自动检测）。")
                .define("sourceLanguage", "auto");
        TRANSLATE_PLAYER_CHAT = b.comment("翻译玩家聊天消息。")
                .define("translatePlayerChat", true);
        TRANSLATE_SYSTEM_CHAT = b.comment("翻译系统或服务器消息，某些模组和 NPC 台词也属于此类；默认关闭以避免刷屏。")
                .define("translateSystemChat", false);
        TRANSLATE_OWN_MESSAGES = b.comment("同时翻译自己发送的消息。")
                .define("translateOwnMessages", false);
        IGNORE_COMMANDS = b.comment("跳过以斜杠开头的消息。")
                .define("ignoreCommands", true);
        SHOW_SENDER = b.comment("在译文行中显示发送者名称。")
                .define("showSender", true);
        PREFIX = b.comment("译文行的前缀。")
                .define("translationPrefix", "[译] ");
        b.pop();

        b.comment("翻译服务设置。隐私提示：待翻译文字会发送至所选接口。").push("provider");
        PROVIDER = b.comment("MYMEMORY 无需密钥；GOOGLE 在部分地区可能需要代理；OPENAI 支持 OpenAI 兼容接口；DEEPL 需要密钥。")
                .defineEnum("provider", ProviderType.MYMEMORY);
        MYMEMORY_ENDPOINT = b.comment("MyMemory 接口地址。")
                .define("mymemoryEndpoint", "https://api.mymemory.translated.net/get");
        MYMEMORY_EMAIL = b.comment("可选邮箱，用于提高 MyMemory 每日配额。")
                .define("mymemoryEmail", "");
        GOOGLE_ENDPOINT = b.comment("谷歌翻译接口地址。")
                .define("googleEndpoint", "https://translate.googleapis.com/translate_a/single");
        OPENAI_BASE_URL = b.comment("OpenAI 兼容接口基础地址，默认使用 Xiaomi MiMo。")
                .define("openaiBaseUrl", "https://api.xiaomimimo.com/v1");
        OPENAI_MODEL = b.comment("OpenAI 兼容接口使用的模型名称。")
                .define("openaiModel", "mimo-v2.6-flash");
        OPENAI_API_KEY = b.comment("OpenAI 兼容接口的 API 密钥，以明文保存在本机 TOML 配置文件中；Ollama 等无需认证的本地服务可留空。")
                .define("openaiApiKey", "");
        OPENAI_MAX_TOKENS = b.comment("每次翻译请求的最大输出令牌数。")
                .defineInRange("openaiMaxTokens", 512, 32, 4096);
        DEEPL_ENDPOINT = b.comment("DeepL 接口地址，免费版密钥请使用 api-free.deepl.com。")
                .define("deeplEndpoint", "https://api-free.deepl.com/v2/translate");
        DEEPL_API_KEY = b.comment("DeepL API 密钥，以明文保存在本机 TOML 配置文件中。")
                .define("deeplApiKey", "");
        b.pop();

        b.comment("限制与诊断设置").push("limits");
        MIN_LENGTH = b.comment("消息最小长度，单位：字符；短于此值的消息不翻译。")
                .defineInRange("minLength", 2, 1, 100);
        MAX_LENGTH = b.comment("消息最大长度，单位：字符；长于此值的消息不翻译。")
                .defineInRange("maxLength", 800, 10, 4000);
        CACHE_SIZE = b.comment("内存中缓存的译文条数。")
                .defineInRange("cacheSize", 512, 0, 5000);
        TIMEOUT_SECONDS = b.comment("HTTP 请求超时时间，单位：秒。")
                .defineInRange("timeoutSeconds", 12, 3, 60);
        DEBUG = b.comment("翻译失败时输出异常堆栈。")
                .define("debug", false);
        b.pop();

        SPEC = b.build();
    }

    private Config() {
    }
}
