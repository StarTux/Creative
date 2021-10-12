package com.winthier.creative.util;

public final class Text {
    private Text() { }

    public static String enumToCamelCase(String in, String glue) {
        String[] toks = in.split("_");
        for (int i = 0; i < toks.length; i += 1) {
            toks[i] = toks[i].substring(0, 1).toUpperCase()
                + toks[i].substring(1).toLowerCase();
        }
        return String.join(glue, toks);
    }

    public static String enumToCamelCase(String in) {
        return enumToCamelCase(in, "");
    }
}
