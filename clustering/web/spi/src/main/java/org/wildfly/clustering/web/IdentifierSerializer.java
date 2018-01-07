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

import javax.xml.bind.DatatypeConverter;

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
            byte[] bytes = DatatypeConverter.parseHexBinary(id);
            output.writeByte(bytes.length);
            output.write(bytes);
        }

        @Override
        public String read(DataInput input) throws IOException {
            byte[] decoded = new byte[input.readUnsignedByte()];
            input.readFully(decoded);
            return DatatypeConverter.printHexBinary(decoded);
        }
    },
    ;
}
