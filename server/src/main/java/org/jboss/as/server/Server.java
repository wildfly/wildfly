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
import java.util.Map;

import org.jboss.as.model.Standalone;
import org.jboss.as.process.StreamUtils;
import org.jboss.as.server.manager.ServerManagerProtocolCommand;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;


/**
 * An actual JBoss Application Server instance.
 *
 * @author Brian Stansberry
 * @author John E. Bailey
 */
public class Server extends AbstractServer {

	private ServerCommunicationHandler serverCommunicationHandler;
    private final MessageHandler messageHandler = new MessageHandler(this);

    public Server(ServerEnvironment environment) {
    	super(environment);
    }

    public void start() {
        launchCommunicationHandler();
        sendMessage(ServerManagerProtocolCommand.SERVER_AVAILABLE);
        log.info("Server Available to start");    	
    }
    
    public void start(Standalone config) throws ServerStartException {
    	try {
    		super.start(config);
    	} catch(ServerStartException e) {
    		sendMessage(ServerManagerProtocolCommand.SERVER_START_FAILED);
    		throw e;
    	}
    }

    public void stop() {
        super.stop();
        sendMessage(ServerManagerProtocolCommand.SERVER_STOPPED);
    }

    ServerStartupListener.Callback createListenerCallback() {
    	return new ServerStartupListener.Callback() {
            public void run(Map<ServiceName, StartException> serviceFailures, long elapsedTime, int totalServices, int onDemandServices, int startedServices) {
                if(serviceFailures.isEmpty()) {
                    log.infof("JBoss AS started in %dms. - Services [Total: %d, On-demand: %d. Started: %d]", elapsedTime, totalServices, onDemandServices, startedServices);
                    sendMessage(ServerManagerProtocolCommand.SERVER_STARTED);
                } else {
                    sendMessage(ServerManagerProtocolCommand.SERVER_START_FAILED);
                    final StringBuilder buff = new StringBuilder(String.format("JBoss AS server start failed. Attempted to start %d services in %dms", totalServices, elapsedTime));
                    buff.append("\nThe following services failed to start:\n");
                    for(Map.Entry<ServiceName, StartException> entry : serviceFailures.entrySet()) {
                        buff.append(String.format("\t%s => %s\n", entry.getKey(), entry.getValue().getMessage()));
                    }
                    log.error(buff.toString());
                }
            }
        };
    }
    
    private void launchCommunicationHandler() {
        this.serverCommunicationHandler = ServerCommunicationHandlerFactory.getInstance().getServerCommunicationHandler(getEnvironment(), messageHandler);
        Thread t = new Thread(this.serverCommunicationHandler.getController(), "Server Process");
        t.start();
    }

    private void sendMessage(ServerManagerProtocolCommand command) {
        try {
            byte[] bytes = command.createCommandBytes(null);
            serverCommunicationHandler.sendMessage(bytes);
        } catch (IOException e) {
            log.error("Failed to send message to Server Manager [" + command + "]", e);
        }
    }
}
