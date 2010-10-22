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

package org.jboss.as.domain.client.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.jboss.as.protocol.ChunkyByteInput;
import org.jboss.as.protocol.ChunkyByteOutput;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * @author John Bailey
 */
class ProtocolUtils {
    private static final MarshallerFactory MARSHALLER_FACTORY;
    private static final MarshallingConfiguration CONFIG;

    static {
        MarshallerFactory marshallerFactory;
        CONFIG = new MarshallingConfiguration();
        try {
            marshallerFactory = Marshalling.getMarshallerFactory("river", Module.getModuleFromDefaultLoader(ModuleIdentifier.fromString("org.jboss.marshalling.river")).getClassLoader());
            CONFIG.setClassResolver(ModularClassResolver.getInstance());
        } catch (ModuleLoadException e) {
            final ClassLoader classLoader = ProtocolUtils.class.getClassLoader();
            marshallerFactory = Marshalling.getMarshallerFactory("river", classLoader);
            CONFIG.setClassResolver(new SimpleClassResolver(classLoader));
        }
        if (marshallerFactory == null) {
            throw new RuntimeException("Failed to construct a Marshaller factory");
        }
        MARSHALLER_FACTORY = marshallerFactory;
    }

    private ProtocolUtils() {
    }

    static void writeRequestHeader(final DataOutput output) throws Exception {
        output.write(Protocol.SIGNATURE);
        output.writeByte(Protocol.VERSION_FIELD);
        output.writeInt(Protocol.VERSION);
        output.writeInt(0);
        output.writeByte(Protocol.DOMAIN_CONTROLLER_REQUEST);
    }

    static int readResponseHeader(final DataInput input) throws IOException {
        validateSignature(input);
        expectHeader(input, Protocol.VERSION_FIELD);
        final int version = input.readInt();
        final int responseId = input.readInt();
        assert responseId == 0;
        return version;
    }

    static void validateSignature(final DataInput input) throws IOException {
        final byte[] signatureBytes = new byte[4];
        input.readFully(signatureBytes);
        if (!Arrays.equals(Protocol.SIGNATURE, signatureBytes)) {
            throw new IOException("Invalid signature [" + Arrays.toString(signatureBytes) + "]");
        }
    }

    static void expectHeader(final InputStream input, int expected) throws IOException {
        expectHeader(StreamUtils.readByte(input), expected);
    }

    static void expectHeader(final DataInput input, int expected) throws IOException {
        expectHeader(input.readByte(), expected);
    }

    private static void expectHeader(final byte actual, int expected) throws IOException {
        if (actual != (byte) expected) {
            throw new IOException("Invalid byte token.  Expecting '" + expected + "' received '" + actual + "'");
        }
    }

    static <T> T unmarshal(final ByteInput input, final Class<T> expectedType) throws Exception {
        final Unmarshaller unmarshaller = MARSHALLER_FACTORY.createUnmarshaller(CONFIG);
        final ChunkyByteInput chunked = new ChunkyByteInput(input);
        try {
            unmarshaller.start(chunked);
            final T result = unmarshaller.readObject(expectedType);
            unmarshaller.finish();
            return result;
        } finally {
            chunked.close();
        }
    }

    static void marshal(final ByteOutput output, final Object object) throws Exception {
        final Marshaller marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
        final ChunkyByteOutput chunked = new ChunkyByteOutput(output);
        try {
            marshaller.start(chunked);
            marshaller.writeObject(object);
            marshaller.finish();
        } finally {
            chunked.close();
        }
    }

    public static Marshaller getMarshaller() throws Exception {
        return MARSHALLER_FACTORY.createMarshaller(CONFIG);
    }

    public static Unmarshaller getUnmarshaller() throws Exception {
        return MARSHALLER_FACTORY.createUnmarshaller(CONFIG);
    }
}
