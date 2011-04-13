/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import java.security.AccessController;
import java.util.AbstractMap;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.infinispan.config.CacheLoaderManagerConfig;
import org.infinispan.config.Configuration;
import org.infinispan.config.Configuration.CacheMode;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.config.InfinispanConfiguration;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.CacheStoreConfig;
import org.infinispan.manager.CacheContainer;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeOperationContext;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.util.loading.ContextClassLoaderSwitcher;
import org.jboss.util.loading.ContextClassLoaderSwitcher.SwitchContext;

/**
 * @author Paul Ferraro
 */
public class CacheContainerAdd implements ModelAddOperationHandler, DescriptionProvider {

    private static final String DEFAULTS = "infinispan-defaults.xml";
    private static final Logger log = Logger.getLogger(CacheContainerAdd.class.getPackage().getName());

    static ModelNode createOperation(ModelNode address, ModelNode existing) {
        ModelNode operation = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
        populate(existing, operation);
        return operation;
    }

    private static void populate(ModelNode source, ModelNode target) {
        target.get(ModelKeys.DEFAULT_CACHE).set(source.require(ModelKeys.DEFAULT_CACHE));
        if (source.hasDefined(ModelKeys.LISTENER_EXECUTOR)) {
            target.get(ModelKeys.LISTENER_EXECUTOR).set(source.get(ModelKeys.LISTENER_EXECUTOR));
        }
        if (source.hasDefined(ModelKeys.EVICTION_EXECUTOR)) {
            target.get(ModelKeys.EVICTION_EXECUTOR).set(source.get(ModelKeys.EVICTION_EXECUTOR));
        }
        if (source.hasDefined(ModelKeys.REPLICATION_QUEUE_EXECUTOR)) {
            target.get(ModelKeys.REPLICATION_QUEUE_EXECUTOR).set(source.get(ModelKeys.REPLICATION_QUEUE_EXECUTOR));
        }
        if (source.hasDefined(ModelKeys.ALIAS)) {
            ModelNode aliases = target.get(ModelKeys.ALIAS);
            for (ModelNode alias: source.get(ModelKeys.ALIAS).asList()) {
                aliases.add(alias);
            }
        }
        if (source.hasDefined(ModelKeys.TRANSPORT)) {
            target.get(ModelKeys.TRANSPORT).set(source.get(ModelKeys.TRANSPORT));
        }
        ModelNode caches = target.get(ModelKeys.CACHE);
        for (ModelNode cache: source.require(ModelKeys.CACHE).asList()) {
            caches.add(cache);
        }
    }

    @SuppressWarnings("unchecked")
    final ContextClassLoaderSwitcher switcher = (ContextClassLoaderSwitcher) AccessController.doPrivileged(ContextClassLoaderSwitcher.INSTANTIATOR);

    final GlobalConfiguration global;
    final Configuration defaultConfig;
    final Map<CacheMode, Configuration> configs;

