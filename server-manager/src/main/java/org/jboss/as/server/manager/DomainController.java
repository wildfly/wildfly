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

import org.jboss.as.model.Standalone;

import java.io.IOException;

/**
 * A client proxy for communication between a ServerManager and the DomainController.
 * 
 * @author John bailey
 */
public final class DomainController {
    static final String DOMAIN_CONTROLLER_PROCESS_NAME = "domain-controller";

    private final ServerCommunicationHandler communicationHandler;

    public DomainController(ServerCommunicationHandler communicationHandler) {
        if (communicationHandler == null) {
            throw new IllegalArgumentException("communicationHandler is null");
        }
        this.communicationHandler = communicationHandler;
    }

    public void start() throws IOException {
        sendCommand(ServerManagerProtocolCommand.START_SERVER, DOMAIN_CONTROLLER_PROCESS_NAME);
    }

    public void stop() throws IOException {
        sendCommand(ServerManagerProtocolCommand.STOP_SERVER, DOMAIN_CONTROLLER_PROCESS_NAME);
    }

    private void sendCommand(ServerManagerProtocolCommand command) throws IOException {
        sendCommand(command, null);
    }    
    
    private void sendCommand(ServerManagerProtocolCommand command, Object o) throws IOException {
        
        byte[] cmd = ServerManagerProtocolUtils.createCommandBytes(command, o);
        communicationHandler.sendMessage(cmd);
    }

}
