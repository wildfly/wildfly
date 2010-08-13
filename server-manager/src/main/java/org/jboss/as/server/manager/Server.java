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

import java.io.Closeable;
import org.jboss.as.model.Standalone;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassTable;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;

/**
 * A client proxy for communication between a ServerManager and a managed server.
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Server {
//    private static final ThreadFactory FACTORY = Executors.defaultThreadFactory();
    
    private final ServerCommunicationHandler communicationHandler;
    
//    public Server(final InputStream errorStream, final InputStream inputStream, final OutputStream outputStream) {
//        this.processManagerSlave = null;
//        final Thread thread = FACTORY.newThread(new Runnable() {
//            public void run() {
//                try {
//                    final InputStreamReader reader = new InputStreamReader(errorStream);
//                    final BufferedReader bufferedReader = new BufferedReader(reader);
//                    String line;
//                    try {
//                        while ((line = bufferedReader.readLine()) != null) {
//                            System.err.println("Server reported error: " + line.trim());
//                        }
//                    } catch (IOException e) {
//                        // todo log it
//                    }
//                } finally {
//                    try {
//                        errorStream.close();
//                    } catch (IOException e) {
//                        // todo log
//                    }
//                }
//            }
//        });
//        thread.start();
//        FACTORY.newThread(new Runnable() {
//            public void run() {
//                String cmd;
//                try {
//                    while ((cmd = readCommand(inputStream)) != null) {
//                        System.out.println("Got msg: " + cmd);
//                    }
//                } catch (IOException e) {
//                    // todo log it
//                } finally {
//                    try {
//                        inputStream.close();
//                    } catch (IOException e) {
//                        // todo log
//                    }
//                }
//            }
//
//        });
//    }
    
    public Server(ServerCommunicationHandler communicationHandler) {
        if (communicationHandler == null) {
            throw new IllegalArgumentException("communicationHandler is null");
        }
        this.communicationHandler = communicationHandler;
    }

    public void start(Standalone serverConf) throws IOException {
        
        ServerCommand command = new ServerCommand(ServerCommand.Command.START, new Object[] {serverConf}, new Class<?>[]{Standalone.class});
        sendCommand(command);
    }

    public void stop() throws IOException {
        
        sendCommand(new ServerCommand(ServerCommand.Command.START)); 
    }

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

    private void sendCommand(ServerCommand command) throws IOException {
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        final Checksum chksum = new Adler32();
        final CheckedOutputStream cos = new CheckedOutputStream(baos, chksum);
        final Marshaller marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
        try {
            marshaller.start(Marshalling.createByteOutput(cos));
            marshaller.writeObject(command);
            marshaller.finish();
            marshaller.close();
            communicationHandler.sendMessage(baos.toByteArray(), chksum.getValue());
        } finally {
            safeClose(marshaller);
        }
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable ignored) {
            // todo: log me
        }
    }

//    private static String readCommand(final InputStream in) throws IOException {
//        final StringBuilder b = new StringBuilder();
//        int c;
//        while ((c = in.read()) != -1 && c != '\n') {
//            b.append((char) (c & 0xff));
//        }
//        if (b.length() == 0 && c == -1) {
//            return null;
//        }
//        return b.toString();
//    }
}
