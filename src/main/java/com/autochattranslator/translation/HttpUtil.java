package com.autochattranslator.translation;

import com.autochattranslator.Config;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public final class HttpUtil {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static String get(String url, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = base(url)
                .header("Accept", "application/json, text/plain, */*")
                .GET();
        headers.forEach(builder::header);
        return send(builder.build());
    }

    public static String post(String url, String body, String contentType, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = base(url)
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        headers.forEach(builder::header);
        return send(builder.build());
    }

    public static JsonObject json(String body) {
        return JsonParser.parseString(body).getAsJsonObject();
    }

    public static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static String abbreviate(String value) {
        String oneLine = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 200 ? oneLine : oneLine.substring(0, 200) + "...";
    }

    private static HttpRequest.Builder base(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(Config.TIMEOUT_SECONDS.get()))
                .header("User-Agent", "AutoChatTranslator/1.0 (Minecraft Forge mod)");
    }

    private static String send(HttpRequest request) throws Exception {
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + ": " + abbreviate(response.body()));
        }
        return response.body();
    }

    private HttpUtil() {
    }
}
