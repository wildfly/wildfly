/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.management.MBeanServer;

import org.infinispan.client.hotrod.ProtocolVersion;
import org.infinispan.client.hotrod.configuration.ClusterConfiguration;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ConnectionPoolConfiguration;
import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfiguration;
import org.infinispan.client.hotrod.configuration.SecurityConfiguration;
import org.infinispan.client.hotrod.configuration.ServerConfiguration;
import org.infinispan.commons.jmx.MBeanServerLookup;
import org.infinispan.commons.marshall.Marshaller;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.clustering.controller.EnumAttributeDefinition;
import org.jboss.as.clustering.controller.MBeanServerResolver;
import org.jboss.as.clustering.controller.ModuleListAttributeDefinition;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.StatisticsEnabledAttributeDefinition;
import org.jboss.as.clustering.infinispan.jmx.MBeanServerProvider;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemModel;
import org.jboss.as.clustering.infinispan.subsystem.ResourceCapabilityDescription;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Describes a remote cache container resource.
 * @author Paul Ferraro
 */
public enum RemoteCacheContainerResourceDescription implements ResourceCapabilityDescription<Configuration>, ResourceServiceConfigurator {
    INSTANCE;

    public static PathElement pathElement(String containerName) {
        return PathElement.pathElement("remote-cache-container", containerName);
    }

