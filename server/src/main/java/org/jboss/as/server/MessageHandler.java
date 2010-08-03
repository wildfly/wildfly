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

import org.jboss.as.model.Standalone;
import org.jboss.as.process.ProcessManagerSlave.Handler;
import org.jboss.as.server.manager.ServerCommand;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

/**
 * A MessageHandler.
 * 
 * @author Brian Stansberry
 */
class MessageHandler implements Handler {

    private final Server server;
    
    MessageHandler(Server server) {
        if (server == null) {
            throw new IllegalArgumentException("server is null");
        }
        this.server = server;
    }

    @Override
    public void handleMessage(String sourceProcessName, byte[] message) {
        final ServerCommand serverCommand;
        try {
            serverCommand = readServerCommand(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read server command", e);
        }

        if(serverCommand == null)
            throw new RuntimeException("Server command is null");

        if("START".equals(serverCommand.getCommand())) {
            final Standalone standalone = (Standalone)serverCommand.getArgs()[0];
            try {
                server.start(standalone);
            } catch (ServerStartException e) {
                throw new RuntimeException("Failed to start server", e);
            }
        } else if("STOP".equals(serverCommand.getCommand())) {
            server.stop();
        }
    }

    private ServerCommand readServerCommand(byte[] message) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(message);
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            final ServerCommand serverCommand = (ServerCommand)objectInputStream.readObject();
            return serverCommand;
        } finally {
            if(objectInputStream != null)
                objectInputStream.close();
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.process.ProcessManagerSlave.Handler#handleMessage(java.lang.String, java.util.List)
     */
    @Override
    public void handleMessage(String sourceProcessName, List<String> message) {
        // FIXME implement handleMessage
        throw new UnsupportedOperationException("implement me");
    }

    @Override
    public void shutdown() {
        server.stop();        
    }

}
