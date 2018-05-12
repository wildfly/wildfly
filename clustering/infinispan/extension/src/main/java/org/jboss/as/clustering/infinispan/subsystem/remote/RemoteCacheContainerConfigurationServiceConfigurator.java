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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.ProtocolVersion;
import org.infinispan.client.hotrod.configuration.ClusterConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ConnectionPoolConfiguration;
import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfiguration;
import org.infinispan.client.hotrod.configuration.NearCacheConfiguration;
import org.infinispan.client.hotrod.configuration.SecurityConfiguration;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.infinispan.subsystem.ThreadPoolResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition.Attribute;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Radoslav Husar
 */
public class RemoteCacheContainerConfigurationServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Supplier<Configuration> {

    private final Map<String, List<SupplierDependency<OutboundSocketBinding>>> clusters = new HashMap<>();
    private final Map<ThreadPoolResourceDefinition, SupplierDependency<ExecutorFactoryConfiguration>> threadPools = new EnumMap<>(ThreadPoolResourceDefinition.class);
    private final SupplierDependency<Module> module;
    private final SupplierDependency<ConnectionPoolConfiguration> connectionPool;
    private final SupplierDependency<NearCacheConfiguration> nearCache;
    private final SupplierDependency<SecurityConfiguration> security;

    private volatile int connectionTimeout;
    private volatile String defaultRemoteCluster;
    private volatile int keySizeEstimate;
    private volatile int maxRetries;
    private volatile String protocolVersion;
    private volatile int socketTimeout;
    private volatile boolean tcpNoDelay;
    private volatile boolean tcpKeepAlive;
    private volatile int valueSizeEstimate;

    RemoteCacheContainerConfigurationServiceConfigurator(PathAddress address) {
        super(RemoteCacheContainerResourceDefinition.Capability.CONFIGURATION, address);
        this.threadPools.put(ThreadPoolResourceDefinition.CLIENT, new ServiceSupplierDependency<>(ThreadPoolResourceDefinition.CLIENT.getServiceName(address)));
        this.module = new ServiceSupplierDependency<>(RemoteCacheContainerComponent.MODULE.getServiceName(address));
        this.connectionPool = new ServiceSupplierDependency<>(RemoteCacheContainerComponent.CONNECTION_POOL.getServiceName(address));
        this.nearCache = new ServiceSupplierDependency<>(RemoteCacheContainerComponent.NEAR_CACHE.getServiceName(address));
        this.security = new ServiceSupplierDependency<>(RemoteCacheContainerComponent.SECURITY.getServiceName(address));
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
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

        Resource container = context.readResource(PathAddress.EMPTY_ADDRESS);
        for (Resource.ResourceEntry entry : container.getChildren(RemoteClusterResourceDefinition.WILDCARD_PATH.getKey())) {
            String clusterName = entry.getName();
            ModelNode cluster = entry.getModel();
            List<String> bindings = StringListAttributeDefinition.unwrapValue(context, RemoteClusterResourceDefinition.Attribute.SOCKET_BINDINGS.resolveModelAttribute(context, cluster));
            List<SupplierDependency<OutboundSocketBinding>> bindingDependencies = new ArrayList<>(bindings.size());
            for (String binding : bindings) {
                bindingDependencies.add(new ServiceSupplierDependency<>(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING.getServiceName(context, binding)));
            }
            this.clusters.put(clusterName, bindingDependencies);
        }

        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<Configuration> configuration = new CompositeDependency(this.module, this.connectionPool, this.nearCache, this.security).register(builder).provides(this.getServiceName());
        for (Dependency dependency : this.threadPools.values()) {
            dependency.register(builder);
        }
        for (List<SupplierDependency<OutboundSocketBinding>> dependencies : this.clusters.values()) {
            for (Dependency dependency : dependencies) {
                dependency.register(builder);
            }
        }
        Service service = new FunctionalService<>(configuration, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Configuration get() {
        ConfigurationBuilder builder = new ConfigurationBuilder()
                .marshaller(new HotRodMarshaller(this.module.get()))
                .connectionTimeout(this.connectionTimeout)
                .keySizeEstimate(this.keySizeEstimate)
                .maxRetries(this.maxRetries)
                .version(ProtocolVersion.parseVersion(this.protocolVersion))
                .socketTimeout(this.socketTimeout)
                .tcpNoDelay(this.tcpNoDelay)
                .tcpKeepAlive(this.tcpKeepAlive)
                .valueSizeEstimate(this.valueSizeEstimate);

        builder.connectionPool().read(this.connectionPool.get());
        builder.nearCache().read(this.nearCache.get());
        builder.asyncExecutorFactory().read(this.threadPools.get(ThreadPoolResourceDefinition.CLIENT).get());

        for (Map.Entry<String, List<SupplierDependency<OutboundSocketBinding>>> cluster : this.clusters.entrySet()) {
            String clusterName = cluster.getKey();
            List<SupplierDependency<OutboundSocketBinding>> bindingDependencies = cluster.getValue();

            if (this.defaultRemoteCluster.equals(clusterName)) {
                for (Supplier<OutboundSocketBinding> bindingDependency : bindingDependencies) {
                    OutboundSocketBinding binding = bindingDependency.get();
                    builder.addServer().host(binding.getUnresolvedDestinationAddress()).port(binding.getDestinationPort());
                }
            } else {
                ClusterConfigurationBuilder clusterConfigurationBuilder = builder.addCluster(clusterName);
                for (Supplier<OutboundSocketBinding> bindingDependency : bindingDependencies) {
                    OutboundSocketBinding binding = bindingDependency.get();
                    clusterConfigurationBuilder.addClusterNode(binding.getUnresolvedDestinationAddress(), binding.getDestinationPort());
                }
            }
        }

        builder.security().read(this.security.get());

        return builder.build();
    }
}
