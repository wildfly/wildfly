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

package org.jboss.as.server.manager.management;

import java.io.DataInput;
import java.io.IOException;
import org.jboss.as.protocol.ChunkyByteInput;
import org.jboss.as.protocol.ChunkyByteOutput;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassTable;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Utility class providing methods for common management tasks.
 *
 * @author John Bailey
 */
public class ManagementUtils {
    private static final MarshallerFactory MARSHALLER_FACTORY;
    private static final MarshallingConfiguration CONFIG;

    static {
        try {
            MARSHALLER_FACTORY = Marshalling.getMarshallerFactory("river", Module.getModuleFromDefaultLoader(ModuleIdentifier.fromString("org.jboss.marshalling.river")).getClassLoader());
        } catch (ModuleLoadException e) {
            throw new RuntimeException(e);
        }
        final MarshallingConfiguration config = new MarshallingConfiguration();
        config.setClassTable(ModularClassTable.getInstance());
        CONFIG = config;
    }

    private ManagementUtils() {}

    public static <T> T unmarshal(final ByteInput input, final Class<T> expectedType) throws Exception {
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

    public static void marshal(final ByteOutput output, final Object object) throws Exception {
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

    public static void expectHeader(final DataInput input, int expected) throws IOException, ManagementException {
        byte header = input.readByte();
        if (header != (byte) expected) {
            throw new ManagementException("Invalid byte token.  Expecting '" + expected + "' received '" + header + "'");
        }
    }
}
