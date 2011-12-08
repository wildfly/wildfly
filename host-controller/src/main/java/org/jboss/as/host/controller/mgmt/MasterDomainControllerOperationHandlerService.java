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

import org.jboss.as.controller.remote.AbstractModelControllerOperationHandlerFactoryService;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandlerFactoryService;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.UnregisteredHostChannelRegistry;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.HandleableCloseable;

import java.io.IOException;

/**
 * Installs {@link MasterDomainControllerOperationHandlerImpl} which handles requests from slave DC to master DC.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MasterDomainControllerOperationHandlerService extends AbstractModelControllerOperationHandlerFactoryService {
    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");

    public static final ServiceName SERVICE_NAME = DomainController.SERVICE_NAME.append(ModelControllerClientOperationHandlerFactoryService.OPERATION_HANDLER_NAME_SUFFIX);

    private final DomainController domainController;
    private final UnregisteredHostChannelRegistry registry;

    public MasterDomainControllerOperationHandlerService(final DomainController domainController, final UnregisteredHostChannelRegistry registry) {
        this.domainController = domainController;
        this.registry = registry;
    }

    @Override
    public Channel.Key initialize(final ManagementChannel channel) {
        final MasterDomainControllerOperationHandlerImpl handler = new MasterDomainControllerOperationHandlerImpl(getExecutor(), getController(), registry, domainController, channel);
        final Channel.Receiver receiver = handler;
        channel.setReceiver(receiver);
        return channel.addCloseHandler(new CloseHandler<Channel>() {
            @Override
            public void handleClose(Channel closed, IOException exception) {
                handler.shutdown();
            }
        });
    }
}
