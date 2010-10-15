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
package org.jboss.as.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;

import org.jboss.as.server.ServerManagerProtocol.Command;
import org.jboss.as.server.ServerManagerProtocol.ServerManagerToServerProtocolCommand;
import org.jboss.as.server.ServerManagerProtocol.ServerToServerManagerProtocolCommand;
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
 * This is in a separate class since the marshalling requires module classloading set up
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ServerManagerProtocolUtils {
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

    public static byte[] createCommandBytes(ServerManagerToServerProtocolCommand protocol, Object data) throws IOException {
        return protocol.createCommandBytes(createDataBytes(data));
    }

    public static byte[] createCommandBytes(ServerToServerManagerProtocolCommand protocol, Object data) throws IOException {
        return protocol.createCommandBytes(createDataBytes(data));
    }

    private static byte[] createDataBytes(Object data) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        byte[] objectBytes = null;
        if (data != null) {
            final Marshaller marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
            try {
                marshaller.start(Marshalling.createByteOutput(baos));
                marshaller.writeObject(data);
                marshaller.finish();
                marshaller.close();
                objectBytes = baos.toByteArray();
            } finally {
                safeClose(marshaller);
            }
        }
        return objectBytes;
    }

    public static <T> T unmarshallCommandData(Class<T> clazz, Command<?> command) throws IOException, ClassNotFoundException{
        if (command == null)
            throw new IllegalArgumentException("Null command");

        if (clazz == null)
            throw new IllegalArgumentException("Null clazz");
        if (command.getData() == null || command.getData().length == 0)
            throw new IllegalArgumentException("No data in command ");

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(command.getData());
        final Unmarshaller unmarshaller = MARSHALLER_FACTORY.createUnmarshaller(CONFIG);
        try {
            unmarshaller.start(Marshalling.createByteInput(byteArrayInputStream));
            final T obj = unmarshaller.readObject(clazz);
            unmarshaller.finish();
            unmarshaller.close();
            return obj;
        } finally {
            safeClose(unmarshaller);
        }
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable ignored) {
            // todo: log me
        }
    }
}
