package com.autochattranslator.translation;

import com.autochattranslator.Config;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class OpenAiProvider implements TranslationProvider {
    private static final Gson GSON = new Gson();

    @Override
    public String translate(String text, String sourceLang, String targetLang) throws Exception {
        return translate(text, targetLang, Config.OPENAI_BASE_URL.get(), Config.OPENAI_MODEL.get(),
                Config.OPENAI_API_KEY.get(), Config.OPENAI_MAX_TOKENS.get());
    }

    public static String translate(String text, String targetLang, String baseUrl, String model,
                                   String key, int maxTokens) throws Exception {
        baseUrl = baseUrl.trim().replaceAll("/+$", "");
        if (baseUrl.isBlank()) {
            throw new IOException("openaiBaseUrl is empty");
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("model", model.trim());
        payload.addProperty("temperature", 0.2);
        payload.addProperty("max_tokens", maxTokens);
        payload.addProperty("stream", false);

        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", "You are a translation engine for Minecraft chat. Translate the user's message into "
                + targetLang + ". Preserve names, numbers, links and tone. Output only the translation, with no quotes or explanation.");
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", text);
        messages.add(user);
        payload.add("messages", messages);

        Map<String, String> headers = new HashMap<>();
        key = key.trim();
        if (!key.isBlank()) {
            headers.put("Authorization", "Bearer " + key);
        }

        String body = HttpUtil.post(baseUrl + "/chat/completions", GSON.toJson(payload), "application/json", headers);
        JsonObject root = HttpUtil.json(body);
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IOException("OpenAI response has no choices: " + HttpUtil.abbreviate(body));
        }

        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        String content = message != null && message.has("content") && !message.get("content").isJsonNull()
                ? message.get("content").getAsString().trim()
                : "";
        if (content.isBlank()) {
            throw new IOException("OpenAI returned an empty translation");
        }
        if (content.length() >= 2 && content.startsWith("\"") && content.endsWith("\"")) {
            content = content.substring(1, content.length() - 1);
        }
        return content;
    }
}
