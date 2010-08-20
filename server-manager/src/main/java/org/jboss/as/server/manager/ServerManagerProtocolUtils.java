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
package org.jboss.as.server.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;

import org.jboss.as.server.manager.ServerManagerProtocolCommand.Command;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassTable;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ServerManagerProtocolUtils {
    private static final MarshallerFactory MARSHALLER_FACTORY;
    private static final MarshallingConfiguration CONFIG;
    static {
        try {
            MARSHALLER_FACTORY = Marshalling.getMarshallerFactory("river", ModuleClassLoader.forModuleName("org.jboss.marshalling:jboss-marshalling-river"));
        } catch (ModuleLoadException e) {
            throw new RuntimeException(e);
        }
        final MarshallingConfiguration config = new MarshallingConfiguration();
        config.setClassTable(ModularClassTable.getInstance());
        CONFIG = config;
    }

    public static byte[] createCommandBytes(ServerManagerProtocolCommand protocol, Object data) throws IOException {
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
        
        return protocol.createCommandBytes(objectBytes);
    }
    
    public static <T> T unmarshallCommandData(Class<T> clazz, Command command) throws IOException, ClassNotFoundException{
        if (clazz == null)
            throw new IllegalArgumentException("Null clazz");
        if (command == null)
            throw new IllegalArgumentException("Null command");
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
