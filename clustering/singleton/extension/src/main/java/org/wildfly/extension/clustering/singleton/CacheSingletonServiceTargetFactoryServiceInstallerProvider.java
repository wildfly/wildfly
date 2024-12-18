/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteredCacheServiceInstallerProvider;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.compat.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.compat.SingletonServiceConfigurator;
import org.wildfly.clustering.singleton.election.SingletonElectionListener;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.server.DefaultSingletonServiceBuilderContext;
import org.wildfly.clustering.singleton.server.DistributedSingletonServiceTarget;
import org.wildfly.clustering.singleton.server.LegacyDistributedSingletonService;
import org.wildfly.clustering.singleton.server.LegacySingletonServiceBuilder;
import org.wildfly.clustering.singleton.server.SingletonReference;
import org.wildfly.clustering.singleton.server.SingletonServiceBuilderContext;
import org.wildfly.clustering.singleton.server.SingletonServiceTargetContext;
import org.wildfly.clustering.singleton.service.SingletonServiceTarget;
import org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory;
import org.wildfly.common.function.Functions;
import org.wildfly.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs services per cache for supporting distributed singleton services.
 * @author Paul Ferraro
 */
@MetaInfServices(ClusteredCacheServiceInstallerProvider.class)
public class CacheSingletonServiceTargetFactoryServiceInstallerProvider implements ClusteredCacheServiceInstallerProvider {

    @SuppressWarnings({ "deprecation", "removal" })
    @Override
    public Iterable<ServiceInstaller> apply(BinaryServiceConfiguration configuration) {
        ServiceDependency<CommandDispatcherFactory<GroupMember>> dispatcherFactory = ServiceDependency.on(configuration.resolveServiceName(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY));
        ServiceDependency<ServiceProviderRegistrar<ServiceName, GroupMember>> registrar = ServiceDependency.on(configuration.resolveServiceName(ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR)).map(ServiceProviderRegistrar.class::cast);
        SingletonServiceTargetContext context = new SingletonServiceTargetContext() {
            @Override
            public ServiceDependency<ServiceProviderRegistrar<ServiceName, GroupMember>> getServiceProviderRegistrarDependency() {
                return registrar;
            }

            @Override
            public ServiceDependency<CommandDispatcherFactory<GroupMember>> getCommandDispatcherFactoryDependency() {
                return dispatcherFactory;
            }
        };
        Function<ServiceBuilder<?>, Consumer<Singleton>> discardingSingletonFactory = new Function<>() {
            @Override
            public Consumer<Singleton> apply(ServiceBuilder<?> builder) {
                return Functions.discardingConsumer();
            }
        };
        SingletonServiceTargetFactory factory = new org.wildfly.clustering.singleton.compat.SingletonServiceTargetFactory() {
            @Override
            public SingletonServiceTarget createSingletonServiceTarget(ServiceTarget target) {
                return new DistributedSingletonServiceTarget(target, context, discardingSingletonFactory);
            }

            @Deprecated
            @Override
            public SingletonServiceConfigurator createSingletonServiceConfigurator(ServiceName name) {
                SingletonServiceBuilderContext builderContext = new DefaultSingletonServiceBuilderContext(name, context);
                // Service created by SingletonServiceConfigurator provides Singleton interface via special service name
                Function<ServiceBuilder<?>, Consumer<Singleton>> singletonFactory = new Function<>() {
                    @Override
                    public Consumer<Singleton> apply(ServiceBuilder<?> builder) {
                        return builder.provides(name.append("singleton"));
                    }
                };
                return new SingletonServiceConfigurator() {
                    @Override
                    public ServiceName getServiceName() {
                        return name;
                    }

                    @Override
                    public org.wildfly.clustering.singleton.service.SingletonServiceBuilder<?> build(ServiceTarget target) {
                        return new DistributedSingletonServiceTarget(target, context, singletonFactory).addService(name)
                                .requireQuorum(builderContext.getQuorum())
                                .withElectionListener(builderContext.getElectionListener())
                                .withElectionPolicy(builderContext.getElectionPolicy());
                    }

                    @Override
                    public SingletonServiceConfigurator withElectionPolicy(SingletonElectionPolicy policy) {
                        builderContext.setElectionPolicy(policy);
                        return this;
                    }

                    @Override
                    public SingletonServiceConfigurator withElectionListener(SingletonElectionListener listener) {
                        builderContext.setElectionListener(listener);
                        return this;
                    }

                    @Override
                    public SingletonServiceConfigurator requireQuorum(int quorum) {
                        builderContext.setQuorum(quorum);
                        return this;
                    }
                };
            }

            @Deprecated
            @Override
            public <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> service) {
                return this.createSingletonServiceBuilder(name, service, null);
            }

            @Deprecated
            @Override
            public <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> primaryService, Service<T> backupService) {
                SingletonServiceBuilderContext builderContext = new DefaultSingletonServiceBuilderContext(name, context);
                return new SingletonServiceBuilder<>() {
                    @Override
                    public ServiceName getServiceName() {
                        return name;
                    }

                    @Override
                    public org.wildfly.clustering.singleton.service.SingletonServiceBuilder<T> build(ServiceTarget target) {
                        SingletonReference reference = new SingletonReference();
                        org.jboss.msc.service.Service<T> service = new LegacyDistributedSingletonService<>(builderContext, primaryService, backupService, reference);
                        return new LegacySingletonServiceBuilder<>(reference, builderContext, target.addService(name, service));
                    }

                    @Override
                    public SingletonServiceBuilder<T> withElectionPolicy(SingletonElectionPolicy policy) {
                        builderContext.setElectionPolicy(policy);
                        return this;
                    }

                    @Override
                    public SingletonServiceBuilder<T> withElectionListener(SingletonElectionListener listener) {
                        builderContext.setElectionListener(listener);
                        return this;
                    }

                    @Override
                    public SingletonServiceBuilder<T> requireQuorum(int quorum) {
                        builderContext.setQuorum(quorum);
                        return this;
                    }
                };
            }
        };
        return List.of(ServiceInstaller.builder(factory)
                .provides(configuration.resolveServiceName(SingletonServiceTargetFactory.SERVICE_DESCRIPTOR))
                .provides(configuration.resolveServiceName(org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory.SERVICE_DESCRIPTOR))
                .provides(configuration.resolveServiceName(org.wildfly.clustering.singleton.SingletonServiceBuilderFactory.SERVICE_DESCRIPTOR))
                .build());
    }
}
