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

package org.wildfly.clustering.server.singleton;

import java.util.function.Supplier;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonService;
import org.wildfly.clustering.singleton.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.election.SimpleSingletonElectionPolicy;

/**
 * Distributed {@link SingletonServiceBuilder} implementation that uses JBoss MSC 1.3.x service installation.
 * @author Paul Ferraro
 */
@Deprecated
public class DistributedSingletonServiceBuilder<T> extends SimpleServiceNameProvider implements SingletonServiceBuilder<T>, DistributedSingletonServiceContext {

    private final SupplierDependency<ServiceProviderRegistry<ServiceName>> registry;
    private final SupplierDependency<CommandDispatcherFactory> dispatcherFactory;
    private final Service<T> primaryService;
    private final Service<T> backupService;

    private volatile SingletonElectionPolicy electionPolicy = new SimpleSingletonElectionPolicy();
    private volatile int quorum = 1;

    public DistributedSingletonServiceBuilder(DistributedSingletonServiceConfiguratorContext context, ServiceName serviceName, Service<T> primaryService, Service<T> backupService) {
        super(serviceName);
        this.registry = context.getServiceProviderRegistryDependency();
        this.dispatcherFactory = context.getCommandDispatcherFactoryDependency();
        this.primaryService = primaryService;
        this.backupService = backupService;
    }

    @Override
    public ServiceBuilder<T> build(ServiceTarget target) {
        SingletonService<T> service = new LegacyDistributedSingletonService<>(this, this.primaryService, this.backupService);
        ServiceBuilder<T> installer = target.addService(this.getServiceName(), service);
        return new CompositeDependency(this.registry, this.dispatcherFactory).register(installer);
    }

    @Override
    public SingletonServiceBuilder<T> requireQuorum(int quorum) {
        if (quorum < 1) {
            throw ClusteringServerLogger.ROOT_LOGGER.invalidQuorum(quorum);
        }
        this.quorum = quorum;
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> electionPolicy(SingletonElectionPolicy electionPolicy) {
        this.electionPolicy = electionPolicy;
        return this;
    }

    @Override
    public Supplier<ServiceProviderRegistry<ServiceName>> getServiceProviderRegistry() {
        return this.registry;
    }

    @Override
    public Supplier<CommandDispatcherFactory> getCommandDispatcherFactory() {
        return this.dispatcherFactory;
    }

    @Override
    public SingletonElectionPolicy getElectionPolicy() {
        return this.electionPolicy;
    }

    @Override
    public int getQuorum() {
        return this.quorum;
    }
}
