package com.app.utils;

import java.text.Normalizer;

public class H2Functions {
    public static String unaccent(String value) {
        if (value == null) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }
}
