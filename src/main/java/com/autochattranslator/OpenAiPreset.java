package com.autochattranslator;

public enum OpenAiPreset {
    CUSTOM(null, null),
    MIMO("https://api.xiaomimimo.com/v1", "mimo-v2.6-flash"),
    OPENROUTER("https://openrouter.ai/api/v1", "openai/gpt-4o-mini"),
    DEEPSEEK("https://api.deepseek.com/v1", "deepseek-flash"),
    OLLAMA("http://localhost:11434/v1", null),
    LMSTUDIO("http://localhost:1234/v1", null);

    final String baseUrl;
    final String model;

    OpenAiPreset(String baseUrl, String model) {
        this.baseUrl = baseUrl;
        this.model = model;
    }
}
