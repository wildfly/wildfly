/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.monitor.services.channel;

import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class ChannelManagementControllerService implements Service<ChannelManagementControllerService> {

    public static ServiceName getServiceName(String channelName) {
        return ChannelManagementService.getServiceName(channelName).append("controller");
    }

    private static final Logger log = Logger.getLogger(ChannelManagementControllerService.class.getPackage().getName());

    private final ServiceName name;
    private final String channelName;

    public ChannelManagementControllerService(ServiceName name, String channelName) {
        this.name = name;
        this.channelName = channelName;
    }

    @Override
    public ChannelManagementControllerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        if (log.isTraceEnabled()) {
            log.debugf("Starting ChannelManagementControllerService: channel name = %s\n", channelName);
        }

        // start the corresponding channel management service
        ServiceName serviceName = ChannelManagementService.getServiceName(channelName);
        ServiceRegistry registry = (ServiceRegistry) context.getController().getServiceContainer();
        ServiceController<ChannelManagement> controller = ServiceContainerHelper.findService(registry, serviceName);

        try {
            // set the mode to active to start the service
            controller.setMode(ServiceController.Mode.ACTIVE);
            // wait until the service comes up
            controller.awaitValue();
        } catch (InterruptedException e) {
            throw new StartException("failed to start RPC service for channel: " + channelName);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (log.isTraceEnabled()) {
            log.debugf("Stopping ChannelManagementControllerService: channel name = %s\n", channelName);
        }

        // Do we need to do anything here?

        /*
        //stop the corresponding channel management service
        ServiceName serviceName = ChannelManagementService.getServiceName(channelName);
        ServiceRegistry registry = (ServiceRegistry) context.getController().getServiceContainer();
        ServiceController<ChannelManagement> controller = ServiceContainerHelper.findService(registry, serviceName);

        try {
            // set the mode to on demand to stop the service
            controller.setMode(ServiceController.Mode.ON_DEMAND);
        } catch (IllegalStateException e) {
            log.warn("Problem stopping RPC manager service for channel: " + channelName);
        }
        */
    }
}
