/*
 * Copyright (c) 1999, 2001, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.jboss.as.jacorb.naming.jndi;

import java.net.MalformedURLException;
import java.io.UnsupportedEncodingException;

import org.jboss.as.jacorb.JacORBMessages;

/**
 * Utilities for dealing with URLs.
 *
 * @author Vincent Ryan
 */

public final class UrlUtil {

    // To prevent creation of this static class
    private UrlUtil() {
    }

    /**
     * Decode a URI string (according to RFC 2396).
     */
    public static String decode(String s) throws MalformedURLException {
        try {
            return decode(s, "8859_1");
        } catch (UnsupportedEncodingException e) {
            // ISO-Latin-1 should always be available?
            throw JacORBMessages.MESSAGES.unavailableISOLatin1Decoder();
        }
    }

    /**
     * Decode a URI string (according to RFC 2396).
     * <p/>
     * Three-character sequences '%xy', where 'xy' is the two-digit
     * hexadecimal representation of the lower 8-bits of a character,
     * are decoded into the character itself.
     * <p/>
     * The string is subsequently converted using the specified encoding
     */
    public static String decode(String s, String enc)
            throws MalformedURLException, UnsupportedEncodingException {

        int length = s.length();
        byte[] bytes = new byte[length];
        int j = 0;

        for (int i = 0; i < length; i++) {
            if (s.charAt(i) == '%') {
                i++;  // skip %
                try {
                    bytes[j++] = (byte)
                            Integer.parseInt(s.substring(i, i + 2), 16);

                } catch (Exception e) {
                    throw JacORBMessages.MESSAGES.invalidURIEncoding(s);
                }
                i++;  // skip first hex char; for loop will skip second one
            } else {
                bytes[j++] = (byte) s.charAt(i);
            }
        }

        return new String(bytes, 0, j, enc);
    }

    /**
     * Encode a string for inclusion in a URI (according to RFC 2396).
     * <p/>
     * Unsafe characters are escaped by encoding them in three-character
     * sequences '%xy', where 'xy' is the two-digit hexadecimal representation
     * of the lower 8-bits of the character.
     * <p/>
     * The question mark '?' character is also escaped, as required by RFC 2255.
     * <p/>
     * The string is first converted to the specified encoding.
     * For LDAP (2255), the encoding must be UTF-8.
     */
    public static String encode(String s, String enc)
            throws UnsupportedEncodingException {

        byte[] bytes = s.getBytes(enc);
        int count = bytes.length;

        /*
         * From RFC 2396:
         *
         *     mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
         * reserved = ";" | "/" | ":" | "?" | "@" | "&" | "=" | "+" | "$" | ","
         */
        final String allowed = "=,+;.'-@&/$_()!~*:"; // '?' is omitted
        char[] buf = new char[3 * count];
        int j = 0;

        for (int i = 0; i < count; i++) {
            if ((bytes[i] >= 0x61 && bytes[i] <= 0x7A) || // a..z
                    (bytes[i] >= 0x41 && bytes[i] <= 0x5A) || // A..Z
                    (bytes[i] >= 0x30 && bytes[i] <= 0x39) || // 0..9
                    (allowed.indexOf(bytes[i]) >= 0)) {
                buf[j++] = (char) bytes[i];
            } else {
                buf[j++] = '%';
                buf[j++] = Character.forDigit(0xF & (bytes[i] >>> 4), 16);
                buf[j++] = Character.forDigit(0xF & bytes[i], 16);
            }
        }
        return new String(buf, 0, j);
    }
}
