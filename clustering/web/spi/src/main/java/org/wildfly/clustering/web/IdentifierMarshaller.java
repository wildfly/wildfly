/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.wildfly.clustering.marshalling.spi.Marshaller;

/**
 * Strategies for externalizing a session identifier.
 * @author Paul Ferraro
 */
public enum IdentifierMarshaller implements Marshaller<String, ByteBuffer> {
    ISO_LATIN_1() {
        @Override
        public String read(ByteBuffer buffer) throws IOException {
            int offset = buffer.arrayOffset();
            int length = buffer.limit() - offset;
            return new String(buffer.array(), offset, length, StandardCharsets.ISO_8859_1);
        }

        @Override
        public ByteBuffer write(String value) throws IOException {
            return ByteBuffer.wrap(value.getBytes(StandardCharsets.ISO_8859_1));
        }

        @Override
        public boolean validate(String id) {
            return true;
        }
    },
    /**
     * Specific optimization for Base64-encoded identifiers (e.g. Undertow).
     */
    BASE64() {
        @Override
        public String read(ByteBuffer buffer) throws IOException {
            return ISO_LATIN_1.read(Base64.getUrlEncoder().encode(buffer));
        }

        @Override
        public ByteBuffer write(String value) throws IOException {
            return Base64.getUrlDecoder().decode(ISO_LATIN_1.write(value));
        }
    },
    /**
     * Specific optimization for hex-encoded identifiers (e.g. Tomcat).
     */
    HEX() {
        @Override
        public String read(ByteBuffer buffer) throws IOException {
            int offset = buffer.arrayOffset();
            int length = buffer.limit() - offset;
            StringBuilder builder = new StringBuilder(length * 2);
            while (buffer.hasRemaining()) {
                byte b = buffer.get();
                builder.append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xf, 16)));
                builder.append(Character.toUpperCase(Character.forDigit(b & 0xf, 16)));
            }
            return builder.toString();
        }

        @Override
        public ByteBuffer write(String value) throws IOException {
            if (value.length() % 2 != 0) {
                throw new IllegalArgumentException(value);
            }
            byte[] bytes = new byte[value.length() / 2];
            for (int i = 0; i < bytes.length; ++i) {
                int index = i * 2;
                int high = Character.digit(value.charAt(index), 16) << 4;
                int low = Character.digit(value.charAt(index + 1), 16);
                bytes[i] = (byte) (high + low);
            }
            return ByteBuffer.wrap(bytes);
        }
    },
    ;

    @Override
    public boolean isMarshallable(Object object) {
        return object instanceof String;
    }

    /**
     * Indicates whether or not the specified identifier is valid for this serializer.
     * @param id an identifier
     * @return true, if the specified identifier is valid, false otherwise.
     */
    public boolean validate(String id) {
        try {
            this.write(id);
            return true;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }
}
