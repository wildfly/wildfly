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

import java.util.ServiceLoader;
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
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;
import org.wildfly.clustering.service.Builder;

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
    private final InjectedValue<GlobalConfiguration> global = new InjectedValue<>();
    private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();

    private final String containerName;
    private final String cacheName;

    private volatile JMXStatisticsConfiguration statistics;
    private volatile ModuleIdentifier module;

    CacheConfigurationBuilder(String containerName, String cacheName) {
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public ServiceName getServiceName() {
        return CacheServiceName.CONFIGURATION.getServiceName(this.containerName, this.cacheName);
    }

    @Override
    public ServiceBuilder<Configuration> build(ServiceTarget target) {
        return new org.wildfly.clustering.infinispan.spi.service.ConfigurationBuilder(this.containerName, this.cacheName, this.andThen(builder -> {
            CacheMode mode = builder.clustering().cacheMode();

            if (mode.isSynchronous() && (this.transaction.getValue().lockingMode() == LockingMode.OPTIMISTIC) && (this.locking.getValue().isolationLevel() == IsolationLevel.REPEATABLE_READ)) {
                builder.locking().writeSkewCheck(true);
                builder.versioning().enable().scheme(VersioningScheme.SIMPLE);
            }

            GroupsConfigurationBuilder groupsBuilder = builder.clustering().hash().groups().enabled();
            ServiceLoader.load(Grouper.class, this.getClassLoader()).forEach(grouper -> groupsBuilder.addGrouper(grouper));
        })).build(target)
                .addDependency(CacheComponent.EVICTION.getServiceName(this.containerName, this.cacheName), EvictionConfiguration.class, this.eviction)
                .addDependency(CacheComponent.EXPIRATION.getServiceName(this.containerName, this.cacheName), ExpirationConfiguration.class, this.expiration)
                .addDependency(CacheComponent.LOCKING.getServiceName(this.containerName, this.cacheName), LockingConfiguration.class, this.locking)
                .addDependency(CacheComponent.PERSISTENCE.getServiceName(this.containerName, this.cacheName), PersistenceConfiguration.class, this.persistence)
                .addDependency(CacheComponent.TRANSACTION.getServiceName(this.containerName, this.cacheName), TransactionConfiguration.class, this.transaction)
                .addDependency(CacheContainerServiceName.CONFIGURATION.getServiceName(this.containerName), GlobalConfiguration.class, this.global)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, this.loader)
                ;
    }

    @Override
    public Builder<Configuration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.module = ModelNodes.asModuleIdentifier(MODULE.getDefinition().resolveModelAttribute(context, model));

        boolean enabled = STATISTICS_ENABLED.getDefinition().resolveModelAttribute(context, model).asBoolean();
        this.statistics = new ConfigurationBuilder().jmxStatistics().enabled(enabled).available(enabled).create();

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

    private ClassLoader getClassLoader() {
        if (this.module != null) {
            try {
                return this.loader.getValue().loadModule(this.module).getClassLoader();
            } catch (ModuleLoadException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return this.global.getValue().classLoader();
    }
}