    private static final PathElement PATH = pathElement(PathElement.WILDCARD_VALUE);
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER_CONFIGURATION).build();

    public static final ModuleListAttributeDefinition MODULES = new ModuleListAttributeDefinition.Builder().setDefaultValue(Module.forClass(RemoteCacheContainer.class)).build();
    public static final DurationAttributeDefinition CONNECTION_TIMEOUT = new DurationAttributeDefinition.Builder("connection-timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMinutes(1)).build();
    public static final DurationAttributeDefinition SOCKET_TIMEOUT = new DurationAttributeDefinition.Builder("socket-timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMinutes(1)).build();
    public static final DurationAttributeDefinition TRANSACTION_TIMEOUT = new DurationAttributeDefinition.Builder("transaction-timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMinutes(1)).build();
    public static final CapabilityReferenceAttributeDefinition<ClusterConfiguration> DEFAULT_REMOTE_CLUSTER = new CapabilityReferenceAttributeDefinition.Builder<>("default-remote-cluster", CapabilityReference.builder(CAPABILITY, RemoteClusterResourceDescription.INSTANCE.getServiceDescriptor()).withParentPath(PATH).build()).setRequired(false).build();
    public static final EnumAttributeDefinition<HotRodMarshallerFactory> MARSHALLER = new EnumAttributeDefinition.Builder<>("marshaller", HotRodMarshallerFactory.LEGACY).build();
    public static final EnumAttributeDefinition<ProtocolVersion> PROTOCOL_VERSION = new EnumAttributeDefinition.Builder<>("protocol-version", ProtocolVersion.PROTOCOL_VERSION_40).setAllowedValues(EnumSet.complementOf(EnumSet.of(ProtocolVersion.PROTOCOL_VERSION_AUTO))).withResolver(ProtocolVersion::parseVersion).build();
    public static final PropertiesAttributeDefinition PROPERTIES = new PropertiesAttributeDefinition.Builder("properties").build();
    public static final StatisticsEnabledAttributeDefinition STATISTICS_ENABLED = new StatisticsEnabledAttributeDefinition.Builder().build();

    public enum Attribute implements AttributeDefinitionProvider, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        KEY_SIZE_ESTIMATE("key-size-estimate", ModelType.INT, new ModelNode(64)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDeprecated(InfinispanSubsystemModel.VERSION_15_0_0.getVersion());
            }
        },
        VALUE_SIZE_ESTIMATE("value-size-estimate", ModelType.INT, new ModelNode(512)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDeprecated(InfinispanSubsystemModel.VERSION_15_0_0.getVersion());
            }
        },
        MAX_RETRIES("max-retries", ModelType.INT, new ModelNode(10)),
        TCP_NO_DELAY("tcp-no-delay", ModelType.BOOLEAN, ModelNode.TRUE),
        TCP_KEEP_ALIVE("tcp-keep-alive", ModelType.BOOLEAN, ModelNode.FALSE),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type))
                    .setAllowExpression(true)
                    .setRequired(defaultValue == null)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    @Override
    public PathElement getPathElement() {
        return PATH;
    }

    @Override
    public UnaryServiceDescriptor<Configuration> getServiceDescriptor() {
        return HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER_CONFIGURATION;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return CAPABILITY;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(Stream.of(MODULES, DEFAULT_REMOTE_CLUSTER, MARSHALLER, PROTOCOL_VERSION, CONNECTION_TIMEOUT, SOCKET_TIMEOUT, TRANSACTION_TIMEOUT, PROPERTIES, STATISTICS_ENABLED), ResourceDescriptor.stream(EnumSet.allOf(Attribute.class)));
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        ServiceDependency<ClusterConfiguration> defaultRemoteCluster = DEFAULT_REMOTE_CLUSTER.resolve(context, model);
        ProtocolVersion version = PROTOCOL_VERSION.resolve(context, model);
        BiFunction<ModuleLoader, List<Module>, Marshaller> marshallerFactory = MARSHALLER.resolve(context, model);
        Duration connectionTimeout = CONNECTION_TIMEOUT.resolve(context, model);
        Duration socketTimeout = SOCKET_TIMEOUT.resolve(context, model);
        Duration transactionTimeout = TRANSACTION_TIMEOUT.resolve(context, model);
        int maxRetries = Attribute.MAX_RETRIES.resolveModelAttribute(context, model).asInt();
        boolean tcpNoDelay = Attribute.TCP_NO_DELAY.resolveModelAttribute(context, model).asBoolean();
        boolean tcpKeepAlive = Attribute.TCP_KEEP_ALIVE.resolveModelAttribute(context, model).asBoolean();
        boolean statisticsEnabled = STATISTICS_ENABLED.resolve(context, model);

        if (marshallerFactory == HotRodMarshallerFactory.LEGACY) {
            InfinispanLogger.ROOT_LOGGER.marshallerEnumValueDeprecated(MARSHALLER.getName(), HotRodMarshallerFactory.LEGACY, EnumSet.complementOf(EnumSet.of(HotRodMarshallerFactory.LEGACY)));
        }

        Resource container = context.readResource(PathAddress.EMPTY_ADDRESS);
        Set<Resource.ResourceEntry> entries = container.getChildren(RemoteClusterResourceDescription.INSTANCE.getPathElement().getKey());
        List<ServiceDependency<ClusterConfiguration>> remoteClusters = new ArrayList<>(entries.size());
        for (Resource.ResourceEntry entry : entries) {
            remoteClusters.add(ServiceDependency.on(RemoteClusterResourceDescription.INSTANCE.getServiceDescriptor(), name, entry.getName()));
        }

        ServiceDependency<ExecutorFactoryConfiguration> executorFactory = ServiceDependency.on(ClientThreadPool.ASYNC.getServiceDescriptor(), name);

        ServiceDependency<MBeanServer> server = new MBeanServerResolver(CAPABILITY).resolve(context, model);

        ServiceDependency<List<Module>> containerModules = MODULES.resolve(context, model);
        ServiceDependency<ConnectionPoolConfiguration> connectionPool = ServiceDependency.on(ConnectionPoolResourceDescription.INSTANCE.getServiceDescriptor(), name);
        ServiceDependency<SecurityConfiguration> security = ServiceDependency.on(SecurityResourceDescription.INSTANCE.getServiceDescriptor(), name);
        ServiceDependency<ModuleLoader> loader = ServiceDependency.on(Services.JBOSS_SERVICE_MODULE_LOADER);
        Properties properties = new Properties();
        for (Map.Entry<String, String> entry : PROPERTIES.resolve(context, model).entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
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
                        .connectionTimeout((int) connectionTimeout.toMillis())
                        .maxRetries(maxRetries)
                        .version(version)
                        .socketTimeout((int) socketTimeout.toMillis())
                        .statistics()
                            .enabled(statisticsEnabled)
                            .jmxDomain("org.wildfly.clustering.infinispan")
                            .jmxEnabled(serverProvider != null)
                            .jmxName(name)
                            .mBeanServerLookup(serverProvider)
                        .tcpNoDelay(tcpNoDelay)
                        .tcpKeepAlive(tcpKeepAlive)
                        .transactionTimeout(transactionTimeout.toMillis(), TimeUnit.MILLISECONDS)
                        ;

                List<Module> modules = containerModules.get();
                Marshaller marshaller = marshallerFactory.apply(loader.get(), modules);
                builder.marshaller(marshaller);
                builder.connectionPool().read(connectionPool.get());
                builder.asyncExecutorFactory().read(executorFactory.get());

                ClusterConfiguration defaultCluster = defaultRemoteCluster.get();
                if (defaultCluster != null) {
                    for (ServerConfiguration server : defaultCluster.getCluster()) {
                        builder.addServer().read(server);
                    }
                }
                for (ServiceDependency<ClusterConfiguration> remoteCluster : remoteClusters) {
                    ClusterConfiguration cluster = remoteCluster.get();
                    String clusterName = cluster.getClusterName();
                    if ((defaultCluster == null) || !clusterName.equals(defaultCluster.getClusterName())) {
                        builder.addCluster(clusterName).read(cluster);
                    }
                }

                return builder.build();
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, configurationFactory)
                .requires(List.of(defaultRemoteCluster, server, containerModules, connectionPool, security, loader, executorFactory))
                .requires(remoteClusters)
                .build();
    }
}
