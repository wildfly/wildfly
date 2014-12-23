/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.GroupsConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.distribution.group.Grouper;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.tm.DummyTransactionManager;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.clustering.infinispan.TransactionManagerProvider;
import org.jboss.as.clustering.infinispan.TransactionSynchronizationRegistryProvider;
import org.jboss.as.server.Services;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.infinispan.spi.service.ConfigurationFactory;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.InjectorDependency;

/**
 * Builder for an advanced cache {@link Configuration}.
 * @author Paul Ferraro
 */
public class AdvancedCacheConfigurationBuilder implements Builder<Configuration>, ConfigurationFactory {

    private static final String DEFAULTS = "infinispan-defaults.xml";
    private static final Map<CacheMode, Configuration> DEFAULT_CONFIGURATIONS = new EnumMap<>(CacheMode.class);

    public static synchronized Configuration getDefaultConfiguration(CacheMode cacheMode) {
        if (DEFAULT_CONFIGURATIONS.isEmpty()) {
            ConfigurationBuilderHolder holder = load(DEFAULTS);
            Configuration defaultConfig = holder.getDefaultConfigurationBuilder().build();
            DEFAULT_CONFIGURATIONS.put(defaultConfig.clustering().cacheMode(), defaultConfig);
            for (ConfigurationBuilder builder : holder.getNamedConfigurationBuilders().values()) {
                Configuration config = builder.build();
                DEFAULT_CONFIGURATIONS.put(config.clustering().cacheMode(), config);
            }
            for (CacheMode mode : CacheMode.values()) {
                if (!DEFAULT_CONFIGURATIONS.containsKey(mode)) {
                    DEFAULT_CONFIGURATIONS.put(mode, new ConfigurationBuilder().read(defaultConfig).clustering().cacheMode(mode).build());
                }
            }
        }
        return DEFAULT_CONFIGURATIONS.get(cacheMode);
    }

    private static ConfigurationBuilderHolder load(String resource) {
        URL url = find(resource, CacheAddHandler.class.getClassLoader());
        ParserRegistry parser = new ParserRegistry(ParserRegistry.class.getClassLoader());
        try (InputStream input = url.openStream()) {
            return parser.parse(input);
        } catch (IOException e) {
            throw InfinispanLogger.ROOT_LOGGER.failedToParse(e, url);
        }
    }

    private static URL find(String resource, ClassLoader... loaders) {
        for (ClassLoader loader : loaders) {
            if (loader != null) {
                URL url = loader.getResource(resource);
                if (url != null) {
                    return url;
                }
            }
        }
        throw new IllegalArgumentException(resource);
    }

    private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();
    private final InjectedValue<EmbeddedCacheManager> container = new InjectedValue<>();
    private final InjectedValue<TransactionManager> tm = new InjectedValue<>();
    private final InjectedValue<TransactionSynchronizationRegistry> tsr = new InjectedValue<>();
    private final String containerName;
    private final Builder<Configuration> builder;
    private final CacheMode mode;
    private final ModuleIdentifier module;
    private final ConfigurationBuilder configurationBuilder;
    private final List<Dependency> dependencies = new LinkedList<>();
    private final List<ServiceName> names = new LinkedList<>();
    private ConsistentHashStrategy consistentHashStrategy = ConsistentHashStrategy.DEFAULT;
    private TransactionMode txMode = TransactionMode.DEFAULT;

    /**
     * Constructs a new builder for an advanced {@link Configuration}.
     */
    public AdvancedCacheConfigurationBuilder(String containerName, String cacheName, CacheMode mode, ModuleIdentifier module) {
        this.builder = new org.wildfly.clustering.infinispan.spi.service.ConfigurationBuilder(containerName, cacheName, this);
        this.containerName = containerName;
        this.configurationBuilder = new ConfigurationBuilder().read(getDefaultConfiguration(mode));
        this.mode = mode;
        this.module = module;
    }

    @Override
    public ServiceName getServiceName() {
        return this.builder.getServiceName();
    }

    @Override
    public ServiceBuilder<Configuration> build(ServiceTarget target) {
        ServiceBuilder<Configuration> builder = this.builder.build(target)
                .addDependency(CacheContainerServiceName.CACHE_CONTAINER.getServiceName(this.containerName), EmbeddedCacheManager.class, this.container)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, this.loader)
                .addDependencies(this.names);

        switch (this.txMode) {
            case NONE: {
                break;
            }
            case BATCH: {
                this.tm.inject(DummyTransactionManager.getInstance());
                break;
            }
            case NON_XA: {
                builder.addDependency(TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, this.tsr);
            }
            default: {
                builder.addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, this.tm);
            }
        }

        for (Dependency dependency : this.dependencies) {
            dependency.register(builder);
        }

        return builder.setInitialMode(ServiceController.Mode.PASSIVE);
    }

    @Override
    public Configuration createConfiguration() {
        if (this.module != null) {
            try {
                Module module = this.loader.getValue().loadModule(this.module);

                GroupsConfigurationBuilder groupsBuilder = this.configurationBuilder.clustering().hash().groups();
                for (Grouper<?> grouper: ServiceLoader.load(Grouper.class, module.getClassLoader())) {
                    groupsBuilder.addGrouper(grouper);
                }
            } catch (ModuleLoadException e) {
                throw new IllegalArgumentException(e);
            }
        }
        TransactionManager tm = this.tm.getOptionalValue();
        if (tm != null) {
            this.configurationBuilder.transaction().transactionManagerLookup(new TransactionManagerProvider(tm));
        }
        TransactionSynchronizationRegistry tsr = this.tsr.getOptionalValue();
        if (tsr != null) {
            this.configurationBuilder.transaction().transactionSynchronizationRegistryLookup(new TransactionSynchronizationRegistryProvider(tsr));
        }
        boolean topologyAware = this.container.getValue().getCacheManagerConfiguration().transport().hasTopologyInfo();
        this.consistentHashStrategy.buildHashConfiguration(this.configurationBuilder.clustering().hash(), this.mode, topologyAware);
        return this.configurationBuilder.build();
    }

    public ConfigurationBuilder getConfigurationBuilder() {
        return this.configurationBuilder;
    }

    public AdvancedCacheConfigurationBuilder setConsistentHashStrategy(ConsistentHashStrategy consistentHashStrategy) {
        this.consistentHashStrategy = consistentHashStrategy;
        return this;
    }

    public AdvancedCacheConfigurationBuilder setTransactionMode(TransactionMode txMode) {
        this.txMode = txMode;
        return this;
    }

    public AdvancedCacheConfigurationBuilder addDependency(ServiceName name) {
        this.names.add(name);
        return this;
    }

    public <T> Value<T> addDependency(ServiceName name, Class<T> targetClass) {
        InjectedValue<T> value = new InjectedValue<>();
        this.dependencies.add(new InjectorDependency<>(name, targetClass, value));
        return value;
    }

    public <T> void addDependency(ServiceName name, Class<T> targetClass, Injector<T> injector) {
        this.dependencies.add(new InjectorDependency<>(name, targetClass, injector));
    }
}
