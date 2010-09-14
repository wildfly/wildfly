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

import java.io.IOException;

import org.jboss.as.model.ServerModel;

/**
 * A client proxy for communication between a ServerManager and a managed server.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Server {

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

    public void start(ServerModel serverConf) throws IOException {
        sendCommand(ServerManagerProtocolCommand.START_SERVER, serverConf);
    }

    public void stop() throws IOException {
        sendCommand(ServerManagerProtocolCommand.START_SERVER);
    }

    private void sendCommand(ServerManagerProtocolCommand command) throws IOException {
        sendCommand(command, null);
    }

    private void sendCommand(ServerManagerProtocolCommand command, Object o) throws IOException {

        byte[] cmd = ServerManagerProtocolUtils.createCommandBytes(command, o);
        communicationHandler.sendMessage(cmd);
    }

}
