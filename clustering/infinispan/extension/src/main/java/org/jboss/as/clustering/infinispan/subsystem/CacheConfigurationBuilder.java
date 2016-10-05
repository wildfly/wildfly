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

import java.util.function.Consumer;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.EvictionConfiguration;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.GroupsConfigurationBuilder;
import org.infinispan.configuration.cache.JMXStatisticsConfiguration;
import org.infinispan.configuration.cache.LockingConfiguration;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.configuration.cache.VersioningScheme;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.distribution.group.Grouper;
import org.infinispan.transaction.LockingMode;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Builds a cache configuration from its components.
 * @author Paul Ferraro
 */
public class CacheConfigurationBuilder implements ResourceServiceBuilder<Configuration>, Consumer<ConfigurationBuilder> {

    private final InjectedValue<EvictionConfiguration> eviction = new InjectedValue<>();
    private final InjectedValue<ExpirationConfiguration> expiration = new InjectedValue<>();
    private final InjectedValue<LockingConfiguration> locking = new InjectedValue<>();
    private final InjectedValue<PersistenceConfiguration> persistence = new InjectedValue<>();
    private final InjectedValue<TransactionConfiguration> transaction = new InjectedValue<>();
    private final InjectedValue<Module> module = new InjectedValue<>();

    private final PathAddress address;
    private final String containerName;
    private final String cacheName;

    private volatile Builder<Configuration> builder;
    private volatile ValueDependency<GlobalConfiguration> global;
    private volatile JMXStatisticsConfiguration statistics;

    CacheConfigurationBuilder(PathAddress address) {
        this.address = address;
        this.containerName = address.getParent().getLastElement().getValue();
        this.cacheName = address.getLastElement().getValue();
    }

    @Override
    public ServiceName getServiceName() {
        return CacheResourceDefinition.Capability.CONFIGURATION.getServiceName(this.address);
    }

    @Override
    public ServiceBuilder<Configuration> build(ServiceTarget target) {
        ServiceBuilder<Configuration> builder = this.builder.build(target)
                .addDependency(CacheComponent.EVICTION.getServiceName(this.address), EvictionConfiguration.class, this.eviction)
                .addDependency(CacheComponent.EXPIRATION.getServiceName(this.address), ExpirationConfiguration.class, this.expiration)
                .addDependency(CacheComponent.LOCKING.getServiceName(this.address), LockingConfiguration.class, this.locking)
                .addDependency(CacheComponent.PERSISTENCE.getServiceName(this.address), PersistenceConfiguration.class, this.persistence)
                .addDependency(CacheComponent.TRANSACTION.getServiceName(this.address), TransactionConfiguration.class, this.transaction)
                .addDependency(CacheComponent.MODULE.getServiceName(this.address), Module.class, this.module)
                .setInitialMode(ServiceController.Mode.PASSIVE)
                ;
        return this.global.register(builder);
    }

    @Override
    public Builder<Configuration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        boolean enabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
        this.statistics = new ConfigurationBuilder().jmxStatistics().enabled(enabled).available(enabled).create();

        this.global = new InjectedValueDependency<>(InfinispanRequirement.CONFIGURATION.getServiceName(context, this.containerName), GlobalConfiguration.class);
        this.builder = new org.wildfly.clustering.infinispan.spi.service.ConfigurationBuilder(InfinispanCacheRequirement.CONFIGURATION.getServiceName(context, this.containerName, this.cacheName), this.containerName, this.cacheName, this.andThen(builder -> {
            CacheMode mode = builder.clustering().cacheMode();

            if (mode.isSynchronous() && (this.transaction.getValue().lockingMode() == LockingMode.OPTIMISTIC) && (this.locking.getValue().isolationLevel() == IsolationLevel.REPEATABLE_READ)) {
                builder.locking().writeSkewCheck(true);
                builder.versioning().enable().scheme(VersioningScheme.SIMPLE);
            }

            GroupsConfigurationBuilder groupsBuilder = builder.clustering().hash().groups().enabled();
            this.module.getValue().loadService(Grouper.class).forEach(grouper -> groupsBuilder.addGrouper(grouper));
        })).configure(context);
        return this;
    }

    @Override
    public void accept(ConfigurationBuilder builder) {
        builder.eviction().read(this.eviction.getValue());
        builder.expiration().read(this.expiration.getValue());
        builder.locking().read(this.locking.getValue());
        builder.persistence().read(this.persistence.getValue());
        builder.transaction().read(this.transaction.getValue());
        builder.jmxStatistics().read(this.statistics);
    }
}
