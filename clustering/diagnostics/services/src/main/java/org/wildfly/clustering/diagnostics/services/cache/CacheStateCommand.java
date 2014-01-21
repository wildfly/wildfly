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

import java.security.AccessController;
import java.util.List;

import org.infinispan.Cache;
import org.infinispan.interceptors.CacheMgmtInterceptor;
import org.infinispan.interceptors.TxInterceptor;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.rpc.RpcManagerImpl;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.StateTransferManager;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.diagnostics.services.ClusteringDiagnosticsServicesLogger;

public class CacheStateCommand implements Command<CacheState, String> {

    private static final long serialVersionUID = 8293200010304591386L;

    private static final Logger log = Logger.getLogger(CacheStateCommand.class.getPackage().getName());

    private final String cacheName;

    public CacheStateCommand(String cacheName) {
        this.cacheName = cacheName;
    }

    @Override
    public CacheState execute(String containerName) throws Exception {
        // trace logging
        ClusteringDiagnosticsServicesLogger.ROOT_LOGGER.executingCacheStateCommand(cacheName);

        ServiceRegistry registry = currentServiceContainer();

        Cache<?, ?> cache = null;

        // two cases: either the cache is defined as a service (web sessions) or it is defined as a cache within a container service (ejb)
        ServiceController<Cache<?, ?>> service = ServiceContainerHelper.findService(registry, CacheService.getServiceName(containerName, this.cacheName));
        if (service != null) {
            // cache is defined as a service - use it
            cache = service.getValue();
        } else {
            // the cache does not exist as a service, so try looking up the container
            ServiceController<EmbeddedCacheManager> containerService = ServiceContainerHelper.getService(registry, EmbeddedCacheManagerService.getServiceName(containerName));
            EmbeddedCacheManager container = containerService.getValue();
            if (container != null) {
                // found the container - now look up the cache
                cache = container.getCache(this.cacheName);
                if (cache == null)
                    return null;
            } else {
                return null;
            }
        }

        // get the view (in JGroupsAddress format)
        StateTransferManager stateTransferManager = cache.getAdvancedCache().getComponentRegistry().getStateTransferManager();
        List<Address> members = stateTransferManager.getCacheTopology().getMembers();

        // only process data if we are in the cache view
        CacheStateResponse response = new CacheStateResponse(members.toString());

        // get the operation stats and distribution
        CacheMgmtInterceptor cacheMgmtInterceptor = findInterceptor(cache.getAdvancedCache().getInterceptorChain(), CacheMgmtInterceptor.class);
        if (cacheMgmtInterceptor != null) {
            response.setCacheStatistics(cacheMgmtInterceptor);
        }

        // get the RPC stats
        RpcManagerImpl rpcManager = (RpcManagerImpl) cache.getAdvancedCache().getRpcManager();
        if (rpcManager != null) {
            response.setRpcStatistics(rpcManager);
        }

        // get the txn stats
        TxInterceptor txInterceptor = findInterceptor(cache.getAdvancedCache().getInterceptorChain(), TxInterceptor.class);
        if (txInterceptor != null) {
            response.setTxStatistics(txInterceptor);
        }

         // trace logging
        ClusteringDiagnosticsServicesLogger.ROOT_LOGGER.executedCacheStateCommand(cacheName);

        return response;
    }

    private static <T extends CommandInterceptor> T findInterceptor(List<CommandInterceptor> interceptors, Class<T> interceptorClass) {
        for (CommandInterceptor interceptor: interceptors) {
            if (interceptorClass.isInstance(interceptor)) {
                return interceptorClass.cast(interceptor);
            }
        }
        return null;
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
