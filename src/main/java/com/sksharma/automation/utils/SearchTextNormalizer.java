package com.sksharma.automation.utils;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Normalizes product-query words for lexical search-result evidence. */
public final class SearchTextNormalizer {

    private SearchTextNormalizer() { }

    public static Set<String> meaningfulTerms(String text) {
        return Set.copyOf(tokens(text));
    }

    public static String primaryProductTerm(String query) {
        List<String> tokens = tokens(query);
        return tokens.isEmpty() ? "" : tokens.get(tokens.size() - 1);
    }

    private static List<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split("[^\\p{L}\\p{N}]+"))
                .filter(token -> token.length() >= 4)
                .map(SearchTextNormalizer::singularizeEnglishPlural)
                .toList();
    }

    private static String singularizeEnglishPlural(String token) {
        if (token.length() > 4 && token.endsWith("ies")) {
            return token.substring(0, token.length() - 3) + "y";
        }
        if (token.length() > 4 && (token.endsWith("sses")
                || token.endsWith("shes") || token.endsWith("ches")
                || token.endsWith("xes") || token.endsWith("zes"))) {
            return token.substring(0, token.length() - 2);
        }
        if (token.length() > 4 && token.endsWith("s")
                && !token.endsWith("ss")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }
}
