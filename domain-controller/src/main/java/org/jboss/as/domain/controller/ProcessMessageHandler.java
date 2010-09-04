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

import org.jboss.as.server.manager.DomainControllerConfig;
import org.jboss.as.server.manager.ServerManagerProtocolCommand;
import org.jboss.as.server.manager.ServerManagerProtocolUtils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Message handler for messages comming from the process manager.
 *
 * @author John E. Bailey
 */
class ProcessMessageHandler implements ProcessCommunicationHandler.Handler {
    private static final Logger logger = Logger.getLogger("org.jboss.as.domain.controller");
    private final DomainController domainController;

    ProcessMessageHandler(DomainController domainController) {
        if (domainController == null) {
            throw new IllegalArgumentException("domainController is null");
        }
        this.domainController = domainController;
    }

    public void handleMessage(List<String> message) {
        logger.info("Message received: " + message);
    }

    @Override
    public void handleMessage(byte[] message) {
        ServerManagerProtocolCommand.Command cmd = null;
        try {
            cmd = ServerManagerProtocolCommand.readCommand(message);
        } catch (IOException e) {
            logger.error("Error reading command", e);
            return;
        }
        switch (cmd.getCommand()) {
            case START_SERVER:
                DomainControllerConfig config = null;
                try {
                    config = ServerManagerProtocolUtils.unmarshallCommandData(DomainControllerConfig.class, cmd);
                } catch (Exception e) {
                    logger.error("Error reading configuration", e);
                    return;
                }
                try {
                    domainController.start(config);
                } catch (Throwable t) {
                    logger.error("Error starting domain controller", t);
                    return;
                }
                break;
            case STOP_SERVER:
                domainController.stop();
                break;
            default:
                throw new IllegalArgumentException("Unknown command " + cmd.getCommand());
        }
    }

    @Override
    public void shutdown() {
        domainController.stop();
    }
}

