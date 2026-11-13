package com.autochattranslator.translation;

import com.autochattranslator.Config;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Collections;

public final class GoogleProvider implements TranslationProvider {
    @Override
    public String translate(String text, String sourceLang, String targetLang) throws Exception {
        String url = Config.GOOGLE_ENDPOINT.get()
                + "?client=gtx&dt=t&sl=" + HttpUtil.enc(sourceLang)
                + "&tl=" + HttpUtil.enc(targetLang)
                + "&q=" + HttpUtil.enc(text);

        String body = HttpUtil.get(url, Collections.emptyMap());
        JsonArray root = JsonParser.parseString(body).getAsJsonArray();
        if (root.isEmpty() || !root.get(0).isJsonArray()) {
            throw new IOException("Unexpected Google response");
        }

        StringBuilder result = new StringBuilder();
        for (JsonElement element : root.get(0).getAsJsonArray()) {
            JsonArray segment = element.getAsJsonArray();
            if (!segment.isEmpty() && !segment.get(0).isJsonNull()) {
                result.append(segment.get(0).getAsString());
            }
        }
        if (result.length() == 0) {
            throw new IOException("Google returned an empty translation");
        }
        return result.toString();
    }
}
