/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher;

import java.util.List;
import java.util.function.BiFunction;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jgroups.Address;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.group.GroupCommandDispatcherFactory;
import org.wildfly.clustering.server.infinispan.dispatcher.ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration;
import org.wildfly.clustering.server.infinispan.dispatcher.EmbeddedCacheManagerCommandDispatcherFactory;
import org.wildfly.clustering.server.infinispan.dispatcher.LocalEmbeddedCacheManagerCommandDispatcherFactoryConfiguration;
import org.wildfly.clustering.server.jgroups.ChannelGroupMember;
import org.wildfly.clustering.server.jgroups.dispatcher.ChannelCommandDispatcherFactory;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public enum CacheContainerCommandDispatcherFactoryServiceInstallerFactory implements BiFunction<String, String, ServiceInstaller> {
    INSTANCE() {
        @Override
        public ServiceInstaller apply(String containerName, String channelName) {
            return ((channelName == ModelDescriptionConstants.LOCAL) ? LOCAL : CHANNEL).apply(containerName, channelName);
        }
    },
    CHANNEL() {
        @Override
        public ServiceInstaller apply(String containerName, String channelName) {
            ServiceDependency<EmbeddedCacheManager> container = ServiceDependency.on(InfinispanServiceDescriptor.CACHE_CONTAINER, containerName);
            ServiceDependency<ChannelCommandDispatcherFactory> dispatcherFactory = ServiceDependency.on(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, channelName).map(ChannelCommandDispatcherFactory.class::cast);
            ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration configuration = new ChannelEmbeddedCacheManagerCommandDispatcherFactoryConfiguration() {
                @Override
                public EmbeddedCacheManager getCacheContainer() {
                    return container.get();
                }

                @Override
                public GroupCommandDispatcherFactory<Address, ChannelGroupMember> getCommandDispatcherFactory() {
                    return dispatcherFactory.get();
                }
            };
            return ServiceInstaller.builder(EmbeddedCacheManagerCommandDispatcherFactory::new, Functions.constantSupplier(configuration))
                    .provides(ServiceNameFactory.resolveServiceName(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, containerName))
                    .requires(List.of(container, dispatcherFactory))
                    .build();
        }
    },
    LOCAL() {
        @Override
        public ServiceInstaller apply(String containerName, String channelName) {
            ServiceDependency<EmbeddedCacheManager> container = ServiceDependency.on(InfinispanServiceDescriptor.CACHE_CONTAINER, containerName);
            LocalEmbeddedCacheManagerCommandDispatcherFactoryConfiguration configuration = new LocalEmbeddedCacheManagerCommandDispatcherFactoryConfiguration() {
                @Override
                public EmbeddedCacheManager getCacheContainer() {
                    return container.get();
                }
            };
            return ServiceInstaller.builder(EmbeddedCacheManagerCommandDispatcherFactory::new, Functions.constantSupplier(configuration))
                    .provides(ServiceNameFactory.resolveServiceName(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, containerName))
                    .requires(List.of(container))
                    .build();
        }
    },
    ;
}
