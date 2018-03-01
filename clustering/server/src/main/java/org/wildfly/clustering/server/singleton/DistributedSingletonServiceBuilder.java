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

import java.util.Optional;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonService;
import org.wildfly.clustering.singleton.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.election.SimpleSingletonElectionPolicy;

/**
 * Builds an MSC service that is only started on one node in the cluster at any given time.
 * @author Paul Ferraro
 */
public class DistributedSingletonServiceBuilder<T> implements SingletonServiceBuilder<T>, DistributedSingletonServiceContext<T> {

    private final ValueDependency<ServiceProviderRegistry<ServiceName>> registry;
    private final ValueDependency<CommandDispatcherFactory> dispatcherFactory;
    private final ServiceName serviceName;
    private final Service<T> primaryService;
    private final Optional<Service<T>> backupService;

    private volatile SingletonElectionPolicy electionPolicy = new SimpleSingletonElectionPolicy();
    private volatile int quorum = 1;

    public DistributedSingletonServiceBuilder(DistributedSingletonServiceBuilderContext context, ServiceName serviceName, Service<T> primaryService, Service<T> backupService) {
        this.registry = context.getServiceProviderRegistryDependency();
        this.dispatcherFactory = context.getCommandDispatcherFactoryDependency();
        this.serviceName = serviceName;
        this.primaryService = primaryService;
        this.backupService = Optional.ofNullable(backupService);
    }

    @Override
    public ServiceName getServiceName() {
        return this.serviceName;
    }

    @Override
    public ServiceBuilder<T> build(ServiceTarget target) {
        SingletonService<T> service = new DistributedSingletonService<>(this);
        ServiceBuilder<T> installer = target.addService(this.serviceName, service);
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
    public Value<ServiceProviderRegistry<ServiceName>> getServiceProviderRegistry() {
        return this.registry;
    }

    @Override
    public Value<CommandDispatcherFactory> getCommandDispatcherFactory() {
        return this.dispatcherFactory;
    }

    @Override
    public Service<T> getPrimaryService() {
        return this.primaryService;
    }

    @Override
    public Optional<Service<T>> getBackupService() {
        return this.backupService;
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
