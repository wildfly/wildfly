/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.controller.operations.global;

import java.util.Locale;

/**
 * Utility class handling low-level parsing of Locale string tag.
 *
 * @author Romain Pelisse - <romain@redhat.com>
 */
public final class LocaleResolver {

    private static final String ENGLISH = new Locale("en").getLanguage();

    private LocaleResolver(){};

    static Locale resolveLocale(String unparsed) throws IllegalArgumentException {
        int len = unparsed.length();
        if ( len < 1 || len > 7 ) {
            throw new IllegalArgumentException(unparsed);
        }

        if (len != 2 && len != 5 && len < 7) {
            throw new IllegalArgumentException(unparsed);
        }

        char char0 = unparsed.charAt(0);
        char char1 = unparsed.charAt(1);
        if (char0 < 'a' || char0 > 'z' || char1 < 'a' || char1 > 'z') {
            throw new IllegalArgumentException(unparsed);
        }
        if (len == 2) {
            return replaceByRootLocaleIfLanguageIsEnglish(new Locale(unparsed, ""));
        }

        if (!isLocaleSeparator(unparsed.charAt(2))) {
            throw new IllegalArgumentException(unparsed);
        }

        char char3 = unparsed.charAt(3);
        if (isLocaleSeparator(char3)) {
            // no country
            return replaceByRootLocaleIfLanguageIsEnglish(new Locale(unparsed.substring(0, 2), "", unparsed.substring(4)));
        }

        char char4 = unparsed.charAt(4);
        if (char3 < 'A' || char3 > 'Z' || char4 < 'A' || char4 > 'Z') {
            throw new IllegalArgumentException(unparsed);
        }

        if (len == 5) {
            return replaceByRootLocaleIfLanguageIsEnglish(new Locale(unparsed.substring(0, 2), unparsed.substring(3)));
        }

        if (!isLocaleSeparator(unparsed.charAt(5))) {
            throw new IllegalArgumentException(unparsed);
        }
        return replaceByRootLocaleIfLanguageIsEnglish(new Locale(unparsed.substring(0, 2), unparsed.substring(3, 5), unparsed.substring(6)));
    }

    private static boolean isLocaleSeparator(char ch) {
        return ch == '-' || ch == '_';
    }

    static Locale replaceByRootLocaleIfLanguageIsEnglish(Locale locale) {
        return (locale.getLanguage().equals(ENGLISH) ? Locale.ROOT : locale);
    }

}
