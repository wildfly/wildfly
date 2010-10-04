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

/**
 *
 */
package org.jboss.as.server.manager;

import java.io.IOException;

import org.jboss.as.process.ProcessManagerSlave.Handler;
import org.jboss.as.server.manager.ServerManagerProtocol.Command;
import org.jboss.as.server.manager.ServerManagerProtocol.ServerToServerManagerCommandHandler;
import org.jboss.as.server.manager.ServerManagerProtocol.ServerToServerManagerProtocolCommand;
import org.jboss.logging.Logger;

/**
 * A MessageHandler.
 *
 * @author Brian Stansberry
 */
public class MessageHandler implements Handler {

    private static final Logger log = Logger.getLogger("org.jboss.server.manager");

    private final ServerManager serverManager;
    private final CommandHandler commandHandler = new CommandHandler();

    public MessageHandler(ServerManager serverManager) {
        if (serverManager == null) {
            throw new IllegalArgumentException("serverManager is null");
        }
        this.serverManager = serverManager;
    }

    @Override
    public void handleMessage(String sourceProcessName, byte[] message) {
        commandHandler.handleCommand(sourceProcessName, message);
    }



    @Override
    public void shutdown() {
        serverManager.stop();
    }

    public void shutdownServers() {
        serverManager.shutdownServers();
    }

    @Override
    public void down(String downProcessName) {
        serverManager.downServer(downProcessName);
    }

//    public void registerServer(String serverName, Server server) {
//        if (serverName == null) {
//            throw new IllegalArgumentException("serverName is null");
//        }
//        if (server == null) {
//            throw new IllegalArgumentException("server is null");
//        }
//        servers.put(serverName, server);
//    }
//
//    public void unregisterServer(String serverName) {
//        if (serverName == null) {
//            throw new IllegalArgumentException("serverName is null");
//        }
//        servers.remove(serverName);
//    }

    /**
     * Callback for the {@link ServerToServerManagerProtocolCommand#handleCommand(String, ServerToServerManagerCommandHandler, Command)} calls
     */
    private class CommandHandler implements ServerToServerManagerCommandHandler{

        void handleCommand(String sourceProcessName, byte[] message) {
            Command<ServerToServerManagerProtocolCommand> cmd = null;
            try {
                cmd = ServerToServerManagerProtocolCommand.readCommand(message);
            } catch (IOException e) {
                log.error("Error reading command", e);
                return;
            }

            try {
                cmd.getCommand().handleCommand(sourceProcessName, this, cmd);
            } catch (IOException e) {
                log.error("Error unmarshalling command data", e);
            } catch (ClassNotFoundException e) {
                log.error("Error unmarshalling command data", e);
            }
        }

        @Override
        public void handleServerAvailable(String sourceProcessName) {
            serverManager.availableServer(sourceProcessName);
        }

        @Override
        public void handleServerReconnectStatus(String sourceProcessName, ServerState state) {
            serverManager.reconnectedServer(sourceProcessName, state);
        }

        @Override
        public void handleServerStartFailed(String sourceProcessName) {
            serverManager.failedStartServer(sourceProcessName);
        }

        @Override
        public void handleServerStarted(String sourceProcessName) {
            serverManager.startedServer(sourceProcessName);
        }

        @Override
        public void handleServerStopped(String sourceProcessName) {
            serverManager.stoppedServer(sourceProcessName);
        }

    }

}
