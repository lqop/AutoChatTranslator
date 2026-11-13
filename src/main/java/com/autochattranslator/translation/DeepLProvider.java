package com.autochattranslator.translation;

import com.autochattranslator.Config;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class DeepLProvider implements TranslationProvider {
    @Override
    public String translate(String text, String sourceLang, String targetLang) throws Exception {
        String key = Config.DEEPL_API_KEY.get();
        if (key.isBlank()) {
            throw new IOException("deeplApiKey is empty");
        }

        StringBuilder form = new StringBuilder("text=").append(HttpUtil.enc(text))
                .append("&target_lang=").append(HttpUtil.enc(toDeepLLanguage(targetLang)));
        if (!"auto".equalsIgnoreCase(sourceLang)) {
            form.append("&source_lang=").append(HttpUtil.enc(toDeepLLanguage(sourceLang)));
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "DeepL-Auth-Key " + key);
        String body = HttpUtil.post(Config.DEEPL_ENDPOINT.get(), form.toString(), "application/x-www-form-urlencoded", headers);

        JsonObject root = HttpUtil.json(body);
        JsonArray translations = root.getAsJsonArray("translations");
        if (translations == null || translations.isEmpty()) {
            throw new IOException("DeepL returned no translation");
        }
        return translations.get(0).getAsJsonObject().get("text").getAsString();
    }

    private static String toDeepLLanguage(String language) {
        String value = language.toLowerCase(Locale.ROOT);
        if (value.startsWith("zh")) {
            return "ZH";
        }
        int dash = value.indexOf('-');
        String base = dash > 0 ? value.substring(0, dash) : value;
        return base.toUpperCase(Locale.ROOT);
    }
}
