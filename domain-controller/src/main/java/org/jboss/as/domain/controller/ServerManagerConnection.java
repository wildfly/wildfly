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

package org.jboss.as.domain.controller;

import org.jboss.as.model.Host;
import org.jboss.logging.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Responsible for managing the communication with a single server manager instance.
 * 
 * @author John E. Bailey
 */
public class ServerManagerConnection implements Runnable {
    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");

    private final String id;
    private final Socket socket;
    private final InputStream socketIn;
    private final OutputStream socketOut;
    private final ServerManagerCommunicationService communicationService;
    private final DomainController domainController;
    private Host hostConfig;


    /**
     * Create a new instance.
     *
     * @param id The server manager identifier
     * @param domainController The domain controller
     * @param communicationService The communication service
     * @param socket The server managers socket
     */
    public ServerManagerConnection(final String id, final DomainController domainController, final ServerManagerCommunicationService communicationService, final Socket socket) {
        this.id = id;
        this.domainController = domainController;
        this.communicationService = communicationService;
        this.socket = socket;
        try {
            this.socketIn = new BufferedInputStream(socket.getInputStream());
            this.socketOut = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            closeSocket();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            for (;;) {
                if(!socket.isConnected())
                    break;
                ServerManagerConnectionProtocol.IncomingCommand.processNext(this, socketIn);
            }
        } catch (Throwable t) {
            log.error(t);
            throw new RuntimeException(t);
        } finally {
            closeSocket();
        }
    }

    public String getId() {
        return id;
    }

    public Host getHostConfig() {
        return hostConfig;
    }

    public void setHostConfig(Host hostConfig) {
        this.hostConfig = hostConfig;
    }

    public void updateDomain() {
        try {
            ServerManagerConnectionProtocol.OutgoingCommand.UPDATE_DOMAIN.execute(this, socketOut);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update server manager with new domain", e); // TODO: Better exception
        }
    }

    void confirmRegistration() throws Exception {
        try {
            ServerManagerConnectionProtocol.OutgoingCommand.CONFIRM_REGISTRATION.execute(this, socketOut);
        } catch (Exception e) {
            throw new RuntimeException(e); // TODO: Better exception
        }
    }

    void unregistered() {
        communicationService.removeServerManagerConnection(this);
    }

    DomainController getDomainController() {
        return domainController;
    }

     private void closeSocket() {
        if(socket == null) return;
        try {
            socket.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.shutdownInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}