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
import org.jboss.as.server.manager.ServerMessage;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.TimingServiceListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;


/**
 * An actual JBoss Application Server instance.
 *
 * @author Brian Stansberry
 * @author John E. Bailey
 */
public class Server {
    private static final Logger logger = Logger.getLogger("org.jboss.as.server");
    private final ServerEnvironment environment;
    private ServerCommunicationHandler serverCommunicationHandler;
    private final MessageHandler messageHandler = new MessageHandler(this);
    private Standalone config;
    private ServiceContainer serviceContainer;

    public Server(ServerEnvironment environment) {
        if (environment == null) {
            throw new IllegalArgumentException("bootstrapConfig is null");
        }
        this.environment = environment;
        launchCommunicationHandler();
        sendMessage("AVAILABLE");
        logger.info("Server Available to start");
    }

    public void start(Standalone config) throws ServerStartException {
        this.config = config;
        logger.info("Starting server with config: " + config.getServerName());
        serviceContainer = ServiceContainer.Factory.create();
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        final TimingServiceListener listener = new TimingServiceListener(new Runnable() {
            @Override
            public void run() {
                logger.info("Server started");
                sendMessage("STARTED");
            }
        });
        batchBuilder.addListener(listener);

        try {
            config.activate(serviceContainer, batchBuilder);
            batchBuilder.install();
            listener.finishBatch();
        } catch (Throwable t) {
            sendMessage("START FAILED");
            throw new ServerStartException("Failed to start server", t);
        }
    }

    public void stop() {
        serviceContainer.shutdown();
        sendMessage("STOPPED");
    }

    private void launchCommunicationHandler() {
        this.serverCommunicationHandler = ServerCommunicationHandlerFactory.getInstance().getProcessManagerSlave(environment, messageHandler);
        Thread t = new Thread(this.serverCommunicationHandler.getController(), "Server Process");
        t.start();
    }

    private void sendMessage(final String message) {
        try {
            final ServerMessage serverMessage = new ServerMessage(message);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            Checksum chksum = new Adler32();
            CheckedOutputStream cos = new CheckedOutputStream(baos, chksum);
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(cos);
                oos.writeObject(serverMessage);
                oos.close();
                oos = null;
                serverCommunicationHandler.sendMessage(baos.toByteArray(), chksum.getValue());
            }
            finally {
                if (oos != null) {
                    try {
                        oos.close();
                    }
                    catch (IOException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to send message to Server Manager [" + message + "]", e);
        }
    }
}
