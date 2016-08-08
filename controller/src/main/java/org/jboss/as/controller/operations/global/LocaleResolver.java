/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
        if ( len < 1 ) {
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

    /**
     * <p>By substituting Locale.ROOT for any locale that includes English, we're counting on the fact that the locale is in the end
     * used in a call to {@link java.util.ResourceBundle#getBundle(java.lang.String, java.util.Locale, java.lang.ClassLoader) }.</p>
     *
     * <p>Note that Locale.ROOT bypasses the default locale (which could be French or German). It relies on the convention we used for
     *    naming bundles files in Wildfly (LocalDescriptions.properties, not LocalDescriptions_en.properties).</p>
     */
    static Locale replaceByRootLocaleIfLanguageIsEnglish(Locale locale) {
        return (locale.getLanguage().equals(ENGLISH) ? Locale.ROOT : locale);
    }
}
