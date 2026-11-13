package com.autochattranslator.translation;

import com.autochattranslator.Config;

public final class Providers {
    public static TranslationProvider create() {
        return switch (Config.PROVIDER.get()) {
            case MYMEMORY -> new MyMemoryProvider();
            case GOOGLE -> new GoogleProvider();
            case OPENAI -> new OpenAiProvider();
            case DEEPL -> new DeepLProvider();
        };
    }

    private Providers() {
    }
}
