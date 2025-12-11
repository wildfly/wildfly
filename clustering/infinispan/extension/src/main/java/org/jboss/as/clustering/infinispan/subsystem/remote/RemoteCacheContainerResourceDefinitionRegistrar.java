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
import java.util.function.UnaryOperator;

import javax.management.MBeanServer;

import org.infinispan.client.hotrod.ProtocolVersion;
import org.infinispan.client.hotrod.configuration.ClusterConfiguration;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
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
import org.jboss.as.clustering.infinispan.subsystem.ConfigurationResourceDefinitionRegistrar;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanCacheContainerBindingFactory;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemModel;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.executor.MetricOperationStepHandler;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * Registers a resource definition for a remote cache container.
 *
 * @author Radoslav Husar
 */
public class RemoteCacheContainerResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<Configuration, ConfigurationBuilder> implements ResourceServiceConfigurator {

    public static final ResourceRegistration REGISTRATION = ResourceRegistration.of(PathElement.pathElement("remote-cache-container"));
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER_CONFIGURATION).build();

    public static final ModuleListAttributeDefinition MODULES = new ModuleListAttributeDefinition.Builder().setDefaultValue(Module.forClass(RemoteCacheContainer.class)).build();
    public static final DurationAttributeDefinition CONNECTION_TIMEOUT = new DurationAttributeDefinition.Builder("connection-timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMinutes(1)).build();
    public static final DurationAttributeDefinition SOCKET_TIMEOUT = new DurationAttributeDefinition.Builder("socket-timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMinutes(1)).build();
    public static final DurationAttributeDefinition TRANSACTION_TIMEOUT = new DurationAttributeDefinition.Builder("transaction-timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMinutes(1)).build();
    public static final CapabilityReferenceAttributeDefinition<ClusterConfiguration> DEFAULT_REMOTE_CLUSTER = new CapabilityReferenceAttributeDefinition.Builder<>("default-remote-cluster", CapabilityReference.builder(CAPABILITY, RemoteClusterResourceDefinitionRegistrar.SERVICE_DESCRIPTOR).withParentPath(REGISTRATION.getPathElement()).build()).setRequired(false).build();
    public static final EnumAttributeDefinition<HotRodMarshallerFactory> MARSHALLER = new EnumAttributeDefinition.Builder<>("marshaller", HotRodMarshallerFactory.LEGACY).build();
    public static final EnumAttributeDefinition<ProtocolVersion> PROTOCOL_VERSION = new EnumAttributeDefinition.Builder<>("protocol-version", ProtocolVersion.PROTOCOL_VERSION_41).setAllowedValues(EnumSet.complementOf(EnumSet.of(ProtocolVersion.PROTOCOL_VERSION_AUTO))).withResolver(ProtocolVersion::parseVersion).build();
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

    private final ServiceValueExecutorRegistry<RemoteCacheContainer> registry = ServiceValueExecutorRegistry.newInstance();

    public RemoteCacheContainerResourceDefinitionRegistrar() {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return REGISTRATION;
            }

            @Override
            public RuntimeCapability<Void> getCapability() {
                return CAPABILITY;
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addAttributes(List.of(MODULES, CONNECTION_TIMEOUT, SOCKET_TIMEOUT, TRANSACTION_TIMEOUT, DEFAULT_REMOTE_CLUSTER, MARSHALLER, PROTOCOL_VERSION, PROPERTIES, STATISTICS_ENABLED))
                .provideAttributes(EnumSet.allOf(Attribute.class))
                .addCapability(RemoteCacheContainerServiceConfigurator.CAPABILITY)
                .requireChildResources(Set.of(ClientThreadPool.ASYNC, RemoteComponentResourceRegistration.SECURITY))
                .withResourceTransformation(RemoteCacheContainerResource::new)
                ;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        new ConnectionPoolResourceDefinitionRegistrar().register(registration, context);
        new RemoteClusterResourceDefinitionRegistrar(super.get(), this.registry).register(registration, context);
        new SecurityResourceDefinitionRegistrar().register(registration, context);

        for (ClientThreadPool pool : EnumSet.allOf(ClientThreadPool.class)) {
            new ClientThreadPoolResourceDefinitionRegistrar(pool).register(registration, context);
        }

        if (context.isRuntimeOnlyRegistrationValid()) {
            new MetricOperationStepHandler<>(new RemoteCacheContainerMetricExecutor(this.registry), RemoteCacheContainerMetric.class).register(registration);

            new RemoteCacheRuntimeResourceDefinitionRegistrar(this.registry).register(registration, context);
        }

        return registration;
    }

    @Override
    public ResourceOperationRuntimeHandler get() {
        return ResourceOperationRuntimeHandler.combine(super.get(), ResourceOperationRuntimeHandler.configureService(this));
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        ResourceServiceInstaller captureInstaller = this.registry.capture(ServiceDependency.on(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER, name));
        ResourceServiceInstaller containerInstaller = RemoteCacheContainerServiceConfigurator.INSTANCE.configure(context, model);
        ResourceServiceInstaller bindingInstaller = new BinderServiceInstaller(InfinispanCacheContainerBindingFactory.REMOTE.apply(name), context.getCapabilityServiceName(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER, name));

        return ResourceServiceInstaller.combine(captureInstaller, containerInstaller, bindingInstaller);
    }

    @Override
    public ServiceDependency<ConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
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
        Set<Resource.ResourceEntry> entries = container.getChildren(RemoteClusterResourceDefinitionRegistrar.REGISTRATION.getPathElement().getKey());
        List<ServiceDependency<ClusterConfiguration>> remoteClusters = new ArrayList<>(entries.size());
        for (Resource.ResourceEntry entry : entries) {
            remoteClusters.add(ServiceDependency.on(RemoteClusterResourceDefinitionRegistrar.SERVICE_DESCRIPTOR, name, entry.getName()));
        }

        ServiceDependency<ExecutorFactoryConfiguration> executorFactory = ServiceDependency.on(ClientThreadPool.ASYNC.getServiceDescriptor(), name);

        ServiceDependency<MBeanServer> server = new MBeanServerResolver(CAPABILITY).resolve(context, model);

        ServiceDependency<List<Module>> modules = MODULES.resolve(context, model);
        ServiceDependency<SecurityConfiguration> security = ServiceDependency.on(SecurityResourceDefinitionRegistrar.SERVICE_DESCRIPTOR, name);
        ServiceDependency<ModuleLoader> loader = ServiceDependency.on(Services.JBOSS_SERVICE_MODULE_LOADER);
        Properties properties = new Properties();
        for (Map.Entry<String, String> entry : PROPERTIES.resolve(context, model).entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }

        return new ServiceDependency<>() {
            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                for (ServiceDependency<ClusterConfiguration> dependency : remoteClusters) {
                    dependency.accept(builder);
                }
                executorFactory.accept(builder);
                server.accept(builder);
                modules.accept(builder);
                security.accept(builder);
                loader.accept(builder);
            }

            @Override
            public ConfigurationBuilder get() {
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

                Marshaller marshaller = marshallerFactory.apply(loader.get(), modules.get());
                builder.marshaller(marshaller);
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

                return builder;
            }
        };
    }
}
