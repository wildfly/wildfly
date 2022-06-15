package org.jboss.as.jdr.util;

import org.jboss.as.jdr.vfs.Filters;

/**
 * Provides the most commonly used sanitizers with their most common configurations.
 */
public class Sanitizers {

    /**
     * creates and returns a {@link Sanitizer} instance that only operates on
     * files that end with a {@code .properties} suffix.
     *
     * @param pattern {@link WildcardPattern} compatible pattern to search for
     * @param replacement text content to replace matches with
     * @return {@link Sanitizer} that only operates on files with names ending with {@code .properties}.
     * @throws Exception
     */
    public static Sanitizer pattern(String pattern, String replacement) throws Exception {
        return new PatternSanitizer(pattern, replacement, Filters.suffix(".properties"));
    }

    /**
     * creates and returns a {@link Sanitizer} instance that only operates on
     * files that end with a {@code .xml} suffix.
     *
     * @param xpath to search for and nullify
     * @return a {@link Sanitizer} instance that only operates on files that end with a {@code .xml} suffix.
     * @throws Exception
     */
    public static Sanitizer xml(String xpath) throws Exception {
        return new XMLSanitizer(xpath, Filters.suffix(".xml"));
    }
}
