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
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.model.ServerModel;
import org.jboss.as.process.RespawnPolicy;

/**
 * A client proxy for communication between a ServerManager and a managed server.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public final class Server {

    private final String serverProcessName;
    private final ServerModel serverConfig;
    private volatile ServerCommunicationHandler communicationHandler;

    private final RespawnPolicy respawnPolicy;
    private final AtomicInteger respawnCount = new AtomicInteger();
    private volatile ServerState state;

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

    public Server(ServerModel serverConfig, RespawnPolicy respawnPolicy) {
        if (serverConfig == null) {
            throw new IllegalArgumentException("serverConfig is null");
        }
        if (respawnPolicy == null) {
            throw new IllegalArgumentException("respawnPolicy is null");
        }
        this.serverProcessName = ServerManager.getServerProcessName(serverConfig);
        this.serverConfig = serverConfig;
        this.respawnPolicy = respawnPolicy;
        this.state = ServerState.BOOTING;
//        this.communicationHandler = communicationHandler;
    }

    public ServerState getState() {
        return state;
    }

    void setState(ServerState state) {
        this.state = state;
    }

    int incrementAndGetRespawnCount() {
        return respawnCount.incrementAndGet();
    }

    String getServerProcessName() {
        return serverProcessName;
    }

    RespawnPolicy getRespawnPolicy() {
        return respawnPolicy;
    }

    ServerModel getServerConfig() {
        return serverConfig;
    }

    void setCommunicationHandler(ServerCommunicationHandler communicationHandler) {
        this.communicationHandler = communicationHandler;
    }

    public void start() throws IOException {
        sendCommand(ServerManagerProtocolCommand.START_SERVER, serverConfig);
    }

    public void stop() throws IOException {
        sendCommand(ServerManagerProtocolCommand.STOP_SERVER);
        respawnCount.set(0);
    }

    private void sendCommand(ServerManagerProtocolCommand command) throws IOException {
        sendCommand(command, null);
    }

    private void sendCommand(ServerManagerProtocolCommand command, Object o) throws IOException {
        byte[] cmd = ServerManagerProtocolUtils.createCommandBytes(command, o);
        communicationHandler.sendMessage(cmd);
    }

}
