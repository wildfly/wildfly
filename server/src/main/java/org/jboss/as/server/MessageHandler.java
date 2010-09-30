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
package org.jboss.as.server;

import java.io.IOException;

import org.jboss.as.model.ServerModel;
import org.jboss.as.server.manager.ServerManagerProtocol.Command;
import org.jboss.as.server.manager.ServerManagerProtocol.ServerManagerToServerCommandHandler;
import org.jboss.as.server.manager.ServerManagerProtocol.ServerManagerToServerProtocolCommand;
import org.jboss.logging.Logger;

/**
 * A MessageHandler.
 *
 * @author Brian Stansberry
 * @author John E. Bailey
 * @author Kabir Khan
 */
class MessageHandler implements ServerCommunicationHandler.Handler {
    private static final Logger logger = Logger.getLogger("org.jboss.as.server");
    private final Server server;
    CommandHandler commandHandler = new CommandHandler();

    MessageHandler(Server server) {
        if (server == null) {
            throw new IllegalArgumentException("server is null");
        }
        this.server = server;
    }

    @Override
    public void handleMessage(byte[] message) {
        commandHandler.handleCommand(message);
    }

    @Override
    public void shutdown() {
        server.stop();
    }

    @Override
    public void reconnectServer(String addr, String port) {
        server.reconnectToServerManager(addr, port);
    }

    /**
     * Callback for the {@link ServerManagerToServerProtocolCommand#handleCommand(ServerManagerToServerCommandHandler, Command)} calls
     */
    private class CommandHandler implements ServerManagerToServerCommandHandler {

        void handleCommand(byte[] message) {
            Command<ServerManagerToServerProtocolCommand> cmd = null;
            try {
                cmd = ServerManagerToServerProtocolCommand.readCommand(message);
            } catch (IOException e) {
                logger.error("Error reading command", e);
                return;
            }

            try {
                cmd.getCommand().handleCommand(this, cmd);
            } catch (IOException e) {
                logger.error("Error unmarshalling command data", e);
            } catch (ClassNotFoundException e) {
                logger.error("Error unmarshalling command data", e);
            }
        }

        @Override
        public void handleStartServer(ServerModel serverModel) {
            try {
                server.start(serverModel);
            } catch (ServerStartException e) {
                logger.error("Error starting server", e);
                return;
            }

        }

        @Override
        public void handleStopServer() {
            server.stop();
        }
    }
}
