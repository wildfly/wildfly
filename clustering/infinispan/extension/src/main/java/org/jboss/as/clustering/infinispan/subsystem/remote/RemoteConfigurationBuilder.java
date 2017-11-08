/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem.remote;

import static org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition.Attribute;
import static org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition.Capability.CONFIGURATION;

import java.net.UnknownHostException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.configuration.ClusterConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ConnectionPoolConfiguration;
import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfiguration;
import org.infinispan.client.hotrod.configuration.NearCacheConfiguration;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.clustering.infinispan.subsystem.ThreadPoolResourceDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Radoslav Husar
 */
public class RemoteConfigurationBuilder implements ResourceServiceBuilder<Configuration>, Value<Configuration> {

    private final PathAddress address;

    private final Map<String, List<InjectedValueDependency<OutboundSocketBinding>>> clusters = new HashMap<>();
    private final InjectedValue<ConnectionPoolConfiguration> connectionPool = new InjectedValue<>();
    private final InjectedValue<NearCacheConfiguration> nearCache = new InjectedValue<>();
    private final Map<ThreadPoolResourceDefinition, ValueDependency<ExecutorFactoryConfiguration>> threadPools = new EnumMap<>(ThreadPoolResourceDefinition.class);

    private volatile int connectionTimeout;
    private volatile String defaultRemoteCluster;
    private volatile int keySizeEstimate;
    private volatile int maxRetries;
    private volatile String protocolVersion;
    private volatile int socketTimeout;
    private volatile boolean tcpNoDelay;
    private volatile boolean tcpKeepAlive;
    private volatile int valueSizeEstimate;

    RemoteConfigurationBuilder(PathAddress address) {
        this.address = address;
        this.threadPools.put(ThreadPoolResourceDefinition.CLIENT, new InjectedValueDependency<>(ThreadPoolResourceDefinition.CLIENT.getServiceName(address), ExecutorFactoryConfiguration.class));
    }

    @Override
    public ServiceName getServiceName() {
        return CONFIGURATION.getServiceName(this.address);
    }

    @Override
    public Builder<Configuration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.connectionTimeout = Attribute.CONNECTION_TIMEOUT.resolveModelAttribute(context, model).asInt();
        this.defaultRemoteCluster = Attribute.DEFAULT_REMOTE_CLUSTER.resolveModelAttribute(context, model).asString();
        this.keySizeEstimate = Attribute.KEY_SIZE_ESTIMATE.resolveModelAttribute(context, model).asInt();
        this.maxRetries = Attribute.MAX_RETRIES.resolveModelAttribute(context, model).asInt();
        this.protocolVersion = Attribute.PROTOCOL_VERSION.resolveModelAttribute(context, model).asString();
        this.socketTimeout = Attribute.SOCKET_TIMEOUT.resolveModelAttribute(context, model).asInt();
        this.tcpNoDelay = Attribute.TCP_NO_DELAY.resolveModelAttribute(context, model).asBoolean();
        this.tcpKeepAlive = Attribute.TCP_KEEP_ALIVE.resolveModelAttribute(context, model).asBoolean();
        this.valueSizeEstimate = Attribute.VALUE_SIZE_ESTIMATE.resolveModelAttribute(context, model).asInt();

        this.clusters.clear();
        if (model.hasDefined(RemoteClusterResourceDefinition.WILDCARD_PATH.getKey())) {
            for (Property property : model.get(RemoteClusterResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                String clusterName = property.getName();
                List<InjectedValueDependency<OutboundSocketBinding>> valueDependencies = StringListAttributeDefinition.unwrapValue(context, RemoteClusterResourceDefinition.Attribute.SOCKET_BINDINGS.resolveModelAttribute(context, property.getValue())).stream()
                        .map(binding -> new InjectedValueDependency<>(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING.getServiceName(context, binding), OutboundSocketBinding.class))
                        .collect(Collectors.toList());
                this.clusters.put(clusterName, valueDependencies);
            }
        }

        return this;
    }

    @Override
    public ServiceBuilder<Configuration> build(ServiceTarget target) {
        ServiceBuilder<Configuration> builder = target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(RemoteCacheContainerComponent.CONNECTION_POOL.getServiceName(this.address), ConnectionPoolConfiguration.class, this.connectionPool)
                .addDependency(RemoteCacheContainerComponent.NEAR_CACHE.getServiceName(address), NearCacheConfiguration.class, this.nearCache)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                ;
        for (ValueDependency<ExecutorFactoryConfiguration> dependency : this.threadPools.values()) {
            dependency.register(builder);
        }
        for (List<InjectedValueDependency<OutboundSocketBinding>> dependencies : this.clusters.values()) {
            for (InjectedValueDependency<OutboundSocketBinding> dependency : dependencies) {
                dependency.register(builder);
            }
        }

        return builder;
    }

    @Override
    public Configuration getValue() {
        ConfigurationBuilder builder = new ConfigurationBuilder()
                .connectionTimeout(this.connectionTimeout)
                .keySizeEstimate(this.keySizeEstimate)
                .maxRetries(this.maxRetries)
                .protocolVersion(this.protocolVersion)
                .socketTimeout(this.socketTimeout)
                .tcpNoDelay(this.tcpNoDelay)
                .tcpKeepAlive(this.tcpKeepAlive)
                .valueSizeEstimate(this.valueSizeEstimate);

        builder.connectionPool().read(this.connectionPool.getValue());
        builder.nearCache().read(this.nearCache.getValue());
        builder.asyncExecutorFactory().read(this.threadPools.get(ThreadPoolResourceDefinition.CLIENT).getValue());

        for (Map.Entry<String, List<InjectedValueDependency<OutboundSocketBinding>>> cluster : this.clusters.entrySet()) {
            String clusterName = cluster.getKey();
            List<InjectedValueDependency<OutboundSocketBinding>> injectedValueDependencies = cluster.getValue();

            if (this.defaultRemoteCluster.equals(clusterName)) {
                for (InjectedValueDependency<OutboundSocketBinding> bindingInjectedValueDependency : injectedValueDependencies) {
                    OutboundSocketBinding binding = bindingInjectedValueDependency.getValue();
                    try {
                        builder.addServer().host(binding.getResolvedDestinationAddress().getHostAddress()).port(binding.getDestinationPort());
                    } catch (UnknownHostException e) {
                        throw InfinispanLogger.ROOT_LOGGER.failedToInjectSocketBinding(e, binding);
                    }
                }
            } else {
                ClusterConfigurationBuilder clusterConfigurationBuilder = builder.addCluster(clusterName);
                for (InjectedValueDependency<OutboundSocketBinding> bindingInjectedValueDependency : injectedValueDependencies) {
                    OutboundSocketBinding binding = bindingInjectedValueDependency.getValue();
                    try {
                        clusterConfigurationBuilder.addClusterNode(binding.getResolvedDestinationAddress().getHostAddress(), binding.getDestinationPort());
                    } catch (UnknownHostException e) {
                        throw InfinispanLogger.ROOT_LOGGER.failedToInjectSocketBinding(e, binding);
                    }
                }
            }
        }

        return builder.build();
    }
}
