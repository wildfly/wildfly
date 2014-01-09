/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.remoting;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedExceptionAction;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.HttpUpgradeHandshake;
import io.undertow.util.HttpString;

/**
 * Utility class to create a server-side HTTP Upgrade handshake.
 *
 * @author Stuart Douglas
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class SimpleHttpUpgradeHandshake implements HttpUpgradeHandshake {

    private final String keyHeader;
    private final String magicNumber;
    private final String acceptHeader;

    public SimpleHttpUpgradeHandshake(String magicNumber, String keyHeader, String acceptHeader) {
        this.keyHeader = keyHeader;
        this.magicNumber = magicNumber;
        this.acceptHeader = acceptHeader;
    }

    public boolean handleUpgrade(final HttpServerExchange exchange) throws IOException {
        String secretKey = exchange.getRequestHeaders().getFirst(keyHeader);
        if (secretKey == null) {
            throw RemotingMessages.MESSAGES.upgradeRequestMissingKey();
        }
        String response = createExpectedResponse(magicNumber, secretKey);
        exchange.getResponseHeaders().put(HttpString.tryFromString(acceptHeader), response);
        return true;
    }

    private String createExpectedResponse(final String magicNumber, final String secretKey) throws IOException {
        try {
            final String concat = secretKey + magicNumber;
            final MessageDigest digest = MessageDigest.getInstance("SHA1");

            digest.update(concat.getBytes("UTF-8"));
            final byte[] bytes = digest.digest();
            return FlexBase64.encodeString(bytes, false);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private static class FlexBase64 {
        /*
         * Note that this code heavily favors performance over reuse and clean style.
         */

        private static final byte[] ENCODING_TABLE;
        private static final byte[] DECODING_TABLE = new byte[80];
        private static final Constructor<String> STRING_CONSTRUCTOR;

        static {
            try {
                ENCODING_TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes("ASCII");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException();
            }

            for (int i = 0; i < ENCODING_TABLE.length; i++) {
                int v = (ENCODING_TABLE[i] & 0xFF) - 43;
                DECODING_TABLE[v] = (byte) (i + 1);  // zero = illegal
            }

            Constructor<String> c = null;
            try {
                PrivilegedExceptionAction<Constructor<String>> runnable = new PrivilegedExceptionAction<Constructor<String>>() {
                    @Override
                    public Constructor<String> run() throws Exception {
                        Constructor<String> c;
                        c = String.class.getDeclaredConstructor(char[].class, boolean.class);
                        c.setAccessible(true);
                        return c;
                    }
                };
                if (System.getSecurityManager() != null) {
                    c = AccessController.doPrivileged(runnable);
                } else {
                    c = runnable.run();
                }
            } catch (Throwable t) {
            }

            STRING_CONSTRUCTOR = c;
        }

        /**
         * Encodes a fixed and complete byte array into a Base64 String.
         *
         * @param source the byte array to encode from
         * @param wrap   whether or not to wrap the output at 76 chars with CRLFs
         * @return a new String representing the Base64 output
         */
        public static String encodeString(byte[] source, boolean wrap) {
            return encodeString(source, 0, source.length, wrap);
        }


        private static String encodeString(byte[] source, int pos, int limit, boolean wrap) {
            int olimit = (limit - pos);
            int remainder = olimit % 3;
            olimit = (olimit + (remainder == 0 ? 0 : 3 - remainder)) / 3 * 4;
            olimit += (wrap ? (olimit / 76) * 2 + 2 : 0);
            char[] target = new char[olimit];
            int opos = 0;
            int last = 0;
            int count = 0;
            int state = 0;
            final byte[] ENCODING_TABLE = FlexBase64.ENCODING_TABLE;

            while (limit > pos) {
                //  ( 6 | 2) (4 | 4) (2 | 6)
                int b = source[pos++] & 0xFF;
                target[opos++] = (char) ENCODING_TABLE[b >>> 2];
                last = (b & 0x3) << 4;
                if (pos >= limit) {
                    state = 1;
                    break;
                }
                b = source[pos++] & 0xFF;
                target[opos++] = (char) ENCODING_TABLE[last | (b >>> 4)];
                last = (b & 0x0F) << 2;
                if (pos >= limit) {
                    state = 2;
                    break;
                }
                b = source[pos++] & 0xFF;
                target[opos++] = (char) ENCODING_TABLE[last | (b >>> 6)];
                target[opos++] = (char) ENCODING_TABLE[b & 0x3F];

                if (wrap) {
                    count += 4;
                    if (count >= 76) {
                        count = 0;
                        target[opos++] = 0x0D;
                        target[opos++] = 0x0A;
                    }
                }
            }

            complete(target, opos, state, last, wrap);

            try {
                // Eliminate copying on Open/Oracle JDK
                if (STRING_CONSTRUCTOR != null) {
                    return STRING_CONSTRUCTOR.newInstance(target, Boolean.TRUE);
                }
            } catch (Exception e) {
            }

            return new String(target);
        }

        private static int complete(char[] target, int pos, int state, int last, boolean wrap) {
            if (state > 0) {
                target[pos++] = (char) ENCODING_TABLE[last];
                for (int i = state; i < 3; i++) {
                    target[pos++] = '=';
                }
            }
            if (wrap) {
                target[pos++] = 0x0D;
                target[pos++] = 0x0A;
            }

            return pos;
        }
    }
}
