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
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.management.MBeanServer;

import org.infinispan.client.hotrod.ProtocolVersion;
import org.infinispan.client.hotrod.configuration.ClusterConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ConnectionPoolConfiguration;
import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfiguration;
import org.infinispan.client.hotrod.configuration.SecurityConfiguration;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.util.AggregatedClassLoader;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.CommonRequirement;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.infinispan.MBeanServerProvider;
import org.jboss.as.clustering.infinispan.subsystem.ThreadPoolResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition.Attribute;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
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
 * @author Paul Ferraro
 */
public class RemoteCacheContainerConfigurationServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Supplier<Configuration> {

    private final Map<String, List<SupplierDependency<OutboundSocketBinding>>> clusters = new HashMap<>();
    private final Map<ThreadPoolResourceDefinition, SupplierDependency<ExecutorFactoryConfiguration>> threadPools = new EnumMap<>(ThreadPoolResourceDefinition.class);
    private final SupplierDependency<ModuleLoader> loader;
    private final SupplierDependency<List<Module>> modules;
    private final Properties properties = new Properties();
    private final SupplierDependency<ConnectionPoolConfiguration> connectionPool;
    private final SupplierDependency<SecurityConfiguration> security;

    private volatile SupplierDependency<MBeanServer> server;
    private volatile int connectionTimeout;
    private volatile String defaultRemoteCluster;
    private volatile int maxRetries;
    private volatile String protocolVersion;
    private volatile int socketTimeout;
    private volatile boolean tcpNoDelay;
    private volatile boolean tcpKeepAlive;
    private volatile boolean statisticsEnabled;
    private volatile long transactionTimeout;
    private volatile HotRodMarshallerFactory marshallerFactory;

    RemoteCacheContainerConfigurationServiceConfigurator(PathAddress address) {
        super(RemoteCacheContainerResourceDefinition.Capability.CONFIGURATION, address);
        this.loader = new ServiceSupplierDependency<>(Services.JBOSS_SERVICE_MODULE_LOADER);
        this.threadPools.put(ThreadPoolResourceDefinition.CLIENT, new ServiceSupplierDependency<>(ThreadPoolResourceDefinition.CLIENT.getServiceName(address)));
        this.modules = new ServiceSupplierDependency<>(RemoteCacheContainerComponent.MODULES.getServiceName(address));
        this.connectionPool = new ServiceSupplierDependency<>(RemoteCacheContainerComponent.CONNECTION_POOL.getServiceName(address));
        this.security = new ServiceSupplierDependency<>(RemoteCacheContainerComponent.SECURITY.getServiceName(address));
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.connectionTimeout = Attribute.CONNECTION_TIMEOUT.resolveModelAttribute(context, model).asInt();
        this.defaultRemoteCluster = Attribute.DEFAULT_REMOTE_CLUSTER.resolveModelAttribute(context, model).asString();
        this.maxRetries = Attribute.MAX_RETRIES.resolveModelAttribute(context, model).asInt();
        this.protocolVersion = Attribute.PROTOCOL_VERSION.resolveModelAttribute(context, model).asString();
        this.socketTimeout = Attribute.SOCKET_TIMEOUT.resolveModelAttribute(context, model).asInt();
        this.tcpNoDelay = Attribute.TCP_NO_DELAY.resolveModelAttribute(context, model).asBoolean();
        this.tcpKeepAlive = Attribute.TCP_KEEP_ALIVE.resolveModelAttribute(context, model).asBoolean();
        this.statisticsEnabled = Attribute.STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
        this.transactionTimeout = Attribute.TRANSACTION_TIMEOUT.resolveModelAttribute(context, model).asLong();
        this.marshallerFactory = HotRodMarshallerFactory.valueOf(Attribute.MARSHALLER.resolveModelAttribute(context, model).asString());

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

        this.server = context.hasOptionalCapability(CommonRequirement.MBEAN_SERVER.getName(), null, null) ? new ServiceSupplierDependency<>(CommonRequirement.MBEAN_SERVER.getServiceName(context)) : null;

        this.properties.clear();
        for (Property property : Attribute.PROPERTIES.resolveModelAttribute(context, model).asPropertyListOrEmpty()) {
            this.properties.setProperty(property.getName(), property.getValue().asString());
        }

        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<Configuration> configuration = new CompositeDependency(this.loader, this.modules, this.connectionPool, this.security, this.server).register(builder).provides(this.getServiceName());
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
        String name = this.getServiceName().getSimpleName();
        ConfigurationBuilder builder = new ConfigurationBuilder();
        // Configure formal security first
        builder.security().read(this.security.get());
        // Apply properties next, which may override formal security configuration
        builder.withProperties(this.properties)
                .connectionTimeout(this.connectionTimeout)
                .maxRetries(this.maxRetries)
                .version(ProtocolVersion.parseVersion(this.protocolVersion))
                .socketTimeout(this.socketTimeout)
                .statistics()
                    .enabled(this.statisticsEnabled)
                    .jmxDomain("org.wildfly.clustering.infinispan")
                    .jmxEnabled(this.server != null)
                    .jmxName(name)
                    .mBeanServerLookup((this.server != null) ? new MBeanServerProvider(this.server.get()) : null)
                .tcpNoDelay(this.tcpNoDelay)
                .tcpKeepAlive(this.tcpKeepAlive)
                .transactionTimeout(this.transactionTimeout, TimeUnit.MILLISECONDS)
                ;

        List<Module> modules = this.modules.get();
        Marshaller marshaller = this.marshallerFactory.apply(this.loader.get(), modules);
        builder.marshaller(marshaller);
        builder.classLoader(modules.size() > 1 ? new AggregatedClassLoader(modules.stream().map(Module::getClassLoader).collect(Collectors.toList())) : modules.get(0).getClassLoader());
        builder.connectionPool().read(this.connectionPool.get());
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

        return builder.build();
    }
}
