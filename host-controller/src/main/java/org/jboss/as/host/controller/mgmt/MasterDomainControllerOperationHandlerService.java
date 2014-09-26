/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.host.controller.mgmt;

import java.io.File;

import org.jboss.as.controller.remote.AbstractModelControllerOperationHandlerFactoryService;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandler;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandlerFactoryService;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementPongRequestHandler;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.remoting3.Channel;

/**
 * Installs {@link MasterDomainControllerOperationHandlerImpl} which handles requests from slave DC to master DC.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MasterDomainControllerOperationHandlerService extends AbstractModelControllerOperationHandlerFactoryService {

    public static final ServiceName SERVICE_NAME = DomainController.SERVICE_NAME.append(ModelControllerClientOperationHandlerFactoryService.OPERATION_HANDLER_NAME_SUFFIX);

    private final DomainController domainController;
    private final HostControllerRegistrationHandler.OperationExecutor operationExecutor;
    private final ManagementPongRequestHandler pongRequestHandler = new ManagementPongRequestHandler();
    private final File tempDir;

    public MasterDomainControllerOperationHandlerService(final DomainController domainController,
                                                         final HostControllerRegistrationHandler.OperationExecutor operationExecutor, File tempDir) {
        this.domainController = domainController;
        this.operationExecutor = operationExecutor;
        this.tempDir = tempDir;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        pongRequestHandler.resetConnectionId();
        super.start(context);
    }

    @Override
    public ManagementChannelHandler startReceiving(final Channel channel) {
        final ManagementChannelHandler handler = new ManagementChannelHandler(ManagementClientChannelStrategy.create(channel), getExecutor());
        handler.getAttachments().attach(ManagementChannelHandler.TEMP_DIR, tempDir);
        // Assemble the request handlers for the domain channel
        handler.addHandlerFactory(new HostControllerRegistrationHandler(handler, domainController, operationExecutor,
                getExecutor()));
        handler.addHandlerFactory(new ModelControllerClientOperationHandler(getController(), handler));
        handler.addHandlerFactory(new MasterDomainControllerOperationHandlerImpl(domainController, getExecutor()));
        handler.addHandlerFactory(pongRequestHandler);
        channel.receiveMessage(handler.getReceiver());
        return handler;
    }
}
