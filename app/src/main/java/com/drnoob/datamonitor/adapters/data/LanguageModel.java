package com.drnoob.datamonitor.adapters.data;

import java.io.Serializable;

public class LanguageModel implements Serializable {
    private String language;
    private String languageCode;

    public LanguageModel() {
        // empty constructor
    }

    public LanguageModel(String language, String languageCode) {
        this.language = language;
        this.languageCode = languageCode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
}
