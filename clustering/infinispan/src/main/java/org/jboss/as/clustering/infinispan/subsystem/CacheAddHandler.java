/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.infinispan.Cache;
import org.infinispan.commons.util.TypedProperties;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.infinispan.configuration.cache.VersioningScheme;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.jdbc.DatabaseType;
import org.infinispan.persistence.jdbc.configuration.AbstractJdbcStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcBinaryStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcMixedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfigurationBuilder;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.Services;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.XAResourceRecoveryRegistry;

/**
 * Base class for cache add handlers
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public abstract class CacheAddHandler extends AbstractAddStepHandler {

    private static final Logger log = Logger.getLogger(CacheAddHandler.class.getPackage().getName());
    private static final String DEFAULTS = "infinispan-defaults.xml";
    private static final String QUERY_MODULE = "org.infinispan.query";
    private static volatile Map<CacheMode, Configuration> defaults = null;

    public static synchronized Configuration getDefaultConfiguration(CacheMode cacheMode) {
        if (defaults == null) {
            ConfigurationBuilderHolder holder = load(DEFAULTS);
            Configuration defaultConfig = holder.getDefaultConfigurationBuilder().build();
            Map<CacheMode, Configuration> map = new EnumMap<>(CacheMode.class);
            map.put(defaultConfig.clustering().cacheMode(), defaultConfig);
            for (ConfigurationBuilder builder : holder.getNamedConfigurationBuilders().values()) {
                Configuration config = builder.build();
                map.put(config.clustering().cacheMode(), config);
            }
            for (CacheMode mode : CacheMode.values()) {
                if (!map.containsKey(mode)) {
                    map.put(mode, new ConfigurationBuilder().read(defaultConfig).clustering().cacheMode(mode).build());
                }
            }
            defaults = map;
        }
        return defaults.get(cacheMode);
    }

    private static ConfigurationBuilderHolder load(String resource) {
        URL url = find(resource, CacheAddHandler.class.getClassLoader());
        log.debugf("Loading Infinispan defaults from %s", url.toString());
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
        throw new IllegalArgumentException(String.format("Failed to locate %s", resource));
    }

    final CacheMode mode;

    CacheAddHandler(CacheMode mode) {
        this.mode = mode;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attribute: CacheResourceDefinition.ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        // Because we use child resources in a read-only manner to configure the cache, replace the local model with the full model
        ModelNode cacheModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        // we also need the containerModel
        PathAddress containerAddress = getCacheContainerAddressFromOperation(operation);
        ModelNode containerModel = context.readResourceFromRoot(containerAddress).getModel();

        // install the services from a reusable method
        newControllers.addAll(this.installRuntimeServices(context, operation, containerModel, cacheModel, verificationHandler));
    }

    Collection<ServiceController<?>> installRuntimeServices(OperationContext context, ModelNode operation, ModelNode containerModel, ModelNode cacheModel, ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        // get all required addresses, names and service names
        PathAddress cacheAddress = getCacheAddressFromOperation(operation);
        PathAddress containerAddress = getCacheContainerAddressFromOperation(operation);
        String cacheName = cacheAddress.getLastElement().getValue();
        String containerName = containerAddress.getLastElement().getValue();

        // get model attributes
        ModelNode resolvedValue = null;
        String jndiName = (resolvedValue = CacheResourceDefinition.JNDI_NAME.resolveModelAttribute(context, cacheModel)).isDefined() ? resolvedValue.asString() : null;
        ServiceController.Mode initialMode = StartMode.valueOf(CacheResourceDefinition.START.resolveModelAttribute(context, cacheModel).asString()).getMode();

        String module = (resolvedValue = CacheResourceDefinition.MODULE.resolveModelAttribute(context, cacheModel)).isDefined() ? resolvedValue.asString() : (Indexing.valueOf(CacheResourceDefinition.INDEXING.resolveModelAttribute(context, cacheModel).asString()).isEnabled() ? QUERY_MODULE : null);
        ModuleIdentifier moduleId = (module != null) ? ModuleIdentifier.fromString(module) : null;

        // create a list for dependencies which may need to be added during processing
        List<Dependency<?>> dependencies = new LinkedList<>();
        // Infinispan Configuration to hold the operation data
        ConfigurationBuilder builder = new ConfigurationBuilder().read(getDefaultConfiguration(this.mode));
        CacheConfigurationDependencies cacheConfigurationDependencies = new CacheConfigurationDependencies(this.mode, builder, moduleId);
        CacheDependencies cacheDependencies = new CacheDependencies();

        // process cache configuration ModelNode describing overrides to defaults
        processModelNode(context, containerName, containerModel, cacheModel, builder, cacheConfigurationDependencies, cacheDependencies, dependencies);

        // get container Model to pick up the value of the default cache of the container
        // AS7-3488 make default-cache no required attribute
        String defaultCacheName = CacheContainerResourceDefinition.DEFAULT_CACHE.resolveModelAttribute(context, containerModel).asString();
        boolean defaultCache = cacheName.equals(defaultCacheName);

        ServiceTarget target = context.getServiceTarget();

        Collection<ServiceController<?>> controllers = new ArrayList<>(3);

        // install the cache configuration service (configures a cache)
        controllers.add(this.installCacheConfigurationService(target, containerName, cacheName, defaultCache, cacheConfigurationDependencies, dependencies, verificationHandler));
        log.debugf("Cache configuration service for %s installed for container %s", cacheName, containerName);

        // now install the corresponding cache service (starts a configured cache)
        controllers.add(this.installCacheService(target, containerName, cacheName, defaultCache, initialMode, cacheDependencies, verificationHandler));

        // install a name service entry for the cache
        controllers.add(this.installJndiService(target, containerName, cacheName, defaultCache, jndiName, verificationHandler));
        log.debugf("Cache service for cache %s installed for container %s", cacheName, containerName);

        return controllers;
    }

    void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode containerModel, ModelNode cacheModel) throws OperationFailedException {
        // get container and cache addresses
        final PathAddress cacheAddress = getCacheAddressFromOperation(operation);
        final PathAddress containerAddress = getCacheContainerAddressFromOperation(operation);

        // get container and cache names
        final String cacheName = cacheAddress.getLastElement().getValue();
        final String containerName = containerAddress.getLastElement().getValue();

        // remove all services started by CacheAdd, in reverse order
        // remove the binder service
        ModelNode resolvedValue = null;
        final String jndiName = (resolvedValue = CacheResourceDefinition.JNDI_NAME.resolveModelAttribute(context, cacheModel)).isDefined() ? resolvedValue.asString() : null;

        ContextNames.BindInfo binding = createCacheBinding((jndiName != null) ? JndiNameFactory.parse(jndiName) : createJndiName(containerName, cacheName));
        context.removeService(binding.getBinderServiceName());
        // remove the CacheService instance
        context.removeService(CacheService.getServiceName(containerName, cacheName));
        // remove the cache configuration service
        context.removeService(CacheConfigurationService.getServiceName(containerName, cacheName));

        log.debugf("cache %s removed for container %s", cacheName, containerName);
    }

    protected PathAddress getCacheAddressFromOperation(ModelNode operation) {
        return PathAddress.pathAddress(operation.get(OP_ADDR));
    }

    protected PathAddress getCacheContainerAddressFromOperation(ModelNode operation) {
        final PathAddress cacheAddress = getCacheAddressFromOperation(operation);
        final PathAddress containerAddress = cacheAddress.subAddress(0, cacheAddress.size()-1);
        return containerAddress;
    }

    ServiceController<?> installCacheConfigurationService(ServiceTarget target, String containerName, String cacheName, boolean defaultCache,
            CacheConfigurationDependencies dependencies, List<Dependency<?>> dependencyList, ServiceVerificationHandler verificationHandler) {

        Service<Configuration> service = new CacheConfigurationService(cacheName, dependencies);
        ServiceBuilder<?> configBuilder = AsynchronousService.addService(target, CacheConfigurationService.getServiceName(containerName, cacheName), service)
                .addDependency(EmbeddedCacheManagerService.getServiceName(containerName), EmbeddedCacheManager.class, dependencies.getCacheContainerInjector())
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, dependencies.getModuleLoaderInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
        ;

        // add in any additional dependencies resulting from ModelNode parsing
        for (Dependency<?> dependency : dependencyList) {
            addDependency(configBuilder, dependency);
        }
        // add an alias for the default cache
        if (defaultCache) {
            configBuilder.addAliases(CacheConfigurationService.getServiceName(containerName, null));
        }
        return configBuilder.install();
    }

    ServiceController<?> installCacheService(ServiceTarget target, String containerName, String cacheName, boolean defaultCache, ServiceController.Mode initialMode,
            CacheDependencies cacheDependencies, ServiceVerificationHandler verificationHandler) {

        Service<Cache<Object, Object>> service = new CacheService<>(cacheName, cacheDependencies);
        ServiceBuilder<?> builder = AsynchronousService.addService(target, CacheService.getServiceName(containerName, cacheName), service)
                .addDependency(GlobalComponentRegistryService.getServiceName(containerName))
                .addDependency(CacheConfigurationService.getServiceName(containerName, cacheName))
                .addDependency(EmbeddedCacheManagerService.getServiceName(containerName), EmbeddedCacheManager.class, cacheDependencies.getCacheContainerInjector())
                .setInitialMode(initialMode)
        ;

        // add an alias for the default cache
        if (defaultCache) {
            builder.addAliases(CacheService.getServiceName(containerName, null));
        }

        if (initialMode == ServiceController.Mode.ACTIVE) {
            builder.addListener(verificationHandler);
        }

        return builder.install();
    }

    ServiceController<?> installJndiService(ServiceTarget target, String containerName, String cacheName, boolean defaultCache, String jndiName, ServiceVerificationHandler verificationHandler) {

        ServiceName cacheServiceName = CacheService.getServiceName(containerName, cacheName);
        ContextNames.BindInfo binding = createCacheBinding((jndiName != null) ? JndiNameFactory.parse(jndiName) : createJndiName(containerName, cacheName));
        ServiceBuilder<ManagedReferenceFactory> builder = new BinderServiceBuilder(target).build(binding, cacheServiceName, Cache.class);
        if (defaultCache) {
            ContextNames.BindInfo defaultBinding = createCacheBinding(createJndiName(containerName, CacheContainer.DEFAULT_CACHE_ALIAS));
            builder.addAliases(defaultBinding.getBinderServiceName(), ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(defaultBinding.getBindName()));
        }
        return builder.install();
    }

    private static JndiName createJndiName(String container, String cache) {
        return JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, InfinispanExtension.SUBSYSTEM_NAME, "cache", container, cache);
    }

    private static ContextNames.BindInfo createCacheBinding(JndiName name) {
        return ContextNames.bindInfoFor(name.getAbsoluteName());
    }

    private static <T> void addDependency(ServiceBuilder<?> builder, Dependency<T> dependency) {
        final ServiceName name = dependency.getName();
        final Injector<T> injector = dependency.getInjector();
        if (injector != null) {
            builder.addDependency(name, dependency.getType(), injector);
        } else {
            builder.addDependency(name);
        }
    }

    /**
     * Create a Configuration object initialized from the operation ModelNode
     *
     * @param containerName  the name of the cache container
     * @param containerModel ModelNode representing cache container configuration
     * @param cache          ModelNode representing cache configuration
     * @param builder        {@link ConfigurationBuilder} object to add data to
     */
    void processModelNode(OperationContext context, String containerName, ModelNode containerModel, ModelNode cache, ConfigurationBuilder builder, CacheConfigurationDependencies cacheConfigurationDependencies, CacheDependencies cacheDependencies, List<Dependency<?>> dependencies) throws OperationFailedException {
        if (cache.hasDefined(ModelKeys.STATISTICS_ENABLED)) {
            // If the cache explicitly defines statistics-enabled, disregard cache container configuration for this cache
            builder.jmxStatistics().enabled(CacheResourceDefinition.STATISTICS_ENABLED.resolveModelAttribute(context, cache).asBoolean());
        } else {
            // Otherwise default to cache container configuration
            builder.jmxStatistics().enabled(CacheContainerResourceDefinition.STATISTICS_ENABLED.resolveModelAttribute(context, containerModel).asBoolean());
        }

        final Indexing indexing = Indexing.valueOf(CacheResourceDefinition.INDEXING.resolveModelAttribute(context, cache).asString());

        // set the cache mode (may be modified when setting up clustering attributes)
        builder.clustering().cacheMode(this.mode);
        final ModelNode indexingPropertiesModel = CacheResourceDefinition.INDEXING_PROPERTIES.resolveModelAttribute(context, cache);
        Properties indexingProperties = new Properties();
        if (indexing.isEnabled() && indexingPropertiesModel.isDefined()) {
            for (Property p : indexingPropertiesModel.asPropertyList()) {
                String value = p.getValue().asString();
                indexingProperties.put(p.getName(), value);
            }
        }
        builder.indexing()
                .enabled(indexing.isEnabled())
                .indexLocalOnly(indexing.isLocalOnly())
                .withProperties(indexingProperties)
        ;

        IsolationLevel isolationLevel = getDefaultConfiguration(this.mode).locking().isolationLevel();
        // locking is a child resource
        if (cache.hasDefined(ModelKeys.LOCKING) && cache.get(ModelKeys.LOCKING, ModelKeys.LOCKING_NAME).isDefined()) {
            ModelNode locking = cache.get(ModelKeys.LOCKING, ModelKeys.LOCKING_NAME);

            isolationLevel = IsolationLevel.valueOf(LockingResourceDefinition.ISOLATION.resolveModelAttribute(context, locking).asString());
            final boolean striping = LockingResourceDefinition.STRIPING.resolveModelAttribute(context, locking).asBoolean();
            final long acquireTimeout = LockingResourceDefinition.ACQUIRE_TIMEOUT.resolveModelAttribute(context, locking).asLong();
            final int concurrencyLevel = LockingResourceDefinition.CONCURRENCY_LEVEL.resolveModelAttribute(context, locking).asInt();

            builder.locking()
                    .isolationLevel(isolationLevel)
                    .useLockStriping(striping)
                    .lockAcquisitionTimeout(acquireTimeout)
                    .concurrencyLevel(concurrencyLevel)
            ;
        }

        LockingMode lockingMode = getDefaultConfiguration(this.mode).transaction().lockingMode();
        // transaction is a child resource
        if (cache.hasDefined(ModelKeys.TRANSACTION) && cache.get(ModelKeys.TRANSACTION, ModelKeys.TRANSACTION_NAME).isDefined()) {
            ModelNode transaction = cache.get(ModelKeys.TRANSACTION, ModelKeys.TRANSACTION_NAME);

            long stopTimeout = TransactionResourceDefinition.STOP_TIMEOUT.resolveModelAttribute(context, transaction).asLong();
            TransactionMode txMode = TransactionMode.valueOf(TransactionResourceDefinition.MODE.resolveModelAttribute(context, transaction).asString());
            lockingMode = LockingMode.valueOf(TransactionResourceDefinition.LOCKING.resolveModelAttribute(context, transaction).asString());
            boolean transactional = (txMode != TransactionMode.NONE);
            boolean batching = (txMode == TransactionMode.BATCH);
            boolean useSynchronization = (txMode == TransactionMode.NON_XA);
            boolean recoveryEnabled = (txMode == TransactionMode.FULL_XA);

            builder.transaction()
                    .cacheStopTimeout(stopTimeout)
                    .transactionMode(transactional ? org.infinispan.transaction.TransactionMode.TRANSACTIONAL : org.infinispan.transaction.TransactionMode.NON_TRANSACTIONAL)
                    .lockingMode(lockingMode)
                    .useSynchronization(useSynchronization)
                    .recovery().enabled(recoveryEnabled)
                    .invocationBatching().enable(transactional)
            ;
            if (transactional) {
                if (batching) {
                    cacheConfigurationDependencies.getTransactionManagerInjector().inject(BatchModeTransactionManager.getInstance());
                } else {
                    dependencies.add(new Dependency<>(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, cacheConfigurationDependencies.getTransactionManagerInjector()));
                    if (useSynchronization) {
                        dependencies.add(new Dependency<>(TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, cacheConfigurationDependencies.getTransactionSynchronizationRegistryInjector()));
                    } else if (recoveryEnabled) {
                        dependencies.add(new Dependency<>(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, XAResourceRecoveryRegistry.class, cacheDependencies.getRecoveryRegistryInjector()));
                    }
                }
            }
        }

        if ((lockingMode == LockingMode.OPTIMISTIC) && (isolationLevel == IsolationLevel.REPEATABLE_READ) && this.mode.isSynchronous() && !this.mode.isInvalidation()) {
            builder.locking().writeSkewCheck(true);
            builder.versioning().enable().scheme(VersioningScheme.SIMPLE);
        }

        // eviction is a child resource
        if (cache.hasDefined(ModelKeys.EVICTION) && cache.get(ModelKeys.EVICTION, ModelKeys.EVICTION_NAME).isDefined()) {
            ModelNode eviction = cache.get(ModelKeys.EVICTION, ModelKeys.EVICTION_NAME);

            final EvictionStrategy strategy = EvictionStrategy.valueOf(EvictionResourceDefinition.STRATEGY.resolveModelAttribute(context, eviction).asString());
            builder.eviction().strategy(strategy);

            if (strategy.isEnabled()) {
                final int maxEntries = EvictionResourceDefinition.MAX_ENTRIES.resolveModelAttribute(context, eviction).asInt();
                builder.eviction().maxEntries(maxEntries);
            }
        }
        // expiration is a child resource
        if (cache.hasDefined(ModelKeys.EXPIRATION) && cache.get(ModelKeys.EXPIRATION, ModelKeys.EXPIRATION_NAME).isDefined()) {

            ModelNode expiration = cache.get(ModelKeys.EXPIRATION, ModelKeys.EXPIRATION_NAME);

            final long maxIdle = ExpirationResourceDefinition.MAX_IDLE.resolveModelAttribute(context, expiration).asLong();
            final long lifespan = ExpirationResourceDefinition.LIFESPAN.resolveModelAttribute(context, expiration).asLong();
            final long interval = ExpirationResourceDefinition.INTERVAL.resolveModelAttribute(context, expiration).asLong();

            builder.expiration()
                    .maxIdle(maxIdle)
                    .lifespan(lifespan)
                    .wakeUpInterval(interval)
            ;
            // Only enable the reaper thread if we need it
            if ((maxIdle > 0) || (lifespan > 0)) {
                builder.expiration().enableReaper();
            } else {
                builder.expiration().disableReaper();
            }
        }

        // stores are a child resource
        String storeKey = findStoreKey(cache);
        if (storeKey != null) {
            ModelNode store = getStoreModelNode(cache);

            final boolean shared = StoreResourceDefinition.SHARED.resolveModelAttribute(context, store).asBoolean();
            final boolean preload = StoreResourceDefinition.PRELOAD.resolveModelAttribute(context, store).asBoolean();
            final boolean passivation = StoreResourceDefinition.PASSIVATION.resolveModelAttribute(context, store).asBoolean();
            final boolean fetchState = StoreResourceDefinition.FETCH_STATE.resolveModelAttribute(context, store).asBoolean();
            final boolean purge = StoreResourceDefinition.PURGE.resolveModelAttribute(context, store).asBoolean();
            final boolean singleton = StoreResourceDefinition.SINGLETON.resolveModelAttribute(context, store).asBoolean();
            // TODO Fix me
            final boolean async = store.hasDefined(ModelKeys.WRITE_BEHIND) && store.get(ModelKeys.WRITE_BEHIND, ModelKeys.WRITE_BEHIND_NAME).isDefined();

            PersistenceConfigurationBuilder persistenceBuilder = builder.persistence()
                    .passivation(passivation)
            ;
            StoreConfigurationBuilder<?, ?> storeBuilder = this.buildCacheStore(context, persistenceBuilder, containerName, store, storeKey, dependencies)
                    .fetchPersistentState(fetchState)
                    .preload(preload)
                    .shared(shared)
                    .purgeOnStartup(purge)
            ;
            storeBuilder.singleton().enabled(singleton);

            if (async) {
                ModelNode writeBehind = store.get(ModelKeys.WRITE_BEHIND, ModelKeys.WRITE_BEHIND_NAME);
                storeBuilder.async().enable()
                        .flushLockTimeout(StoreWriteBehindResourceDefinition.FLUSH_LOCK_TIMEOUT.resolveModelAttribute(context, writeBehind).asLong())
                        .modificationQueueSize(StoreWriteBehindResourceDefinition.MODIFICATION_QUEUE_SIZE.resolveModelAttribute(context, writeBehind).asInt())
                        .shutdownTimeout(StoreWriteBehindResourceDefinition.SHUTDOWN_TIMEOUT.resolveModelAttribute(context, writeBehind).asLong())
                        .threadPoolSize(StoreWriteBehindResourceDefinition.THREAD_POOL_SIZE.resolveModelAttribute(context, writeBehind).asInt())
                ;
            }

            final Properties properties = new TypedProperties();
            if (store.hasDefined(ModelKeys.PROPERTY)) {
                for (Property property : store.get(ModelKeys.PROPERTY).asPropertyList()) {
                    // format of properties
                    // "property" => {
                    //   "property-name" => {"value => "property-value"}
                    // }
                    String propertyName = property.getName();
                    // get the value from ModelNode {"value" => "property-value"}
                    ModelNode propertyValue = null;
                    propertyValue = StorePropertyResourceDefinition.VALUE.resolveModelAttribute(context,property.getValue());
                    properties.setProperty(propertyName, propertyValue.asString());
                }
            }
            storeBuilder.withProperties(properties);
        }
    }

    private static String findStoreKey(ModelNode cache) {
        if (cache.hasDefined(ModelKeys.STORE)) {
            return ModelKeys.STORE;
        } else if (cache.hasDefined(ModelKeys.FILE_STORE)) {
            return ModelKeys.FILE_STORE;
        } else if (cache.hasDefined(ModelKeys.STRING_KEYED_JDBC_STORE)) {
            return ModelKeys.STRING_KEYED_JDBC_STORE;
        } else if (cache.hasDefined(ModelKeys.BINARY_KEYED_JDBC_STORE)) {
            return ModelKeys.BINARY_KEYED_JDBC_STORE;
        } else if (cache.hasDefined(ModelKeys.MIXED_KEYED_JDBC_STORE)) {
            return ModelKeys.MIXED_KEYED_JDBC_STORE;
        } else if (cache.hasDefined(ModelKeys.REMOTE_STORE)) {
            return ModelKeys.REMOTE_STORE;
        }
        return null;
    }

    private static ModelNode getStoreModelNode(ModelNode cache) {
        if (cache.hasDefined(ModelKeys.STORE)) {
            return cache.get(ModelKeys.STORE, ModelKeys.STORE_NAME);
        } else if (cache.hasDefined(ModelKeys.FILE_STORE)) {
            return cache.get(ModelKeys.FILE_STORE, ModelKeys.FILE_STORE_NAME);
        } else if (cache.hasDefined(ModelKeys.STRING_KEYED_JDBC_STORE)) {
            return cache.get(ModelKeys.STRING_KEYED_JDBC_STORE, ModelKeys.STRING_KEYED_JDBC_STORE_NAME);
        } else if (cache.hasDefined(ModelKeys.BINARY_KEYED_JDBC_STORE)) {
            return cache.get(ModelKeys.BINARY_KEYED_JDBC_STORE, ModelKeys.BINARY_KEYED_JDBC_STORE_NAME);
        } else if (cache.hasDefined(ModelKeys.MIXED_KEYED_JDBC_STORE)) {
            return cache.get(ModelKeys.MIXED_KEYED_JDBC_STORE, ModelKeys.MIXED_KEYED_JDBC_STORE_NAME);
        } else if (cache.hasDefined(ModelKeys.REMOTE_STORE)) {
            return cache.get(ModelKeys.REMOTE_STORE, ModelKeys.REMOTE_STORE_NAME);
        }
        return null;
    }


    private StoreConfigurationBuilder<?, ?> buildCacheStore(OperationContext context, PersistenceConfigurationBuilder persistenceBuilder, String containerName, ModelNode store, String storeKey, List<Dependency<?>> dependencies) throws OperationFailedException {

        ModelNode resolvedValue = null;
        if (storeKey.equals(ModelKeys.FILE_STORE)) {
            final SingleFileStoreConfigurationBuilder builder = persistenceBuilder.addSingleFileStore();

            final String path = ((resolvedValue = FileStoreResourceDefinition.RELATIVE_PATH.resolveModelAttribute(context, store)).isDefined()) ? resolvedValue.asString() : InfinispanExtension.SUBSYSTEM_NAME + File.separatorChar + containerName;
            final String relativeTo = ((resolvedValue = FileStoreResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, store)).isDefined()) ? resolvedValue.asString() : ServerEnvironment.SERVER_DATA_DIR;
            Injector<PathManager> injector = new Injector<PathManager>() {
                private volatile PathManager.Callback.Handle callbackHandle;
                @Override
                public void inject(PathManager value) {
                    this.callbackHandle = value.registerCallback(relativeTo, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED);
                    builder.location(value.resolveRelativePathEntry(path, relativeTo));
                }

                @Override
                public void uninject() {
                    if (this.callbackHandle != null) {
                        this.callbackHandle.remove();
                    }
                }
            };
            dependencies.add(new Dependency<>(PathManagerService.SERVICE_NAME, PathManager.class, injector));
            return builder;
        } else if (storeKey.equals(ModelKeys.STRING_KEYED_JDBC_STORE) || storeKey.equals(ModelKeys.BINARY_KEYED_JDBC_STORE) || storeKey.equals(ModelKeys.MIXED_KEYED_JDBC_STORE)) {
            ModelNode dialect = JDBCStoreResourceDefinition.DIALECT.resolveModelAttribute(context, store);
            DatabaseType type = dialect.isDefined() ? DatabaseType.valueOf(dialect.asString()) : null;

            AbstractJdbcStoreConfigurationBuilder<?, ?> builder = buildJdbcStore(persistenceBuilder, context, store, type);

            String datasource = JDBCStoreResourceDefinition.DATA_SOURCE.resolveModelAttribute(context, store).asString();

            dependencies.add(new Dependency<>(ServiceName.JBOSS.append("data-source", datasource)));
            builder.dataSource().jndiUrl(datasource);
            return builder;
        } else if (storeKey.equals(ModelKeys.REMOTE_STORE)) {
            final RemoteStoreConfigurationBuilder builder = persistenceBuilder.addStore(RemoteStoreConfigurationBuilder.class);
            for (ModelNode server : store.require(ModelKeys.REMOTE_SERVERS).asList()) {
                String outboundSocketBinding = server.get(ModelKeys.OUTBOUND_SOCKET_BINDING).asString();
                Injector<OutboundSocketBinding> injector = new Injector<OutboundSocketBinding>() {
                    @Override
                    public void inject(OutboundSocketBinding value) {
                        try {
                            builder.addServer().host(value.getResolvedDestinationAddress().getHostAddress()).port(value.getDestinationPort());
                        } catch (UnknownHostException e) {
                            throw InfinispanLogger.ROOT_LOGGER.failedToInjectSocketBinding(e, value);
                        }
                    }
                    @Override
                    public void uninject() {
                        // Do nothing
                    }
                };
                dependencies.add(new Dependency<>(OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(outboundSocketBinding), OutboundSocketBinding.class, injector));
            }
            if (store.hasDefined(ModelKeys.CACHE)) {
                builder.remoteCacheName(store.get(ModelKeys.CACHE).asString());
            }
            if (store.hasDefined(ModelKeys.SOCKET_TIMEOUT)) {
                builder.socketTimeout(store.require(ModelKeys.SOCKET_TIMEOUT).asLong());
            }
            if (store.hasDefined(ModelKeys.TCP_NO_DELAY)) {
                builder.tcpNoDelay(store.require(ModelKeys.TCP_NO_DELAY).asBoolean());
            }
            return builder;
        } else {
            String className = store.require(ModelKeys.CLASS).asString();
            try {
                return persistenceBuilder.addStore(StoreConfigurationBuilder.class.getClassLoader().loadClass(className).asSubclass(StoreConfigurationBuilder.class));
            } catch (Exception e) {
                throw InfinispanLogger.ROOT_LOGGER.invalidCacheStore(e, className);
            }
        }
    }

    private static AbstractJdbcStoreConfigurationBuilder<?, ?> buildJdbcStore(PersistenceConfigurationBuilder persistenceBuilder, OperationContext context, ModelNode store, DatabaseType type) throws OperationFailedException {
        boolean useStringKeyedTable = store.hasDefined(ModelKeys.STRING_KEYED_TABLE);
        boolean useBinaryKeyedTable = store.hasDefined(ModelKeys.BINARY_KEYED_TABLE);
        if (useStringKeyedTable && !useBinaryKeyedTable) {
            JdbcStringBasedStoreConfigurationBuilder builder = persistenceBuilder.addStore(JdbcStringBasedStoreConfigurationBuilder.class);
            buildStringKeyedTable(builder.table(), context, store.get(ModelKeys.STRING_KEYED_TABLE), type);
            return builder;
        } else if (useBinaryKeyedTable && !useStringKeyedTable) {
            JdbcBinaryStoreConfigurationBuilder builder = persistenceBuilder.addStore(JdbcBinaryStoreConfigurationBuilder.class);
            buildBinaryKeyedTable(builder.table(), context, store.get(ModelKeys.BINARY_KEYED_TABLE), type);
            return builder;
        }
        // Else, use mixed mode
        JdbcMixedStoreConfigurationBuilder builder = persistenceBuilder.addStore(JdbcMixedStoreConfigurationBuilder.class);
        buildStringKeyedTable(builder.stringTable(), context, store.get(ModelKeys.STRING_KEYED_TABLE), type);
        buildBinaryKeyedTable(builder.binaryTable(), context, store.get(ModelKeys.BINARY_KEYED_TABLE), type);
        return builder;
    }

    private static void buildBinaryKeyedTable(TableManipulationConfigurationBuilder<?, ?> builder, OperationContext context, ModelNode table, DatabaseType type) throws OperationFailedException {
        buildTable(builder, context, table, type, "ispn_bucket");
    }

    private static void buildStringKeyedTable(TableManipulationConfigurationBuilder<?, ?> builder, OperationContext context, ModelNode table, DatabaseType type) throws OperationFailedException {
        buildTable(builder, context, table, type, "ispn_entry");
    }

    private static void buildTable(TableManipulationConfigurationBuilder<?, ?> builder, OperationContext context, ModelNode table, DatabaseType type, String defaultTableNamePrefix) throws OperationFailedException {
        ModelNode tableNamePrefix = JDBCStoreResourceDefinition.PREFIX.resolveModelAttribute(context, table);
        builder.databaseType(type)
                .batchSize(JDBCStoreResourceDefinition.BATCH_SIZE.resolveModelAttribute(context, table).asInt())
                .fetchSize(JDBCStoreResourceDefinition.FETCH_SIZE.resolveModelAttribute(context, table).asInt())
                .tableNamePrefix(tableNamePrefix.isDefined() ? tableNamePrefix.asString() : defaultTableNamePrefix)
                .idColumnName(getColumnProperty(context, table, ModelKeys.ID_COLUMN, JDBCStoreResourceDefinition.COLUMN_NAME, "id"))
                .idColumnType(getColumnProperty(context, table, ModelKeys.ID_COLUMN, JDBCStoreResourceDefinition.COLUMN_TYPE, "VARCHAR"))
                .dataColumnName(getColumnProperty(context, table, ModelKeys.DATA_COLUMN, JDBCStoreResourceDefinition.COLUMN_NAME, "datum"))
                .dataColumnType(getColumnProperty(context, table, ModelKeys.DATA_COLUMN, JDBCStoreResourceDefinition.COLUMN_TYPE, "BINARY"))
                .timestampColumnName(getColumnProperty(context, table, ModelKeys.TIMESTAMP_COLUMN, JDBCStoreResourceDefinition.COLUMN_NAME, "version"))
                .timestampColumnType(getColumnProperty(context, table, ModelKeys.TIMESTAMP_COLUMN, JDBCStoreResourceDefinition.COLUMN_TYPE, "BIGINT"))
        ;
    }

    private static String getColumnProperty(OperationContext context, ModelNode table, String columnKey, AttributeDefinition columnAttribute, String defaultValue) throws OperationFailedException
    {
        if (!table.isDefined() || !table.hasDefined(columnKey)) return defaultValue;
        ModelNode column = table.get(columnKey);
        ModelNode resolvedValue = null;
        return ((resolvedValue = columnAttribute.resolveModelAttribute(context, column)).isDefined()) ? resolvedValue.asString() : defaultValue;
    }

    /*
     * Allows us to store dependency requirements for later processing.
     */
    protected class Dependency<I> {
        private final ServiceName name;
        private final Class<I> type;
        private final Injector<I> target;

        Dependency(ServiceName name) {
            this(name, null, null);
        }

        Dependency(ServiceName name, Class<I> type, Injector<I> target) {
            this.name = name;
            this.type = type;
            this.target = target;
        }

        ServiceName getName() {
            return this.name;
        }

        public Class<I> getType() {
            return this.type;
        }

        public Injector<I> getInjector() {
            return this.target;
        }
    }

    static class CacheDependencies implements CacheService.Dependencies {

        private final InjectedValue<EmbeddedCacheManager> container = new InjectedValue<>();
        private final InjectedValue<XAResourceRecoveryRegistry> recoveryRegistry = new InjectedValue<>();

        Injector<EmbeddedCacheManager> getCacheContainerInjector() {
            return this.container;
        }

        Injector<XAResourceRecoveryRegistry> getRecoveryRegistryInjector() {
            return this.recoveryRegistry;
        }

        @Override
        public EmbeddedCacheManager getCacheContainer() {
            return this.container.getValue();
        }

        @Override
        public XAResourceRecoveryRegistry getRecoveryRegistry() {
            return this.recoveryRegistry.getOptionalValue();
        }
    }

    static class CacheConfigurationDependencies implements CacheConfigurationService.Dependencies {

        private final InjectedValue<EmbeddedCacheManager> container = new InjectedValue<>();
        private final InjectedValue<TransactionManager> tm = new InjectedValue<>();
        private final InjectedValue<TransactionSynchronizationRegistry> tsr = new InjectedValue<>();
        private final InjectedValue<ModuleLoader> moduleLoader = new InjectedValue<>();
        private final CacheMode mode;
        private final ModuleIdentifier moduleId;
        private final ConfigurationBuilder builder;
        private volatile ConsistentHashStrategy consistentHashStrategy = ConsistentHashStrategy.DEFAULT;

        CacheConfigurationDependencies(CacheMode mode, ConfigurationBuilder builder, ModuleIdentifier moduleId) {
            this.mode = mode;
            this.builder = builder;
            this.moduleId = moduleId;
        }

        Injector<EmbeddedCacheManager> getCacheContainerInjector() {
            return this.container;
        }

        Injector<TransactionManager> getTransactionManagerInjector() {
            return this.tm;
        }

        Injector<TransactionSynchronizationRegistry> getTransactionSynchronizationRegistryInjector() {
            return this.tsr;
        }

        Injector<ModuleLoader> getModuleLoaderInjector() {
            return this.moduleLoader;
        }

        void setConsistentHashStrategy(ConsistentHashStrategy consistentHashStrategy) {
            this.consistentHashStrategy = consistentHashStrategy;
        }

        @Override
        public CacheMode getCacheMode() {
            return this.mode;
        }

        @Override
        public ConfigurationBuilder getConfigurationBuilder() {
            return this.builder;
        }

        @Override
        public ModuleIdentifier getModuleIdentifier() {
            return this.moduleId;
        }

        @Override
        public ConsistentHashStrategy getConsistentHashStrategy() {
            return this.consistentHashStrategy;
        }

        @Override
        public EmbeddedCacheManager getCacheContainer() {
            return this.container.getValue();
        }

        @Override
        public TransactionManager getTransactionManager() {
            return this.tm.getOptionalValue();
        }

        @Override
        public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
            return this.tsr.getOptionalValue();
        }

        @Override
        public ModuleLoader getModuleLoader() {
            return this.moduleLoader.getValue();
        }
    }
}
