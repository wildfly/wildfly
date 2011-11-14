/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.io.InputStream;

import org.jboss.com.sun.net.httpserver.Headers;

/**
 * Parses MIME headers
 *
 * @author Jason T. Greene
 */
public final class MimeHeaderParser {

    private static StringBuilder trim(StringBuilder builder) {
        int i = builder.length();
        while (--i >= 0 && builder.charAt(i) <= ' ') {}

        builder.setLength(i + 1);
        return builder;
    }

    private static StringBuilder ltrim(StringBuilder builder) {
        int i = 0;
        while (i < builder.length() && builder.charAt(i++) <= ' ') {}

        builder.delete(0, i - 1);
        return builder;
    }

    public static final class ParseResult {
        private Headers headers;
        private boolean eof;

        private ParseResult(Headers headers, boolean eof) {
            this.headers = headers;
            this.eof = eof;
        }

        public Headers headers() {
            return headers;
        }

        public boolean eof() {
            return eof;
        }
    }

    public static ParseResult parseHeaders(InputStream stream) throws IOException {
        Headers headers  = new Headers();

        StringBuilder builder = new StringBuilder();
        String key = null, value = null;
        int c;

        loop:
        for (int lastc = -1, lastc2 = -1, lastc3 = -1; (c = stream.read()) != -1;) {
            boolean skip = false;
            switch (c) {
                case ':':
                    if (key == null) {
                        key = trim(builder).toString();
                        builder.setLength(0);
                        skip = true;
                    }
                    break;
                case '\t':
                    // Convert tabs to spaces
                    c = ' ';
                    break;
                case ' ':
                    // continuation
                    if (lastc == '\n' && lastc2 == '\r') {
                        builder.setLength(builder.length() - 2);
                    }

                    break;
                case '\n':
                    if (lastc3 == '\r' && lastc2 == '\n' && lastc == '\r') {
                        key = value = null;
                        break loop;
                    }
                    break;
                default:
                    if (lastc == '\n' && lastc2 == '\r') {
                        // Ignore garbage lines without a key
                        if (key != null) {
                            builder.setLength(builder.length() - 2);
                            value =  ltrim(builder).toString();
                        }

                        builder.setLength(0);
                    }
                    break;
            }

            if (!skip)
                builder.append((char)c);

            if (value != null) {
                headers.add(key, value);
                key = null;
                value = null;
            }

            lastc3 = lastc2;
            lastc2 = lastc;
            lastc = c;
        }

        if (key != null) {
            value = ltrim(builder).toString();
            headers.add(key, value);
        }

        return new ParseResult(headers, c == -1);
    }
}
