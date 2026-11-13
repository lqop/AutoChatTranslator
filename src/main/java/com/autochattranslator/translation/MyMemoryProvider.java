package com.autochattranslator.translation;

import com.autochattranslator.Config;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Collections;

public final class MyMemoryProvider implements TranslationProvider {
    @Override
    public String translate(String text, String sourceLang, String targetLang) throws Exception {
        String source = "auto".equalsIgnoreCase(sourceLang) ? "autodetect" : sourceLang;
        String url = Config.MYMEMORY_ENDPOINT.get()
                + "?q=" + HttpUtil.enc(text)
                + "&langpair=" + HttpUtil.enc(source + "|" + targetLang);
        String email = Config.MYMEMORY_EMAIL.get();
        if (!email.isBlank()) {
            url += "&de=" + HttpUtil.enc(email);
        }

        JsonObject root = HttpUtil.json(HttpUtil.get(url, Collections.emptyMap()));
        if (root.has("quotaFinished") && root.get("quotaFinished").getAsBoolean()) {
            throw new IOException("MyMemory daily quota finished");
        }
        int status = root.has("responseStatus") ? Integer.parseInt(root.get("responseStatus").getAsString()) : 200;
        if (status != 200) {
            String details = root.has("responseDetails") ? root.get("responseDetails").getAsString() : "";
            throw new IOException("MyMemory status " + status + " " + details);
        }

        JsonObject data = root.getAsJsonObject("responseData");
        String translated = data != null && data.has("translatedText") ? data.get("translatedText").getAsString() : "";
        if (translated.isBlank()) {
            throw new IOException("MyMemory returned an empty translation");
        }
        return unescape(translated);
    }

    private static String unescape(String value) {
        return value
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }
}
