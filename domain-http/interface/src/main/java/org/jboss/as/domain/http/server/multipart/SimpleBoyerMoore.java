/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.domain.http.server.multipart;

/**
 * <code>SimpleBoyerMoore</code> is an implementation of the simplified version of the Boyer-Moore pattern search algorithm.
 * This just means that the "good match" portion of the algorithm is removed, which improves the performance of non repeating
 * patterns with the obvious side-effect of reducing repeating pattern performance (e.g. gaggaggaaaggaggaagagaggaga). This
 * version of the algorithm performs incredibly well if the pattern is rare, for example a MIME boundary. This algorithm is
 * binary safe.
 *
 * @author Jason T. Greene
 */
public class SimpleBoyerMoore {
    private int[] badMatch = new int[256];

    private byte[] pattern;

    public static final int PATTERN_NOT_FOUND = -1;

    /**
     * Constructs a <code>SimpleBoyerMoore</code> instance. This internally stores the pattern so that the same instance can be
     * used across several searches.
     *
     * @param pattern the pattern to search for
     */
    public SimpleBoyerMoore(byte[] pattern) {
        this.pattern = pattern;
        precomputeBadMatchTable();
    }

    private void precomputeBadMatchTable() {
        java.util.Arrays.fill(badMatch, pattern.length);
        for (int i = 0; i < pattern.length - 1; i++) {
            badMatch[pattern[i] & 0xff] = pattern.length - i - 1;
        }
    }

    /**
     * Find an occurrence of the search pattern within text.
     *
     * @param text a byte array of data to search
     * @param offset the index in text to start searching from
     * @param length the maximum number of bytes to search
     * @return if a match is found, the index of text where the patter occurs, otherwise {@link #PATTERN_NOT_FOUND}
     */
    public int patternSearch(byte[] text, int offset, int length) {
        if (pattern.length > length) {
            return PATTERN_NOT_FOUND;
        }

        int i = 0, j = 0, k = 0;
        int end = offset + length;

        for (i = offset + pattern.length - 1; i < end; i += badMatch[text[i] & 0xff]) {
            for (j = pattern.length - 1, k = i; (j >= 0) && (text[k] == pattern[j]); j--) {
                k--;
            }
            if (j == -1) {
                return k + 1;
            }
        }

        return PATTERN_NOT_FOUND;
    }
}