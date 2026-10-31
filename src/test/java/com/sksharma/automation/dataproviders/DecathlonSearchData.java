package com.sksharma.automation.dataproviders;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

/** Test query fixtures live separately from the search assertions. */
public final class DecathlonSearchData {
    private static final Properties DATA = load();

    private DecathlonSearchData() { }

    public static Object[][] validQueries() {
        return Arrays.stream(DATA.getProperty("valid.queries").split("\\|"))
                .map(query -> new Object[]{query})
                .toArray(Object[][]::new);
    }

    public static String noResultsQuery() {
        return DATA.getProperty("no.results.query");
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream input = DecathlonSearchData.class.getClassLoader()
                .getResourceAsStream("testdata/decathlon-search.properties")) {
            if (input == null) throw new IllegalStateException("Decathlon search fixtures are missing");
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load Decathlon search fixtures", e);
        }
    }
}
