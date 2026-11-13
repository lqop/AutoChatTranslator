package com.autochattranslator.translation;

public interface TranslationProvider {
    String translate(String text, String sourceLang, String targetLang) throws Exception;
}
