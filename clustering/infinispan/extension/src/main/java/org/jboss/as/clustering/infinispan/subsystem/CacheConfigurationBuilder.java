/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CacheResourceDefinition.Attribute.*;
import static org.jboss.as.clustering.infinispan.subsystem.CacheResourceDefinition.Capability.*;

import java.util.function.Consumer;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.GroupsConfigurationBuilder;
import org.infinispan.configuration.cache.JMXStatisticsConfiguration;
import org.infinispan.configuration.cache.LockingConfiguration;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.distribution.group.Grouper;
import org.infinispan.eviction.EvictionStrategy;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Builds a cache configuration from its components.
 * @author Paul Ferraro
 */
public class CacheConfigurationBuilder extends CapabilityServiceNameProvider implements ResourceServiceBuilder<Configuration>, Consumer<ConfigurationBuilder> {

    private final ValueDependency<MemoryConfiguration> memory;
    private final ValueDependency<ExpirationConfiguration> expiration;
    private final ValueDependency<LockingConfiguration> locking;
    private final ValueDependency<PersistenceConfiguration> persistence;
    private final ValueDependency<TransactionConfiguration> transaction;
    private final ValueDependency<Module> module;
    private final String containerName;
    private final String cacheName;

    private volatile Builder<Configuration> builder;
    private volatile JMXStatisticsConfiguration statistics;

    CacheConfigurationBuilder(PathAddress address) {
        super(CONFIGURATION, address);
        this.containerName = address.getParent().getLastElement().getValue();
        this.cacheName = address.getLastElement().getValue();
        this.memory = new InjectedValueDependency<>(CacheComponent.MEMORY.getServiceName(address), MemoryConfiguration.class);
        this.expiration = new InjectedValueDependency<>(CacheComponent.EXPIRATION.getServiceName(address), ExpirationConfiguration.class);
        this.locking = new InjectedValueDependency<>(CacheComponent.LOCKING.getServiceName(address), LockingConfiguration.class);
        this.persistence = new InjectedValueDependency<>(CacheComponent.PERSISTENCE.getServiceName(address), PersistenceConfiguration.class);
        this.transaction = new InjectedValueDependency<>(CacheComponent.TRANSACTION.getServiceName(address), TransactionConfiguration.class);
        this.module = new InjectedValueDependency<>(CacheComponent.MODULE.getServiceName(address), Module.class);
    }

    @Override
    public ServiceBuilder<Configuration> build(ServiceTarget target) {
        ServiceBuilder<Configuration> builder = this.builder.build(target);
        return new CompositeDependency(this.memory, this.expiration, this.locking, this.persistence, this.transaction, this.module).register(builder);
    }

    @Override
    public Builder<Configuration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        boolean enabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
        this.statistics = new ConfigurationBuilder().jmxStatistics().enabled(enabled).available(enabled).create();

        this.builder = new org.wildfly.clustering.infinispan.spi.service.ConfigurationBuilder(CONFIGURATION.getServiceName(context.getCurrentAddress()), this.containerName, this.cacheName, this.andThen(builder -> {
            GroupsConfigurationBuilder groupsBuilder = builder.clustering().hash().groups().enabled();
            for (Grouper<?> grouper : this.module.getValue().loadService(Grouper.class)) {
                groupsBuilder.addGrouper(grouper);
            }
        })).configure(context);
        return this;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void accept(ConfigurationBuilder builder) {
        builder.memory().read(this.memory.getValue());
        builder.expiration().read(this.expiration.getValue());
        builder.locking().read(this.locking.getValue());
        builder.persistence().read(this.persistence.getValue());
        builder.transaction().read(this.transaction.getValue());
        builder.jmxStatistics().read(this.statistics);
        // Still need to specify this to silence log messages
        builder.eviction().strategy(EvictionStrategy.MANUAL);
    }
}