    public CacheContainerAdd() {
        InfinispanConfiguration config = load(DEFAULTS);
        this.global = config.parseGlobalConfiguration();
        this.configs = new EnumMap<CacheMode, Configuration>(CacheMode.class);
        this.defaultConfig = config.parseDefaultConfiguration();
        Map<String, Configuration> namedConfigs = config.parseNamedConfigurations();
        for (CacheMode mode: CacheMode.values()) {
            Configuration configuration = this.defaultConfig.clone();
            Configuration override = namedConfigs.get(mode.name());
            if (override != null) {
                configuration.applyOverrides(override);
            }
            configuration.setCacheMode(mode);
            this.configs.put(mode, configuration);
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.descriptions.DescriptionProvider#getModelDescription(java.util.Locale)
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return LocalDescriptions.getCacheContainerAddDescription(locale);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.ModelAddOperationHandler#execute(org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode, org.jboss.as.controller.ResultHandler)
     */
    @Override
    public OperationResult execute(OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
        ModelNode opAddr = operation.require(ModelDescriptionConstants.OP_ADDR);

        final ModelNode removeOperation = Util.getResourceRemoveOperation(opAddr);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();

        populate(operation, context.getSubModel());

        RuntimeOperationContext runtime = context.getRuntimeContext();
        if (runtime != null) {
            RuntimeTask task = new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    String defaultCache = operation.require(ModelKeys.DEFAULT_CACHE).asString();
                    Set<String> aliases = new HashSet<String>();
                    if (operation.hasDefined(ModelKeys.ALIAS)) {
                        for (ModelNode alias: operation.get(ModelKeys.ALIAS).asList()) {
                            aliases.add(alias.asString());
                        }
                    }
                    GlobalConfiguration global = CacheContainerAdd.this.global.clone();
                    String transportExecutor = null;
                    if (operation.hasDefined(ModelKeys.TRANSPORT)) {
                        ModelNode transport = operation.get(ModelKeys.TRANSPORT);
                        if (transport.hasDefined(ModelKeys.EXECUTOR)) {
                            transportExecutor = transport.get(ModelKeys.EXECUTOR).asString();
                        }
                        if (transport.hasDefined(ModelKeys.LOCK_TIMEOUT)) {
                            global.setDistributedSyncTimeout(transport.get(ModelKeys.LOCK_TIMEOUT).asLong());
                        }
                        if (transport.hasDefined(ModelKeys.SITE)) {
                            global.setSiteId(transport.get(ModelKeys.SITE).asString());
                        }
                        if (transport.hasDefined(ModelKeys.RACK)) {
                            global.setRackId(transport.get(ModelKeys.RACK).asString());
                        }
                        if (transport.hasDefined(ModelKeys.MACHINE)) {
                            global.setMachineId(transport.get(ModelKeys.MACHINE).asString());
                        }
                    }
                    List<Map.Entry<String, Injector<String>>> locationInjectors = new LinkedList<Map.Entry<String, Injector<String>>>();
                    Map<String, Configuration> configs = new LinkedHashMap<String, Configuration>();
                    for (ModelNode cache: operation.require(ModelKeys.CACHE).asList()) {
                        String cacheName = cache.require(ModelKeys.NAME).asString();
                        CacheMode mode = CacheMode.valueOf(cache.require(ModelKeys.MODE).asString());
                        Configuration config = CacheContainerAdd.this.configs.get(mode).clone();
                        if (cache.hasDefined(ModelKeys.QUEUE_SIZE)) {
                            int queueSize = cache.get(ModelKeys.QUEUE_SIZE).asInt();
                            config.setUseReplQueue(queueSize > 0);
                            config.setReplQueueMaxElements(queueSize);
                        }
                        if (cache.hasDefined(ModelKeys.QUEUE_FLUSH_INTERVAL)) {
                            config.setReplQueueInterval(cache.get(ModelKeys.QUEUE_FLUSH_INTERVAL).asLong());
                        }
                        if (cache.hasDefined(ModelKeys.REMOTE_TIMEOUT)) {
                            config.setSyncReplTimeout(cache.get(ModelKeys.REMOTE_TIMEOUT).asLong());
                        }
                        if (cache.hasDefined(ModelKeys.OWNERS)) {
                            config.setNumOwners(cache.get(ModelKeys.OWNERS).asInt());
                        }
                        if (cache.hasDefined(ModelKeys.L1_LIFESPAN)) {
                            long lifespan = cache.get(ModelKeys.L1_LIFESPAN).asLong();
                            config.setL1CacheEnabled(lifespan > 0);
                            config.setL1Lifespan(lifespan);
                        }
                        if (cache.hasDefined(ModelKeys.LOCKING)) {
                            ModelNode locking = cache.get(ModelKeys.LOCKING);
                            if (locking.hasDefined(ModelKeys.ISOLATION)) {
                                IsolationLevel level = IsolationLevel.valueOf(locking.get(ModelKeys.ISOLATION).asString());
                                config.setIsolationLevel(level);
                            }
                            if (locking.hasDefined(ModelKeys.STRIPING)) {
                                config.setUseLockStriping(locking.get(ModelKeys.STRIPING).asBoolean());
                            }
                            if (locking.hasDefined(ModelKeys.ACQUIRE_TIMEOUT)) {
                                config.setLockAcquisitionTimeout(locking.get(ModelKeys.ACQUIRE_TIMEOUT).asLong());
                            }
                            if (locking.hasDefined(ModelKeys.CONCURRENCY_LEVEL)) {
                                config.setConcurrencyLevel(locking.get(ModelKeys.CONCURRENCY_LEVEL).asInt());
                            }
                        }
                        if (cache.hasDefined(ModelKeys.TRANSACTION)) {
                            ModelNode transaction = cache.get(ModelKeys.TRANSACTION);
                            if (transaction.hasDefined(ModelKeys.STOP_TIMEOUT)) {
                                config.setCacheStopTimeout(transaction.get(ModelKeys.TIMEOUT).asInt());
                            }
                            if (transaction.hasDefined(ModelKeys.SYNC_PHASE)) {
                                SyncPhase phase = SyncPhase.valueOf(transaction.get(ModelKeys.SYNC_PHASE).asString());
                                config.setSyncCommitPhase(phase.isCommit());
                                config.setSyncRollbackPhase(phase.isRollback());
                            }
                            if (transaction.hasDefined(ModelKeys.EAGER_LOCKING)) {
                                EagerLocking eager = EagerLocking.valueOf(transaction.get(ModelKeys.EAGER_LOCKING).asString());
                                config.setUseEagerLocking(eager.isEnabled());
                                config.setEagerLockSingleNode(eager.isSingleOwner());
                            }
                        }
                        if (cache.hasDefined(ModelKeys.EVICTION)) {
                            ModelNode eviction = cache.get(ModelKeys.EVICTION);
                            if (eviction.hasDefined(ModelKeys.STRATEGY)) {
                                EvictionStrategy strategy = EvictionStrategy.valueOf(eviction.get(ModelKeys.STRATEGY).asString());
                                config.setEvictionStrategy(strategy);
                            }
                            if (eviction.hasDefined(ModelKeys.MAX_ENTRIES)) {
                                config.setEvictionMaxEntries(eviction.get(ModelKeys.MAX_ENTRIES).asInt());
                            }
                            if (eviction.hasDefined(ModelKeys.INTERVAL)) {
                                config.setEvictionWakeUpInterval(eviction.get(ModelKeys.INTERVAL).asLong());
                            }
                        }
                        if (cache.hasDefined(ModelKeys.EXPIRATION)) {
                            ModelNode expiration = cache.get(ModelKeys.EXPIRATION);
                            if (expiration.hasDefined(ModelKeys.MAX_IDLE)) {
                                config.setExpirationMaxIdle(expiration.get(ModelKeys.MAX_IDLE).asLong());
                            }
                            if (expiration.hasDefined(ModelKeys.LIFESPAN)) {
                                config.setExpirationLifespan(expiration.get(ModelKeys.LIFESPAN).asLong());
                            }
                        }
                        if (cache.hasDefined(ModelKeys.STATE_TRANSFER)) {
                            ModelNode stateTransfer = cache.get(ModelKeys.STATE_TRANSFER);
                            if (stateTransfer.hasDefined(ModelKeys.ENABLED)) {
                                config.setFetchInMemoryState(stateTransfer.get(ModelKeys.ENABLED).asBoolean());
                            }
                            if (stateTransfer.hasDefined(ModelKeys.TIMEOUT)) {
                                config.setStateRetrievalTimeout(stateTransfer.get(ModelKeys.TIMEOUT).asLong());
                            }
                            if (stateTransfer.hasDefined(ModelKeys.FLUSH_TIMEOUT)) {
                                config.setStateRetrievalLogFlushTimeout(stateTransfer.get(ModelKeys.FLUSH_TIMEOUT).asLong());
                            }
                        }
                        if (cache.hasDefined(ModelKeys.REHASHING)) {
                            ModelNode rehashing = cache.get(ModelKeys.REHASHING);
                            if (rehashing.hasDefined(ModelKeys.ENABLED)) {
                                config.setRehashEnabled(rehashing.get(ModelKeys.ENABLED).asBoolean());
                            }
                            if (rehashing.hasDefined(ModelKeys.TIMEOUT)) {
                                config.setRehashRpcTimeout(rehashing.get(ModelKeys.TIMEOUT).asLong());
                            }
                        }
                        if (cache.hasDefined(ModelKeys.STORE)) {
                            ModelNode store = cache.get(ModelKeys.STORE);
                            CacheLoaderManagerConfig storeManagerConfig = config.getCacheLoaderManagerConfig();
                            storeManagerConfig.setShared(store.hasDefined(ModelKeys.SHARED) ? store.get(ModelKeys.SHARED).asBoolean() : false);
                            storeManagerConfig.setPreload(store.hasDefined(ModelKeys.PRELOAD) ? store.get(ModelKeys.PRELOAD).asBoolean() : false);
                            storeManagerConfig.setPassivation(store.hasDefined(ModelKeys.PASSIVATION) ? store.get(ModelKeys.PASSIVATION).asBoolean() : true);
                            CacheStoreConfig storeConfig = this.createCacheLoaderConfig(store, locationInjectors);
                            storeConfig.getSingletonStoreConfig().setSingletonStoreEnabled(store.hasDefined(ModelKeys.SINGLETON) ? store.get(ModelKeys.SINGLETON).asBoolean() : false);
                            storeConfig.setFetchPersistentState(store.hasDefined(ModelKeys.FETCH_STATE) ? store.get(ModelKeys.FETCH_STATE).asBoolean() : true);
                            storeConfig.setPurgeOnStartup(store.hasDefined(ModelKeys.PURGE) ? store.get(ModelKeys.PURGE).asBoolean() : true);
                            if (store.hasDefined(ModelKeys.PROPERTY) && (storeConfig instanceof AbstractCacheStoreConfig)) {
                                Properties properties = new Properties();
                                for (Property property: store.get(ModelKeys.PROPERTY).asPropertyList()) {
                                    properties.setProperty(property.getName(), property.getValue().asString());
                                }
                                ((AbstractCacheStoreConfig) storeConfig).setProperties(properties);
                            }
                            storeManagerConfig.addCacheLoaderConfig(storeConfig);
                        }
                        configs.put(cacheName, config);
                    }
                    if (!configs.containsKey(defaultCache)) {
                        throw new IllegalArgumentException(String.format("%s is not a valid default cache. The %s cache container does not contain a cache with that name", defaultCache, name));
                    }
                    EmbeddedCacheManagerService service = new EmbeddedCacheManagerService(name, defaultCache, aliases, global, CacheContainerAdd.this.defaultConfig.clone(), configs);
                    ServiceBuilder<CacheContainer> builder = service.build(context.getServiceTarget());
                    if (operation.hasDefined(ModelKeys.LISTENER_EXECUTOR)) {
                        service.addListenerExecutorDependency(builder, operation.get(ModelKeys.LISTENER_EXECUTOR).asString());
                    }
                    if (operation.hasDefined(ModelKeys.EVICTION_EXECUTOR)) {
                        service.addEvictionExecutorDependency(builder, operation.get(ModelKeys.EVICTION_EXECUTOR).asString());
                    }
                    if (operation.hasDefined(ModelKeys.REPLICATION_QUEUE_EXECUTOR)) {
                        service.addReplicationQueueExecutorDependency(builder, operation.get(ModelKeys.REPLICATION_QUEUE_EXECUTOR).asString());
                    }
                    if (transportExecutor != null) {
                        service.addTransportExecutorDependency(builder, transportExecutor);
                    }
                    for (Map.Entry<String, Injector<String>> injector: locationInjectors) {
                        builder.addDependency(AbstractPathService.pathNameOf(injector.getKey()), String.class, injector.getValue());
                    }
                    builder.addListener(new ResultHandler.ServiceStartListener(resultHandler));
                    builder.install();
                }

                private CacheStoreConfig createCacheLoaderConfig(ModelNode store, List<Map.Entry<String, Injector<String>>> injectors) {
                    if (store.hasDefined(ModelKeys.CLASS)) {
                        String className = store.get(ModelKeys.CLASS).asString();
                        try {
                            CacheStore cacheStore = Class.forName(className).asSubclass(CacheStore.class).newInstance();
                            return cacheStore.getConfigurationClass().asSubclass(CacheStoreConfig.class).newInstance();
                        } catch (Exception e) {
                            throw new IllegalArgumentException(String.format("%s is not a valid cache store", className), e);
                        }
                    }
                    // If no class, we assume it's a file cache store
                    String relativeTo = ServerEnvironment.SERVER_DATA_DIR;
                    if (store.hasDefined(ModelKeys.RELATIVE_TO)) {
                        relativeTo = store.get(ModelKeys.RELATIVE_TO).asString();
                    }
                    FileCacheStoreConfig storeConfig = new FileCacheStoreConfig();
                    injectors.add(new AbstractMap.SimpleImmutableEntry<String, Injector<String>>(relativeTo, storeConfig.getRelativeToInjector()));
                    storeConfig.setPath(store.hasDefined(ModelKeys.PATH) ? store.get(ModelKeys.PATH).asString() : name);
                    return storeConfig;
                }
            };
            runtime.setRuntimeTask(task);
        } else {
            resultHandler.handleResultComplete();
        }

        return new BasicOperationResult(removeOperation);
    }

    private InfinispanConfiguration load(String resource) {
        URL url = InfinispanExtension.class.getClassLoader().getResource(resource);
        if (url == null) {
            throw new IllegalStateException(String.format("Failed to locate %s", resource));
        }
        try {
            InputStream input = url.openStream();
            SwitchContext context = this.switcher.getSwitchContext(InfinispanConfiguration.class.getClassLoader());
            try {
                return InfinispanConfiguration.newInfinispanConfiguration(input);
            } finally {
                context.reset();
                try {
                    input.close();
                } catch (IOException e) {
                    log.warn(e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Failed to parse %s", url), e);
        }
    }
}
