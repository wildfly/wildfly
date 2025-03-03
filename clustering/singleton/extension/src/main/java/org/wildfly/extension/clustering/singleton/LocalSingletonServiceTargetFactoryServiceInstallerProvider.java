/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.LocalCacheServiceInstallerProvider;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.compat.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.compat.SingletonServiceConfigurator;
import org.wildfly.clustering.singleton.election.SingletonElectionListener;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.server.LocalSingletonServiceTarget;
import org.wildfly.clustering.singleton.service.SingletonServiceTarget;
import org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory;
import org.wildfly.common.function.Functions;
import org.wildfly.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs services per local cache for supporting local singleton services.
 * @author Paul Ferraro
 */
@MetaInfServices(LocalCacheServiceInstallerProvider.class)
public class LocalSingletonServiceTargetFactoryServiceInstallerProvider implements LocalCacheServiceInstallerProvider {

    @Override
    public Iterable<ServiceInstaller> apply(BinaryServiceConfiguration configuration) {
        ServiceDependency<GroupMember> member = ServiceDependency.<Group<GroupMember>>on(ServiceNameFactory.resolveServiceName(ClusteringServiceDescriptor.GROUP, ModelDescriptionConstants.LOCAL)).map(Group::getLocalMember);
        Function<ServiceBuilder<?>, Consumer<Singleton>> discardingStateProviderFactory = new Function<>() {
            @Override
            public Consumer<Singleton> apply(ServiceBuilder<?> builder) {
                return Functions.discardingConsumer();
            }
        };
        SingletonServiceTargetFactory factory = new org.wildfly.clustering.singleton.compat.SingletonServiceTargetFactory() {
            @Override
            public SingletonServiceTarget createSingletonServiceTarget(ServiceTarget target) {
                return new LocalSingletonServiceTarget(target, member, discardingStateProviderFactory);
            }

            @Override
            public SingletonServiceConfigurator createSingletonServiceConfigurator(ServiceName name) {
                Function<ServiceBuilder<?>, Consumer<Singleton>> singletonFactory = new Function<>() {
                    @Override
                    public Consumer<Singleton> apply(ServiceBuilder<?> builder) {
                        return builder.provides(name.append("singleton"));
                    }
                };
                return new SingletonServiceConfigurator() {
                    private SingletonElectionListener listener = null;

                    @Override
                    public ServiceName getServiceName() {
                        return name;
                    }

                    @Override
                    public org.wildfly.clustering.singleton.service.SingletonServiceBuilder<?> build(ServiceTarget target) {
                        return new LocalSingletonServiceTarget(target, member, singletonFactory).addService(name)
                                .withElectionListener(this.listener);
                    }

                    @Override
                    public SingletonServiceConfigurator withElectionPolicy(SingletonElectionPolicy policy) {
                        return this;
                    }

                    @Override
                    public SingletonServiceConfigurator withElectionListener(SingletonElectionListener listener) {
                        this.listener = listener;
                        return this;
                    }

                    @Override
                    public SingletonServiceConfigurator requireQuorum(int quorum) {
                        return this;
                    }
                };
            }

            @Override
            public <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> service) {
                return new SingletonServiceBuilder<>() {
                    private SingletonElectionListener listener = null;

                    @Override
                    public ServiceName getServiceName() {
                        return name;
                    }

                    @Override
                    public org.wildfly.clustering.singleton.service.SingletonServiceBuilder<T> build(ServiceTarget target) {
                        return new LocalSingletonServiceTarget(target, member, discardingStateProviderFactory).addService(name, service)
                                .withElectionListener(this.listener);
                    }

                    @Override
                    public SingletonServiceBuilder<T> withElectionPolicy(SingletonElectionPolicy policy) {
                        return this;
                    }

                    @Override
                    public SingletonServiceBuilder<T> withElectionListener(SingletonElectionListener listener) {
                        this.listener = listener;
                        return this;
                    }

                    @Override
                    public SingletonServiceBuilder<T> requireQuorum(int quorum) {
                        return this;
                    }
                };
            }

            @Override
            public <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> primaryService, Service<T> backupService) {
                // Ignore backup service
                return this.createSingletonServiceBuilder(name, primaryService);
            }
        };
        return List.of(ServiceInstaller.builder(factory)
                .provides(configuration.resolveServiceName(SingletonServiceTargetFactory.SERVICE_DESCRIPTOR))
                .provides(configuration.resolveServiceName(org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory.SERVICE_DESCRIPTOR))
                .provides(configuration.resolveServiceName(org.wildfly.clustering.singleton.SingletonServiceBuilderFactory.SERVICE_DESCRIPTOR))
                .build());
    }
}
