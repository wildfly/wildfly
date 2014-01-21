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
package org.wildfly.clustering.diagnostics.services.cache;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.dispatcher.CommandResponse;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.diagnostics.services.ClusteringDiagnosticsServicesLogger;

public class CacheManagementService implements Service<CacheManagement>, CacheManagement {

    public static ServiceName getServiceName(String containerName) {
        return EmbeddedCacheManagerService.getServiceName(containerName).append("management");
    }

    private static final Logger log = Logger.getLogger(CacheManagementService.class.getPackage().getName());

    private final ServiceName name;
    private final Value<CommandDispatcherFactory> dispatcherFactory;
    private volatile CommandDispatcher<String> dispatcher;
    private final String containerName;

    public CacheManagementService(ServiceName name, Value<CommandDispatcherFactory> dispatcherFactory, String containerName) {
        this.name = name;
        this.dispatcherFactory = dispatcherFactory;
        this.containerName = containerName;
    }

    @Override
    public CacheManagement getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        // trace logging
        ClusteringDiagnosticsServicesLogger.ROOT_LOGGER.startingCacheManagementService(containerName);

        this.dispatcher = this.dispatcherFactory.getValue().createCommandDispatcher(this.name, this.containerName);
    }

    @Override
    public void stop(StopContext context) {
        // trace logging
        ClusteringDiagnosticsServicesLogger.ROOT_LOGGER.stoppingCacheManagementService(containerName);

        this.dispatcher.close();
    }

    @Override
    public Map<Node, CacheState> getCacheState(String cacheName) throws InterruptedException {
        // trace logging
        ClusteringDiagnosticsServicesLogger.ROOT_LOGGER.gettingCacheState(cacheName);

        Command<CacheState, String> command = new CacheStateCommand(cacheName);
        Map<Node, CommandResponse<CacheState>> responses = this.dispatcher.executeOnCluster(command);
        Map<Node, CacheState> result = new TreeMap<Node, CacheState>();
        for (Map.Entry<Node, CommandResponse<CacheState>> entry: responses.entrySet()) {
            try {
                CacheState response = entry.getValue().get();
                if (response != null) {
                    result.put(entry.getKey(), response);
                }
            } catch (ExecutionException e) {
                // Log warning
                ClusteringDiagnosticsServicesLogger.ROOT_LOGGER.exceptionProcessingCacheState(e);
            }
        }
        // trace logging
        ClusteringDiagnosticsServicesLogger.ROOT_LOGGER.gotCacheState(cacheName);

        return result;
    }
}
