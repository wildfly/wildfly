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
package org.wildfly.clustering.monitor.services.cache;

import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.monitor.services.ClusteringMonitorServicesLogger;
import org.wildfly.clustering.monitor.services.ClusteringMonitorServicesMessages;

public class CacheManagementControllerService implements Service<CacheManagementControllerService> {

    public static ServiceName getServiceName(String containerName) {
        return CacheManagementService.getServiceName(containerName).append("controller");
    }

    private static final Logger log = Logger.getLogger(CacheManagementControllerService.class.getPackage().getName());

    private final ServiceName name;
    private final String containerName;
    private volatile ServiceController.Mode mode = null;

    public CacheManagementControllerService(ServiceName name, String containerName) {
        this.name = name;
        this.containerName = containerName;
    }

    @Override
    public CacheManagementControllerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        // trace logging
        ClusteringMonitorServicesLogger.ROOT_LOGGER.startingCacheManagementControllerService(containerName);

        // start the corresponding cache management service
        ServiceName serviceName = CacheManagementService.getServiceName(containerName);
        ServiceRegistry registry = (ServiceRegistry) context.getController().getServiceContainer();
        ServiceController<CacheManagement> controller = ServiceContainerHelper.findService(registry, serviceName);

        try {
            // set the mode to active to start the service
            controller.setMode(ServiceController.Mode.ACTIVE);
            // wait until the service comes up
            controller.awaitValue();
        } catch (InterruptedException e) {
            throw ClusteringMonitorServicesMessages.MESSAGES.failedToStartRPCServiceForContainer(containerName);
        }
    }

    @Override
    public void stop(StopContext context) {
        // trace logging
        ClusteringMonitorServicesLogger.ROOT_LOGGER.stoppingCacheManagementControllerService(containerName);

        // Do we need to do anything here?

        /*
        //stop the corresponding channel management service
        ServiceName serviceName = ChannelManagementService.getServiceName(containerName);
        ServiceRegistry registry = (ServiceRegistry) context.getController().getServiceContainer();
        ServiceController<ChannelManagement> controller = ServiceContainerHelper.findService(registry, serviceName);

        try {
            // set the mode to on demand to stop the service
            controller.setMode(ServiceController.Mode.ON_DEMAND);
        } catch (IllegalStateException e) {
            log.warn("Problem stopping RPC manager service for cache container: " + containerName);
        }
        */
    }
}
