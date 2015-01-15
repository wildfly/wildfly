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

import java.io.File;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.ServiceLoader;

import org.infinispan.Cache;
import org.infinispan.commons.util.TypedProperties;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.infinispan.configuration.cache.VersioningScheme;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.persistence.jdbc.DatabaseType;
import org.infinispan.persistence.jdbc.configuration.AbstractJdbcStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcBinaryStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcMixedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfigurationBuilder;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.infinispan.transaction.LockingMode;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceNameFactory;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupBuilderProvider;
import org.wildfly.clustering.spi.ClusteredCacheGroupBuilderProvider;
import org.wildfly.clustering.spi.LocalCacheGroupBuilderProvider;

/**
 * Base class for cache add handlers
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheAddHandler extends AbstractAddStepHandler {

    private static final Logger log = Logger.getLogger(CacheAddHandler.class.getPackage().getName());
    private static final ModuleIdentifier QUERY_MODULE = ModuleIdentifier.fromString("org.infinispan.query");

    final CacheMode mode;

    CacheAddHandler(CacheMode mode) {
        super(CacheResourceDefinition.ATTRIBUTES);
        this.mode = mode;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        // Because we use child resources in a read-only manner to configure the cache, replace the local model with the full model
        ModelNode cacheModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        // we also need the containerModel
        PathAddress address = context.getCurrentAddress();
        PathAddress containerAddress = address.subAddress(0, address.size() - 1);
        ModelNode containerModel = context.readResourceFromRoot(containerAddress).getModel();

        this.installRuntimeServices(context, operation, containerModel, cacheModel);
    }

    void installRuntimeServices(OperationContext context, ModelNode operation, ModelNode containerModel, ModelNode cacheModel) throws OperationFailedException {
        PathAddress address = Operations.getPathAddress(operation);
        String containerName = address.getElement(address.size() - 2).getValue();
        String cacheName = address.getElement(address.size() - 1).getValue();

        // get model attributes
        String jndiName = ModelNodes.asString(CacheResourceDefinition.JNDI_NAME.resolveModelAttribute(context, cacheModel));
        ServiceController.Mode initialMode = StartMode.valueOf(CacheResourceDefinition.START.resolveModelAttribute(context, cacheModel).asString()).getMode();

        ModuleIdentifier module = ModelNodes.asModuleIdentifier(CacheResourceDefinition.MODULE.resolveModelAttribute(context, cacheModel));
        if ((module == null) && Index.valueOf(CacheResourceDefinition.INDEXING.resolveModelAttribute(context, cacheModel).asString()).isEnabled()) {
            module = QUERY_MODULE;
        }

        // create a list for dependencies which may need to be added during processing
        AdvancedCacheConfigurationBuilder builder = new AdvancedCacheConfigurationBuilder(containerName, cacheName, this.mode, module);

        // process cache configuration ModelNode describing overrides to defaults
        processModelNode(context, containerName, containerModel, cacheModel, builder);

        ServiceTarget target = context.getServiceTarget();

        // Install cache configuration service
        builder.build(target).install();

        // Install cache service
        new CacheBuilder<>(containerName, cacheName).build(target).setInitialMode(initialMode).install();

        // Install jndi binding for cache
        @SuppressWarnings("rawtypes")
        BinderServiceBuilder<Cache> bindingBuilder = new BinderServiceBuilder<>(InfinispanBindingFactory.createCacheBinding(containerName, cacheName), CacheServiceName.CACHE.getServiceName(containerName, cacheName), Cache.class);
        if (jndiName != null) {
            bindingBuilder.alias(ContextNames.bindInfoFor(JndiNameFactory.parse(jndiName).getAbsoluteName()));
        }
        bindingBuilder.build(target).install();

        new XAResourceRecoveryBuilder(containerName, cacheName).build(target).install();

        Class<? extends CacheGroupBuilderProvider> providerClass = this.mode.isClustered() ? ClusteredCacheGroupBuilderProvider.class : LocalCacheGroupBuilderProvider.class;
        for (CacheGroupBuilderProvider provider : ServiceLoader.load(providerClass, providerClass.getClassLoader())) {
            log.debugf("Installing %s for cache %s of container %s", provider.getClass().getSimpleName(), cacheName, containerName);
            for (Builder<?> groupBuilder : provider.getBuilders(containerName, cacheName)) {
                groupBuilder.build(target).install();
            }
        }
    }

    void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode containerModel, ModelNode cacheModel) {
        PathAddress address = Operations.getPathAddress(operation);
        String containerName = address.getElement(address.size() - 2).getValue();
        String cacheName = address.getElement(address.size() - 1).getValue();

        // remove all services started by CacheAdd, in reverse order
        // remove the binder service
        context.removeService(InfinispanBindingFactory.createCacheBinding(containerName, cacheName).getBinderServiceName());

        for (CacheServiceNameFactory factory : CacheServiceName.values()) {
            context.removeService(factory.getServiceName(containerName, cacheName));
        }

        Class<? extends CacheGroupBuilderProvider> installerClass = this.mode.isClustered() ? ClusteredCacheGroupBuilderProvider.class : LocalCacheGroupBuilderProvider.class;
        for (CacheGroupBuilderProvider provider : ServiceLoader.load(installerClass, installerClass.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(containerName, cacheName)) {
                context.removeService(builder.getServiceName());
            }
        }

        log.debugf("cache %s removed for container %s", cacheName, containerName);
    }

    /**
     * Create a Configuration object initialized from the operation ModelNode
     *
     * @param containerName  the name of the cache container
     * @param containerModel ModelNode representing cache container configuration
     * @param cache          ModelNode representing cache configuration
     * @param builder        {@link ConfigurationBuilder} object to add data to
     */
    void processModelNode(OperationContext context, String containerName, ModelNode containerModel, ModelNode cache, AdvancedCacheConfigurationBuilder configBuilder) throws OperationFailedException {
        ConfigurationBuilder builder = configBuilder.getConfigurationBuilder();
        if (cache.hasDefined(CacheResourceDefinition.STATISTICS_ENABLED.getName())) {
            // If the cache explicitly defines statistics-enabled, disregard cache container configuration for this cache
            builder.jmxStatistics().enabled(CacheResourceDefinition.STATISTICS_ENABLED.resolveModelAttribute(context, cache).asBoolean());
        } else {
            // Otherwise default to cache container configuration
            builder.jmxStatistics().enabled(CacheContainerResourceDefinition.STATISTICS_ENABLED.resolveModelAttribute(context, containerModel).asBoolean());
        }

        final Index indexing = Index.valueOf(CacheResourceDefinition.INDEXING.resolveModelAttribute(context, cache).asString());

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
                .index(indexing)
                .withProperties(indexingProperties)
        ;

        IsolationLevel isolationLevel = IsolationLevel.valueOf(LockingResourceDefinition.ISOLATION.getDefaultValue().asString());
        // locking is a child resource
        if (cache.hasDefined(LockingResourceDefinition.PATH.getKey()) && cache.get(LockingResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
            ModelNode locking = cache.get(LockingResourceDefinition.PATH.getKeyValuePair());

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

        if (cache.hasDefined(TransactionResourceDefinition.PATH.getKey())) {
            ModelNode transaction = cache.get(TransactionResourceDefinition.PATH.getKeyValuePair());
            if (transaction.isDefined()) {
                long stopTimeout = TransactionResourceDefinition.STOP_TIMEOUT.resolveModelAttribute(context, transaction).asLong();
                TransactionMode txMode = TransactionMode.valueOf(TransactionResourceDefinition.MODE.resolveModelAttribute(context, transaction).asString());
                LockingMode lockingMode = LockingMode.valueOf(TransactionResourceDefinition.LOCKING.resolveModelAttribute(context, transaction).asString());

                configBuilder.setTransactionMode(txMode);
                builder.transaction()
                        .cacheStopTimeout(stopTimeout)
                        .transactionMode((txMode == TransactionMode.NONE) ? org.infinispan.transaction.TransactionMode.NON_TRANSACTIONAL : org.infinispan.transaction.TransactionMode.TRANSACTIONAL)
                        .lockingMode(lockingMode)
                        .useSynchronization(txMode == TransactionMode.NON_XA)
                        .recovery().enabled(txMode == TransactionMode.FULL_XA)
                        .invocationBatching().disable()
                ;

                if ((lockingMode == LockingMode.OPTIMISTIC) && (isolationLevel == IsolationLevel.REPEATABLE_READ) && this.mode.isSynchronous() && !this.mode.isInvalidation()) {
                    builder.locking().writeSkewCheck(true);
                    builder.versioning().enable().scheme(VersioningScheme.SIMPLE);
                }
            }
        }

        if (cache.hasDefined(EvictionResourceDefinition.PATH.getKey())) {
            ModelNode eviction = cache.get(EvictionResourceDefinition.PATH.getKeyValuePair());
            if (eviction.isDefined()) {
                final EvictionStrategy strategy = EvictionStrategy.valueOf(EvictionResourceDefinition.STRATEGY.resolveModelAttribute(context, eviction).asString());
                builder.eviction().strategy(strategy);

                if (strategy.isEnabled()) {
                    final int maxEntries = EvictionResourceDefinition.MAX_ENTRIES.resolveModelAttribute(context, eviction).asInt();
                    builder.eviction().maxEntries(maxEntries);
                }
            }
        }

        if (cache.hasDefined(ExpirationResourceDefinition.PATH.getKey())) {
            ModelNode expiration = cache.get(ExpirationResourceDefinition.PATH.getKeyValuePair());
            if (expiration.isDefined()) {
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
        }

        StoreType type = findStoreType(cache);
        if (type != null) {
            ModelNode store = cache.get(type.pathElement().getKeyValuePair());
            if (store.isDefined()) {
                PersistenceConfigurationBuilder persistenceBuilder = builder.persistence()
                        .passivation(StoreResourceDefinition.PASSIVATION.resolveModelAttribute(context, store).asBoolean())
                ;
                StoreConfigurationBuilder<?, ?> storeBuilder = buildCacheStore(context, persistenceBuilder, containerName, type, store, configBuilder)
                        .fetchPersistentState(StoreResourceDefinition.FETCH_STATE.resolveModelAttribute(context, store).asBoolean())
                        .preload(StoreResourceDefinition.PRELOAD.resolveModelAttribute(context, store).asBoolean())
                        .shared(StoreResourceDefinition.SHARED.resolveModelAttribute(context, store).asBoolean())
                        .purgeOnStartup(StoreResourceDefinition.PURGE.resolveModelAttribute(context, store).asBoolean())
                ;
                storeBuilder.singleton().enabled(StoreResourceDefinition.SINGLETON.resolveModelAttribute(context, store).asBoolean());

                if (store.hasDefined(StoreWriteBehindResourceDefinition.PATH.getKey())) {
                    ModelNode writeBehind = store.get(StoreWriteBehindResourceDefinition.PATH.getKeyValuePair());
                    if (writeBehind.isDefined()) {
                        storeBuilder.async().enable()
                                .flushLockTimeout(StoreWriteBehindResourceDefinition.FLUSH_LOCK_TIMEOUT.resolveModelAttribute(context, writeBehind).asLong())
                                .modificationQueueSize(StoreWriteBehindResourceDefinition.MODIFICATION_QUEUE_SIZE.resolveModelAttribute(context, writeBehind).asInt())
                                .shutdownTimeout(StoreWriteBehindResourceDefinition.SHUTDOWN_TIMEOUT.resolveModelAttribute(context, writeBehind).asLong())
                                .threadPoolSize(StoreWriteBehindResourceDefinition.THREAD_POOL_SIZE.resolveModelAttribute(context, writeBehind).asInt())
                        ;
                    }
                }

                Properties properties = new TypedProperties();
                if (store.hasDefined(StorePropertyResourceDefinition.WILDCARD_PATH.getKey())) {
                    for (Property property : store.get(StorePropertyResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                        properties.setProperty(property.getName(), StorePropertyResourceDefinition.VALUE.resolveModelAttribute(context, property.getValue()).asString());
                    }
                }
                storeBuilder.withProperties(properties);
            }
        }
    }

    private static StoreType findStoreType(ModelNode cache) {
        for (StoreType store: StoreType.values()) {
            if (cache.hasDefined(store.pathElement().getKey())) {
                return store;
            }
        }
        return null;
    }

    private static StoreConfigurationBuilder<?, ?> buildCacheStore(OperationContext context, PersistenceConfigurationBuilder persistenceBuilder, String containerName, StoreType storeType, ModelNode store, AdvancedCacheConfigurationBuilder configBuilder) throws OperationFailedException {
        switch (storeType) {
            case FILE: {
                final SingleFileStoreConfigurationBuilder builder = persistenceBuilder.addSingleFileStore();
                final String path = ModelNodes.asString(FileStoreResourceDefinition.RELATIVE_PATH.resolveModelAttribute(context, store), InfinispanExtension.SUBSYSTEM_NAME + File.separatorChar + containerName);
                final String relativeTo = FileStoreResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, store).asString();
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
                configBuilder.addDependency(PathManagerService.SERVICE_NAME, PathManager.class, injector);
                return builder;
            }
            case STRING_KEYED_JDBC:
            case BINARY_KEYED_JDBC:
            case MIXED_KEYED_JDBC: {
                DatabaseType dialect = ModelNodes.asEnum(JDBCStoreResourceDefinition.DIALECT.resolveModelAttribute(context, store), DatabaseType.class);

                AbstractJdbcStoreConfigurationBuilder<?, ?> builder = buildJdbcStore(persistenceBuilder, context, store).dialect(dialect);

                String datasource = JDBCStoreResourceDefinition.DATA_SOURCE.resolveModelAttribute(context, store).asString();

                configBuilder.addDependency(ServiceName.JBOSS.append("data-source", datasource));
                builder.dataSource().jndiUrl(datasource);
                return builder;
            }
            case REMOTE: {
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
                    configBuilder.addDependency(OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(outboundSocketBinding), OutboundSocketBinding.class, injector);
                }
                builder.remoteCacheName(RemoteStoreResourceDefinition.CACHE.resolveModelAttribute(context, store).asString());
                builder.socketTimeout(RemoteStoreResourceDefinition.SOCKET_TIMEOUT.resolveModelAttribute(context, store).asLong());
                builder.tcpNoDelay(RemoteStoreResourceDefinition.TCP_NO_DELAY.resolveModelAttribute(context, store).asBoolean());
                return builder;
            }
            case CUSTOM: {
                String className = store.require(ModelKeys.CLASS).asString();
                try {
                    return persistenceBuilder.addStore(StoreConfigurationBuilder.class.getClassLoader().loadClass(className).asSubclass(StoreConfigurationBuilder.class));
                } catch (Exception e) {
                    throw InfinispanLogger.ROOT_LOGGER.invalidCacheStore(e, className);
                }
            }
            default: {
                throw new IllegalStateException();
            }
        }
    }

    private static AbstractJdbcStoreConfigurationBuilder<?, ?> buildJdbcStore(PersistenceConfigurationBuilder persistenceBuilder, OperationContext context, ModelNode store) throws OperationFailedException {
        boolean useStringKeyedTable = store.hasDefined(JDBCStoreResourceDefinition.STRING_KEYED_TABLE.getName());
        boolean useBinaryKeyedTable = store.hasDefined(JDBCStoreResourceDefinition.BINARY_KEYED_TABLE.getName());
        if (useStringKeyedTable && !useBinaryKeyedTable) {
            JdbcStringBasedStoreConfigurationBuilder builder = persistenceBuilder.addStore(JdbcStringBasedStoreConfigurationBuilder.class);
            buildStringKeyedTable(builder.table(), context, store.get(JDBCStoreResourceDefinition.STRING_KEYED_TABLE.getName()));
            return builder;
        } else if (useBinaryKeyedTable && !useStringKeyedTable) {
            JdbcBinaryStoreConfigurationBuilder builder = persistenceBuilder.addStore(JdbcBinaryStoreConfigurationBuilder.class);
            buildBinaryKeyedTable(builder.table(), context, store.get(JDBCStoreResourceDefinition.BINARY_KEYED_TABLE.getName()));
            return builder;
        }
        // Else, use mixed mode
        JdbcMixedStoreConfigurationBuilder builder = persistenceBuilder.addStore(JdbcMixedStoreConfigurationBuilder.class);
        buildStringKeyedTable(builder.stringTable(), context, store.get(JDBCStoreResourceDefinition.STRING_KEYED_TABLE.getName()));
        buildBinaryKeyedTable(builder.binaryTable(), context, store.get(JDBCStoreResourceDefinition.BINARY_KEYED_TABLE.getName()));
        return builder;
    }

    private static void buildBinaryKeyedTable(TableManipulationConfigurationBuilder<?, ?> builder, OperationContext context, ModelNode table) throws OperationFailedException {
        buildTable(builder, context, table, "ispn_bucket");
    }

    private static void buildStringKeyedTable(TableManipulationConfigurationBuilder<?, ?> builder, OperationContext context, ModelNode table) throws OperationFailedException {
        buildTable(builder, context, table, "ispn_entry");
    }

    private static void buildTable(TableManipulationConfigurationBuilder<?, ?> builder, OperationContext context, ModelNode table, String defaultTableNamePrefix) throws OperationFailedException {
        builder.batchSize(JDBCStoreResourceDefinition.BATCH_SIZE.resolveModelAttribute(context, table).asInt())
                .fetchSize(JDBCStoreResourceDefinition.FETCH_SIZE.resolveModelAttribute(context, table).asInt())
                .tableNamePrefix(ModelNodes.asString(JDBCStoreResourceDefinition.PREFIX.resolveModelAttribute(context, table), defaultTableNamePrefix))
                .idColumnName(getColumnProperty(context, table, JDBCStoreResourceDefinition.ID_COLUMN, JDBCStoreResourceDefinition.COLUMN_NAME, "id"))
                .idColumnType(getColumnProperty(context, table, JDBCStoreResourceDefinition.ID_COLUMN, JDBCStoreResourceDefinition.COLUMN_TYPE, "VARCHAR"))
                .dataColumnName(getColumnProperty(context, table, JDBCStoreResourceDefinition.DATA_COLUMN, JDBCStoreResourceDefinition.COLUMN_NAME, "datum"))
                .dataColumnType(getColumnProperty(context, table, JDBCStoreResourceDefinition.DATA_COLUMN, JDBCStoreResourceDefinition.COLUMN_TYPE, "BINARY"))
                .timestampColumnName(getColumnProperty(context, table, JDBCStoreResourceDefinition.TIMESTAMP_COLUMN, JDBCStoreResourceDefinition.COLUMN_NAME, "version"))
                .timestampColumnType(getColumnProperty(context, table, JDBCStoreResourceDefinition.TIMESTAMP_COLUMN, JDBCStoreResourceDefinition.COLUMN_TYPE, "BIGINT"))
        ;
    }

    private static String getColumnProperty(OperationContext context, ModelNode table, AttributeDefinition columnResourceDefinition, AttributeDefinition columnAttribute, String defaultValue) throws OperationFailedException {
        if (!table.isDefined() || !table.hasDefined(columnResourceDefinition.getName())) return defaultValue;
        return ModelNodes.asString(columnAttribute.resolveModelAttribute(context, table.get(columnResourceDefinition.getName())), defaultValue);
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
}
