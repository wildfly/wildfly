/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.management.MBeanServer;

import org.infinispan.client.hotrod.ProtocolVersion;
import org.infinispan.client.hotrod.configuration.ClusterConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ConfigurationChildBuilder;
import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfiguration;
import org.infinispan.client.hotrod.configuration.SecurityConfiguration;
import org.infinispan.client.hotrod.configuration.ServerConfigurationBuilder;
import org.infinispan.commons.jmx.MBeanServerLookup;
import org.infinispan.commons.marshall.Marshaller;
import org.jboss.as.clustering.controller.MBeanServerResolver;
import org.jboss.as.clustering.infinispan.jmx.MBeanServerProvider;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition.Attribute;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
public enum RemoteCacheContainerConfigurationServiceConfigurator implements ResourceServiceConfigurator {
    INSTANCE;

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        int connectionTimeout = Attribute.CONNECTION_TIMEOUT.resolveModelAttribute(context, model).asInt();
        String defaultRemoteCluster = Attribute.DEFAULT_REMOTE_CLUSTER.resolveModelAttribute(context, model).asString();
        int maxRetries = Attribute.MAX_RETRIES.resolveModelAttribute(context, model).asInt();
        ProtocolVersion version = ProtocolVersion.parseVersion(Attribute.PROTOCOL_VERSION.resolveModelAttribute(context, model).asString());
        int socketTimeout = Attribute.SOCKET_TIMEOUT.resolveModelAttribute(context, model).asInt();
        boolean tcpNoDelay = Attribute.TCP_NO_DELAY.resolveModelAttribute(context, model).asBoolean();
        boolean tcpKeepAlive = Attribute.TCP_KEEP_ALIVE.resolveModelAttribute(context, model).asBoolean();
        boolean statisticsEnabled = Attribute.STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
        long transactionTimeout = Attribute.TRANSACTION_TIMEOUT.resolveModelAttribute(context, model).asLong();
        BiFunction<ModuleLoader, List<Module>, Marshaller> marshallerFactory = HotRodMarshallerFactory.valueOf(Attribute.MARSHALLER.resolveModelAttribute(context, model).asString());

        Map<String, Collection<ServiceDependency<OutboundSocketBinding>>> clusters = new TreeMap<>();

        Resource container = context.readResource(PathAddress.EMPTY_ADDRESS);
        for (Resource.ResourceEntry entry : container.getChildren(RemoteClusterResourceDefinition.WILDCARD_PATH.getKey())) {
            String clusterName = entry.getName();
            ModelNode cluster = entry.getModel();
            List<ModelNode> bindingNames = RemoteClusterResourceDefinition.Attribute.SOCKET_BINDINGS.resolveModelAttribute(context, cluster).asListOrEmpty();
            List<ServiceDependency<OutboundSocketBinding>> bindings = new ArrayList<>(bindingNames.size());
            for (ModelNode bindingName : bindingNames) {
                bindings.add(ServiceDependency.on(OutboundSocketBinding.SERVICE_DESCRIPTOR, bindingName.asString()));
            }
            clusters.put(clusterName, bindings);
        }

        Map<ClientThreadPoolResourceDefinition, ServiceDependency<ExecutorFactoryConfiguration>> pools = new EnumMap<>(ClientThreadPoolResourceDefinition.class);
        for (ClientThreadPoolResourceDefinition pool : EnumSet.allOf(ClientThreadPoolResourceDefinition.class)) {
            pools.put(pool, ServiceDependency.on(pool, name));
        }

        ServiceDependency<MBeanServer> server = new MBeanServerResolver(RemoteCacheContainerResourceDefinition.REMOTE_CACHE_CONTAINER_CONFIGURATION).resolve(context, model);

        ServiceDependency<List<Module>> containerModules = ServiceDependency.on(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER_MODULES, name);
        ServiceDependency<SecurityConfiguration> security = ServiceDependency.on(SecurityResourceDefinition.SERVICE_DESCRIPTOR, name);
        ServiceDependency<ModuleLoader> loader = ServiceDependency.on(Services.JBOSS_SERVICE_MODULE_LOADER);

        Properties properties = new Properties();
        for (Property property : Attribute.PROPERTIES.resolveModelAttribute(context, model).asPropertyListOrEmpty()) {
            properties.setProperty(property.getName(), property.getValue().asString());
        }

