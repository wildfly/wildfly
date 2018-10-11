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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Base64;

import org.wildfly.clustering.marshalling.spi.Serializer;

/**
 * Strategies for externalizing a session identifier.
 * @author Paul Ferraro
 */
public enum IdentifierSerializer implements Serializer<String> {
    UTF8() {
        @Override
        public void write(DataOutput output, String id) throws IOException {
            output.writeUTF(id);
        }

        @Override
        public String read(DataInput input) throws IOException {
            return input.readUTF();
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
        public void write(DataOutput output, String id) throws IOException {
            byte[] bytes = Base64.getUrlDecoder().decode(id);
            output.writeByte(bytes.length);
            output.write(bytes);
        }

        @Override
        public String read(DataInput input) throws IOException {
            byte[] decoded = new byte[input.readUnsignedByte()];
            input.readFully(decoded);
            return Base64.getUrlEncoder().encodeToString(decoded);
        }
    },
    /**
     * Specific optimization for hex-encoded identifiers (e.g. Tomcat).
     */
    HEX() {
        @Override
        public void write(DataOutput output, String id) throws IOException {
            if (id.length() % 2 != 0) {
                throw new IllegalArgumentException(id);
            }
            byte[] bytes = new byte[id.length() / 2];
            for (int i = 0; i < bytes.length; ++i) {
                int index = i * 2;
                int high = Character.digit(id.charAt(index), 16) << 4;
                int low = Character.digit(id.charAt(index + 1), 16);
                bytes[i] = (byte) (high + low);
            }
            output.writeByte(bytes.length);
            output.write(bytes);
        }

        @Override
        public String read(DataInput input) throws IOException {
            byte[] decoded = new byte[input.readUnsignedByte()];
            input.readFully(decoded);
            StringBuilder builder = new StringBuilder(decoded.length * 2);
            for (byte b : decoded) {
                builder.append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xf, 16)));
                builder.append(Character.toUpperCase(Character.forDigit(b & 0xf, 16)));
            }
            return builder.toString();
        }
    },
    ;

    /**
     * Indicates whether or not the specified identifier is valid for this serializer.
     * @param id an identifier
     * @return true, if the specified identifier is valid, false otherwise.
     */
    public boolean validate(String id) {
        try {
            this.write(NULL_SINK, id);
            return true;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    private static final DataOutput NULL_SINK = new DataOutput() {

        @Override
        public void write(int b) throws IOException {
        }

        @Override
        public void write(byte[] b) throws IOException {
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
        }

        @Override
        public void writeBoolean(boolean v) throws IOException {
        }

        @Override
        public void writeByte(int v) throws IOException {
        }

        @Override
        public void writeShort(int v) throws IOException {
        }

        @Override
        public void writeChar(int v) throws IOException {
        }

        @Override
        public void writeInt(int v) throws IOException {
        }

        @Override
        public void writeLong(long v) throws IOException {
        }

        @Override
        public void writeFloat(float v) throws IOException {
        }

        @Override
        public void writeDouble(double v) throws IOException {
        }

        @Override
        public void writeBytes(String s) throws IOException {
        }

        @Override
        public void writeChars(String s) throws IOException {
        }

        @Override
        public void writeUTF(String s) throws IOException {
        }
    };
}