        Supplier<Configuration> configurationFactory = new Supplier<>() {
            @Override
            public Configuration get() {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                // Configure formal security first
                builder.security().read(security.get());
                MBeanServerLookup serverProvider = Optional.ofNullable(server.get()).map(MBeanServerProvider::new).orElse(null);
                // Apply properties next, which may override formal security configuration
                builder.withProperties(properties)
                        .connectionTimeout(connectionTimeout)
                        .maxRetries(maxRetries)
                        .version(version)
                        .socketTimeout(socketTimeout)
                        .statistics()
                            .enabled(statisticsEnabled)
                            .jmxDomain("org.wildfly.clustering.infinispan")
                            .jmxEnabled(serverProvider != null)
                            .jmxName(name)
                            .mBeanServerLookup(serverProvider)
                        .tcpNoDelay(tcpNoDelay)
                        .tcpKeepAlive(tcpKeepAlive)
                        .transactionTimeout(transactionTimeout, TimeUnit.MILLISECONDS)
                        ;

                List<Module> modules = containerModules.get();
                Marshaller marshaller = marshallerFactory.apply(loader.get(), modules);
                builder.marshaller(marshaller);
                builder.asyncExecutorFactory().read(pools.get(ClientThreadPoolResourceDefinition.ASYNC).get());

                for (Map.Entry<String, Collection<ServiceDependency<OutboundSocketBinding>>> entry : clusters.entrySet()) {
                    String clusterName = entry.getKey();
                    ClusterConfigurator<? extends ConfigurationChildBuilder> configurator = defaultRemoteCluster.equals(clusterName) ? DefaultClusterConfigurator.INSTANCE : new NonDefaultClusterConfigurator(clusterName);
                    this.configureCluster(builder, configurator, entry.getValue());
                }

                return builder.build();
            }

            private <C extends ConfigurationChildBuilder> void configureCluster(ConfigurationBuilder builder, ClusterConfigurator<C> configurator, Collection<ServiceDependency<OutboundSocketBinding>> dependencies) {
                C cluster = configurator.addCluster(builder);
                for (Supplier<OutboundSocketBinding> dependency : dependencies) {
                    OutboundSocketBinding binding = dependency.get();
                    configurator.getBindingConsumer(cluster).accept(binding.getUnresolvedDestinationAddress(), binding.getDestinationPort());
                }
            }
        };
        return CapabilityServiceInstaller.builder(RemoteCacheContainerResourceDefinition.REMOTE_CACHE_CONTAINER_CONFIGURATION, configurationFactory)
                .requires(List.of(server, containerModules, security, loader))
                .requires(pools.values())
                .requires(clusters.values().stream().<Consumer<RequirementServiceBuilder<?>>>flatMap(Collection::stream).collect(Collectors.toUnmodifiableList()))
                .build();
    }

    interface ClusterConfigurator<B extends ConfigurationChildBuilder> {
        B addCluster(ConfigurationBuilder builder);
        ObjIntConsumer<String> getBindingConsumer(B builder);
        Consumer<String> getSNIHostNameConsumer(B builder);
    }

    enum DefaultClusterConfigurator implements ClusterConfigurator<ServerConfigurationBuilder> {
        INSTANCE;

        @Override
        public ServerConfigurationBuilder addCluster(ConfigurationBuilder builder) {
            return builder.addServer();
        }

        @Override
        public ObjIntConsumer<String> getBindingConsumer(ServerConfigurationBuilder server) {
            return new ObjIntConsumer<>() {
                @Override
                public void accept(String host, int port) {
                    server.host(host).port(port);
                }
            };
        }

        @Override
        public Consumer<String> getSNIHostNameConsumer(ServerConfigurationBuilder server) {
            return server.security().ssl()::sniHostName;
        }
    }

    static class NonDefaultClusterConfigurator implements ClusterConfigurator<ClusterConfigurationBuilder> {
        private final String clusterName;

        NonDefaultClusterConfigurator(String clusterName) {
            this.clusterName = clusterName;
        }

        @Override
        public ClusterConfigurationBuilder addCluster(ConfigurationBuilder builder) {
            return builder.addCluster(this.clusterName);
        }

        @Override
        public ObjIntConsumer<String> getBindingConsumer(ClusterConfigurationBuilder cluster) {
            return cluster::addClusterNode;
        }

        @Override
        public Consumer<String> getSNIHostNameConsumer(ClusterConfigurationBuilder cluster) {
            return cluster::clusterSniHostName;
        }
    }
}
