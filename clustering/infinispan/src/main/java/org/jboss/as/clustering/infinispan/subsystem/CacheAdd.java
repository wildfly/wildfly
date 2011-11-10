package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.config.parsing.XmlConfigHelper;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.loaders.AbstractCacheLoaderConfig;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.CacheStoreConfig;
import org.infinispan.loaders.file.FileCacheStoreConfig.FsyncMode;
import org.infinispan.loaders.jdbc.AbstractJdbcCacheStoreConfig;
import org.infinispan.loaders.jdbc.TableManipulation;
import org.infinispan.loaders.jdbc.binary.JdbcBinaryCacheStoreConfig;
import org.infinispan.loaders.jdbc.connectionfactory.ManagedConnectionFactory;
import org.infinispan.loaders.jdbc.mixed.JdbcMixedCacheStoreConfig;
import org.infinispan.loaders.jdbc.stringbased.JdbcStringBasedCacheStoreConfig;
import org.infinispan.loaders.remote.RemoteCacheStoreConfig;
import org.infinispan.manager.CacheContainer;
import org.infinispan.transaction.LockingMode;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.clustering.infinispan.InfinispanMessages;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Base class for cache add handlers
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheAdd extends AbstractAddStepHandler {

    private static final Logger log = Logger.getLogger(CacheAdd.class.getPackage().getName()) ;

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
    //
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
    //
    }

    /**
     * Transfer operation ModelNode values to model ModelNode values
     *
     * @param operation
     * @param model
     * @throws OperationFailedException
     */
    void populateCacheModelNode(ModelNode operation, ModelNode model) throws OperationFailedException {

        // the name attribute is required, and can always be found from the operation address
        PathAddress cacheAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        String cacheName = cacheAddress.getLastElement().getValue();
        model.get(ModelKeys.NAME).set(cacheName);

        // if the cache type is local-cache, set the MODE (type CacheMode) here as
        // it will not be further modified
        String cacheType = cacheAddress.getLastElement().getKey();
        if (cacheType.equals((ModelKeys.LOCAL_CACHE))) {
            model.get(ModelKeys.CACHE_MODE).set(Configuration.CacheMode.LOCAL.name());
        }
        populate(operation, model) ;
    }

    /**
     * Transfer elements common to both operations and models
     *
     * @param operation
     * @param model
     */
    protected static void populate(ModelNode operation, ModelNode model) {

        if (operation.hasDefined(ModelKeys.START)) {
            model.get(ModelKeys.START).set(operation.get(ModelKeys.START)) ;
        }
        if (operation.hasDefined(ModelKeys.BATCHING)) {
            model.get(ModelKeys.BATCHING).set(operation.get(ModelKeys.BATCHING)) ;
        }
        if (operation.hasDefined(ModelKeys.INDEXING)) {
            model.get(ModelKeys.INDEXING).set(operation.get(ModelKeys.INDEXING)) ;
        }
        // child elements
        if (operation.hasDefined(ModelKeys.LOCKING)) {
            model.get(ModelKeys.LOCKING).set(operation.get(ModelKeys.LOCKING)) ;
        }
        if (operation.hasDefined(ModelKeys.TRANSACTION)) {
            model.get(ModelKeys.TRANSACTION).set(operation.get(ModelKeys.TRANSACTION)) ;
        }
        if (operation.hasDefined(ModelKeys.EVICTION)) {
            model.get(ModelKeys.EVICTION).set(operation.get(ModelKeys.EVICTION)) ;
        }
        if (operation.hasDefined(ModelKeys.EXPIRATION)) {
            model.get(ModelKeys.EXPIRATION).set(operation.get(ModelKeys.EXPIRATION)) ;
        }
        if (operation.hasDefined(ModelKeys.STORE)) {
            model.get(ModelKeys.STORE).set(operation.get(ModelKeys.STORE)) ;
        }
        if (operation.hasDefined(ModelKeys.FILE_STORE)) {
            model.get(ModelKeys.FILE_STORE).set(operation.get(ModelKeys.FILE_STORE)) ;
        }
        if (operation.hasDefined(ModelKeys.JDBC_STORE)) {
            model.get(ModelKeys.JDBC_STORE).set(operation.get(ModelKeys.JDBC_STORE)) ;
        }
        if (operation.hasDefined(ModelKeys.REMOTE_STORE)) {
            model.get(ModelKeys.REMOTE_STORE).set(operation.get(ModelKeys.REMOTE_STORE)) ;
        }
    }

    /**
     * Create a Configuration object initialized from the operation ModelNode
     *
     * @param cache ModelNode representing cache configuration
     * @param configuration Configuration object to add data to
     * @return initialised Configuration object
     */
    Configuration processCacheModelNode(ModelNode cache, Configuration configuration, List<AdditionalDependency> additionalDeps) {

        String cacheName = cache.require(ModelKeys.NAME).asString();

        configuration.setClassLoader(this.getClass().getClassLoader());
        FluentConfiguration fluent = configuration.fluent();

        // set cache mode
        Configuration.CacheMode mode = Configuration.CacheMode.valueOf(cache.require(ModelKeys.CACHE_MODE).asString());
        fluent.mode(mode);

        if (cache.hasDefined(ModelKeys.BATCHING)) {
            if (cache.get(ModelKeys.BATCHING).asBoolean()) {
                fluent.invocationBatching();
            }
        }
        if (cache.hasDefined(ModelKeys.INDEXING)) {
            Indexing indexing = Indexing.valueOf(cache.get(ModelKeys.INDEXING).asString());
            if (indexing.isEnabled()) {
                fluent.indexing().indexLocalOnly(indexing.isLocalOnly());
            }
        }
        if (cache.hasDefined(ModelKeys.QUEUE_SIZE)) {
            fluent.async().replQueueMaxElements(cache.get(ModelKeys.QUEUE_SIZE).asInt());
        }
        if (cache.hasDefined(ModelKeys.QUEUE_FLUSH_INTERVAL)) {
            fluent.async().replQueueInterval(cache.get(ModelKeys.QUEUE_FLUSH_INTERVAL).asLong());
        }
        if (cache.hasDefined(ModelKeys.REMOTE_TIMEOUT)) {
            fluent.sync().replTimeout(cache.get(ModelKeys.REMOTE_TIMEOUT).asLong());
        }
        if (cache.hasDefined(ModelKeys.OWNERS)) {
            fluent.hash().numOwners(cache.get(ModelKeys.OWNERS).asInt());
        }
        if (cache.hasDefined(ModelKeys.VIRTUAL_NODES)) {
            fluent.hash().numVirtualNodes(cache.get(ModelKeys.VIRTUAL_NODES).asInt());
        }
        if (cache.hasDefined(ModelKeys.L1_LIFESPAN)) {
            long lifespan = cache.get(ModelKeys.L1_LIFESPAN).asLong();
            if (lifespan > 0) {
                fluent.l1().lifespan(lifespan);
            } else {
                fluent.l1().disable();
            }
        }
        if (cache.hasDefined(ModelKeys.LOCKING)) {
            ModelNode locking = cache.get(ModelKeys.LOCKING);
            FluentConfiguration.LockingConfig fluentLocking = fluent.locking();
            if (locking.hasDefined(ModelKeys.ISOLATION)) {
                fluentLocking.isolationLevel(IsolationLevel.valueOf(locking.get(ModelKeys.ISOLATION).asString()));
            }
            if (locking.hasDefined(ModelKeys.STRIPING)) {
                fluentLocking.useLockStriping(locking.get(ModelKeys.STRIPING).asBoolean());
            }
            if (locking.hasDefined(ModelKeys.ACQUIRE_TIMEOUT)) {
                fluentLocking.lockAcquisitionTimeout(locking.get(ModelKeys.ACQUIRE_TIMEOUT).asLong());
            }
            if (locking.hasDefined(ModelKeys.CONCURRENCY_LEVEL)) {
                fluentLocking.concurrencyLevel(locking.get(ModelKeys.CONCURRENCY_LEVEL).asInt());
            }
        }
        FluentConfiguration.TransactionConfig fluentTx = fluent.transaction();
        TransactionMode txMode = TransactionMode.NON_XA;
        LockingMode lockingMode = LockingMode.OPTIMISTIC;
        if (cache.hasDefined(ModelKeys.TRANSACTION)) {
            ModelNode transaction = cache.get(ModelKeys.TRANSACTION);
            if (transaction.hasDefined(ModelKeys.STOP_TIMEOUT)) {
                fluentTx.cacheStopTimeout(transaction.get(ModelKeys.STOP_TIMEOUT).asInt());
            }
            if (transaction.hasDefined(ModelKeys.MODE)) {
                txMode = TransactionMode.valueOf(transaction.get(ModelKeys.MODE).asString());
            }
            if (transaction.hasDefined(ModelKeys.LOCKING)) {
                lockingMode = LockingMode.valueOf(transaction.get(ModelKeys.LOCKING).asString());
            }
        }
        fluentTx.transactionMode(txMode.getMode());
        fluentTx.lockingMode(lockingMode);
        FluentConfiguration.RecoveryConfig recovery = fluentTx.useSynchronization(!txMode.isXAEnabled()).recovery();
        if (txMode.isRecoveryEnabled()) {
            recovery.syncCommitPhase(true).syncRollbackPhase(true);
        } else {
            recovery.disable();
        }
        if (cache.hasDefined(ModelKeys.EVICTION)) {
            ModelNode eviction = cache.get(ModelKeys.EVICTION);
            FluentConfiguration.EvictionConfig fluentEviction = fluent.eviction();
            if (eviction.hasDefined(ModelKeys.STRATEGY)) {
                fluentEviction.strategy(EvictionStrategy.valueOf(eviction.get(ModelKeys.STRATEGY).asString()));
            }
            if (eviction.hasDefined(ModelKeys.MAX_ENTRIES)) {
                fluentEviction.maxEntries(eviction.get(ModelKeys.MAX_ENTRIES).asInt());
            }
        }
        if (cache.hasDefined(ModelKeys.EXPIRATION)) {
            ModelNode expiration = cache.get(ModelKeys.EXPIRATION);
            FluentConfiguration.ExpirationConfig fluentExpiration = fluent.expiration();
            if (expiration.hasDefined(ModelKeys.MAX_IDLE)) {
                fluentExpiration.maxIdle(expiration.get(ModelKeys.MAX_IDLE).asLong());
            }
            if (expiration.hasDefined(ModelKeys.LIFESPAN)) {
                fluentExpiration.lifespan(expiration.get(ModelKeys.LIFESPAN).asLong());
            }
            if (expiration.hasDefined(ModelKeys.INTERVAL)) {
                fluentExpiration.wakeUpInterval(expiration.get(ModelKeys.INTERVAL).asLong());
            }
        }
        if (cache.hasDefined(ModelKeys.STATE_TRANSFER)) {
            ModelNode stateTransfer = cache.get(ModelKeys.STATE_TRANSFER);
            FluentConfiguration.StateRetrievalConfig fluentStateTransfer = fluent.stateRetrieval();
            if (stateTransfer.hasDefined(ModelKeys.ENABLED)) {
                fluentStateTransfer.fetchInMemoryState(stateTransfer.get(ModelKeys.ENABLED).asBoolean());
            }
            if (stateTransfer.hasDefined(ModelKeys.TIMEOUT)) {
                fluentStateTransfer.timeout(stateTransfer.get(ModelKeys.TIMEOUT).asLong());
            }
            if (stateTransfer.hasDefined(ModelKeys.FLUSH_TIMEOUT)) {
                fluentStateTransfer.logFlushTimeout(stateTransfer.get(ModelKeys.FLUSH_TIMEOUT).asLong());
            }
        }
        if (cache.hasDefined(ModelKeys.REHASHING)) {
            ModelNode rehashing = cache.get(ModelKeys.REHASHING);
            FluentConfiguration.HashConfig fluentHash = fluent.hash();
            if (rehashing.hasDefined(ModelKeys.ENABLED)) {
                fluentHash.rehashEnabled(rehashing.get(ModelKeys.ENABLED).asBoolean());
            }
            if (rehashing.hasDefined(ModelKeys.TIMEOUT)) {
                fluentHash.rehashRpcTimeout(rehashing.get(ModelKeys.TIMEOUT).asLong());
            }
        }
        String storeKey = this.findStoreKey(cache);
        if (storeKey != null) {
            ModelNode store = cache.get(storeKey);
            FluentConfiguration.LoadersConfig fluentStores = fluent.loaders();
            fluentStores.shared(store.hasDefined(ModelKeys.SHARED) ? store.get(ModelKeys.SHARED).asBoolean() : false);
            fluentStores.preload(store.hasDefined(ModelKeys.PRELOAD) ? store.get(ModelKeys.PRELOAD).asBoolean() : false);
            fluentStores.passivation(store.hasDefined(ModelKeys.PASSIVATION) ? store.get(ModelKeys.PASSIVATION).asBoolean() : true);
            CacheStoreConfig storeConfig = buildCacheStore(cacheName, store, storeKey, additionalDeps);
            storeConfig.singletonStore().enabled(store.hasDefined(ModelKeys.SINGLETON) ? store.get(ModelKeys.SINGLETON).asBoolean() : false);
            storeConfig.fetchPersistentState(store.hasDefined(ModelKeys.FETCH_STATE) ? store.get(ModelKeys.FETCH_STATE).asBoolean() : true);
            storeConfig.purgeOnStartup(store.hasDefined(ModelKeys.PURGE) ? store.get(ModelKeys.PURGE).asBoolean() : true);
            fluentStores.addCacheLoader(storeConfig);
        }
        return configuration ;
    }

    private String findStoreKey(ModelNode cache) {
        if (cache.hasDefined(ModelKeys.STORE)) {
            return ModelKeys.STORE;
        } else if (cache.hasDefined(ModelKeys.FILE_STORE)) {
            return ModelKeys.FILE_STORE;
        } else if (cache.hasDefined(ModelKeys.JDBC_STORE)) {
            return ModelKeys.JDBC_STORE;
        } else if (cache.hasDefined(ModelKeys.REMOTE_STORE)) {
            return ModelKeys.REMOTE_STORE;
        }
        return null;
    }

    private CacheStoreConfig buildCacheStore(String name, ModelNode store, String storeKey, List<AdditionalDependency> additionalDeps) {
        Properties properties = new Properties();
        if (store.hasDefined(ModelKeys.PROPERTY)) {
            for (Property property : store.get(ModelKeys.PROPERTY).asPropertyList()) {
                String propertyName = property.getName();
                String propertyValue = property.getValue().asString();
                properties.setProperty(propertyName, propertyValue);
            }
        }

        if (storeKey.equals(ModelKeys.FILE_STORE)) {
            FileCacheStoreConfig storeConfig = new FileCacheStoreConfig();
            String relativeTo = store.hasDefined(ModelKeys.RELATIVE_TO) ? store.get(ModelKeys.RELATIVE_TO).asString() : ServerEnvironment.SERVER_DATA_DIR;
            // builder.addDependency(AbstractPathService.pathNameOf(relativeTo), String.class, storeConfig.getRelativeToInjector());
            AdditionalDependency<String> dep = new AdditionalDependency<String>(AbstractPathService.pathNameOf(relativeTo), String.class, storeConfig.getRelativeToInjector());
            additionalDeps.add(dep);
            storeConfig.path(store.hasDefined(ModelKeys.PATH) ? store.get(ModelKeys.PATH).asString() : name);
            storeConfig.fsyncMode(FsyncMode.PER_WRITE);
            storeConfig.setProperties(properties);
            XmlConfigHelper.setValues(storeConfig, properties, false, true);
            return storeConfig;
        } else if (storeKey.equals(ModelKeys.JDBC_STORE)) {
            AbstractJdbcCacheStoreConfig storeConfig = this.buildJDBCStoreConfig(store);
            String datasource = store.require(ModelKeys.DATASOURCE).asString();
            // builder.addDependency(ServiceName.JBOSS.append("data-source").append("reference-factory").append(datasource));
            AdditionalDependency<Object> dep = new AdditionalDependency<Object>(ServiceName.JBOSS.append("data-source").append("reference-factory").append(datasource));
            additionalDeps.add(dep);
            storeConfig.setDatasourceJndiLocation(datasource);
            storeConfig.setConnectionFactoryClass(ManagedConnectionFactory.class.getName());
            storeConfig.setProperties(properties);
            XmlConfigHelper.setValues(storeConfig, properties, false, true);
            return storeConfig;
        } else if (storeKey.equals(ModelKeys.REMOTE_STORE)) {
            final RemoteCacheStoreConfig storeConfig = new RemoteCacheStoreConfig();
            for(ModelNode server : store.require(ModelKeys.REMOTE_SERVER).asList()) {
                String outboundSocketBinding = server.get(ModelKeys.OUTBOUND_SOCKET_BINDING).asString();
                // builder.addDependency(OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(outboundSocketBinding), OutboundSocketBinding.class, new Injector<OutboundSocketBinding>() {
                AdditionalDependency<OutboundSocketBinding> dep = new AdditionalDependency<OutboundSocketBinding>(OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(outboundSocketBinding),
                        OutboundSocketBinding.class, new Injector<OutboundSocketBinding>() {
                    @Override
                    public void inject(OutboundSocketBinding value) throws InjectionException {
                        final String address;
                        try {
                            address = value.getDestinationAddress().getHostAddress()+":"+Integer.toString(value.getDestinationPort());
                        } catch (UnknownHostException uhe) {
                            throw InfinispanMessages.MESSAGES.failedToInjectSocketBinding(uhe, value);
                        }
                        String serverList = storeConfig.getHotRodClientProperties().getProperty(ConfigurationProperties.SERVER_LIST);
                        serverList = serverList==null?address:serverList+";"+address;
                        storeConfig.getHotRodClientProperties().setProperty(ConfigurationProperties.SERVER_LIST, serverList);
                    }
                    @Override
                    public void uninject() {
                    }
                });
            additionalDeps.add(dep);
            }
            if (store.hasDefined(ModelKeys.CACHE)) {
                storeConfig.setRemoteCacheName(store.get(ModelKeys.CACHE).asString());
            }
            if (store.hasDefined(ModelKeys.SOCKET_TIMEOUT)) {
                properties.setProperty(ConfigurationProperties.SO_TIMEOUT, store.require(ModelKeys.SOCKET_TIMEOUT).asString());
            }
            if (store.hasDefined(ModelKeys.TCP_NO_DELAY)) {
                properties.setProperty(ConfigurationProperties.TCP_NO_DELAY, store.require(ModelKeys.TCP_NO_DELAY).asString());
            }
            storeConfig.setHotRodClientProperties(properties);
            return storeConfig;
        }

        String className = store.require(ModelKeys.CLASS).asString();
        try {
            CacheStore cacheStore = CacheStore.class.getClassLoader().loadClass(className).asSubclass(CacheStore.class).newInstance();
            CacheStoreConfig storeConfig = cacheStore.getConfigurationClass().asSubclass(CacheStoreConfig.class).newInstance();
            if (storeConfig instanceof AbstractCacheLoaderConfig) {
                ((AbstractCacheLoaderConfig) storeConfig).setProperties(properties);
                XmlConfigHelper.setValues(storeConfig, properties, false, true);
            }
            return storeConfig;
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("%s is not a valid cache store", className), e);
        }
    }

    private AbstractJdbcCacheStoreConfig buildJDBCStoreConfig(ModelNode store) {
        boolean useEntryTable = store.hasDefined(ModelKeys.ENTRY_TABLE);
        boolean useBucketTable = store.hasDefined(ModelKeys.BUCKET_TABLE);
        if (useEntryTable && !useBucketTable) {
            JdbcStringBasedCacheStoreConfig storeConfig = new JdbcStringBasedCacheStoreConfig();
            storeConfig.setTableManipulation(this.buildEntryTableManipulation(store.get(ModelKeys.ENTRY_TABLE)));
            return storeConfig;
        } else if (useBucketTable && !useEntryTable) {
            JdbcBinaryCacheStoreConfig storeConfig = new JdbcBinaryCacheStoreConfig();
            storeConfig.setTableManipulation(this.buildBucketTableManipulation(store.get(ModelKeys.BUCKET_TABLE)));
            return storeConfig;
        }
        // Else, use mixed mode
        JdbcMixedCacheStoreConfig storeConfig = new JdbcMixedCacheStoreConfig();
        storeConfig.setStringsTableManipulation(this.buildEntryTableManipulation(store.get(ModelKeys.ENTRY_TABLE)));
        storeConfig.setBinaryTableManipulation(this.buildBucketTableManipulation(store.get(ModelKeys.BUCKET_TABLE)));
        return storeConfig;
    }

    private TableManipulation buildBucketTableManipulation(ModelNode table) {
        return this.buildTableManipulation(table, "ispn_bucket");
    }

    private TableManipulation buildEntryTableManipulation(ModelNode table) {
        return this.buildTableManipulation(table, "ispn_entry");
    }

    private TableManipulation buildTableManipulation(ModelNode table, String defaultPrefix) {
        TableManipulation manipulation = new TableManipulation();
        manipulation.setBatchSize(table.isDefined() && table.hasDefined(ModelKeys.BATCH_SIZE) ? table.get(ModelKeys.BATCH_SIZE).asInt() : TableManipulation.DEFAULT_BATCH_SIZE);
        manipulation.setFetchSize(table.isDefined() && table.hasDefined(ModelKeys.FETCH_SIZE) ? table.get(ModelKeys.FETCH_SIZE).asInt() : TableManipulation.DEFAULT_FETCH_SIZE);
        manipulation.setTableNamePrefix(table.isDefined() && table.hasDefined(ModelKeys.PREFIX) ? table.get(ModelKeys.PREFIX).asString() : defaultPrefix);
        manipulation.setIdColumnName(this.getColumnProperty(table,  ModelKeys.ID_COLUMN, ModelKeys.NAME, "id"));
        manipulation.setIdColumnType(this.getColumnProperty(table,  ModelKeys.ID_COLUMN, ModelKeys.TYPE, "VARCHAR"));
        manipulation.setDataColumnName(this.getColumnProperty(table,  ModelKeys.DATA_COLUMN, ModelKeys.NAME, "datum"));
        manipulation.setDataColumnType(this.getColumnProperty(table,  ModelKeys.DATA_COLUMN, ModelKeys.TYPE, "BINARY"));
        manipulation.setTimestampColumnName(this.getColumnProperty(table,  ModelKeys.TIMESTAMP_COLUMN, ModelKeys.NAME, "version"));
        manipulation.setTimestampColumnType(this.getColumnProperty(table,  ModelKeys.TIMESTAMP_COLUMN, ModelKeys.TYPE, "BIGINT"));
        return manipulation;
    }

    String getColumnProperty(ModelNode table, String columnKey, String key, String defaultValue) {
        if (!table.isDefined() || !table.hasDefined(columnKey)) return defaultValue;

        ModelNode column = table.get(columnKey);
        return column.hasDefined(key) ? column.get(key).asString() : defaultValue;
    }

    void performCacheRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        // Configuration to hold the operation data
        Configuration overrides = new Configuration() ;

         // create a list for dependencies which may need to be added during processing
        List<AdditionalDependency> additionalDeps = new LinkedList<AdditionalDependency>() ;

        // process cache configuration ModelNode describing overrides to defaults
        processCacheModelNode(model, overrides, additionalDeps) ;

        // get all required addresses, names and service names
        PathAddress cacheAddress = PathAddress.pathAddress(operation.get(OP_ADDR)) ;
        PathAddress containerAddress = cacheAddress.subAddress(0, cacheAddress.size()-1) ;
        String cacheName = cacheAddress.getLastElement().getValue() ;
        String containerName = containerAddress.getLastElement().getValue() ;
        ServiceName containerServiceName = EmbeddedCacheManagerService.getServiceName(containerName) ;
        ServiceName cacheServiceName = containerServiceName.append(cacheName) ;
        ServiceName cacheConfigurationServiceName = CacheConfigurationService.getServiceName(containerName, cacheName);

        // get container Model
        Resource rootResource = context.getRootResource() ;
        ModelNode container = rootResource.navigate(containerAddress).getModel() ;

        // get default cache of the container and start mode
        String defaultCache = container.require(ModelKeys.DEFAULT_CACHE).asString() ;
        StartMode startMode = operation.hasDefined(ModelKeys.START) ? StartMode.valueOf(operation.get(ModelKeys.START).asString()) : StartMode.LAZY;

        // setup configuration helper
        CacheConfigurationService.CacheConfigurationHelperImpl helper = new CacheConfigurationService.CacheConfigurationHelperImpl(cacheName) ;

        // install the cache configuration service (configures a cache)
        ServiceTarget target = context.getServiceTarget() ;
        CacheConfigurationService cacheConfigurationService = new CacheConfigurationService(cacheName, overrides, helper);

        ServiceBuilder<Configuration> configBuilder = target.addService(cacheConfigurationServiceName, cacheConfigurationService) ;
        configBuilder.addDependency(containerServiceName, CacheContainer.class, helper.getCacheContainerInjector()) ;
        configBuilder.addDependency(EmbeddedCacheManagerDefaultsService.SERVICE_NAME, EmbeddedCacheManagerDefaults.class, helper.getDefaultsInjector());
        configBuilder.addDependency(ServiceBuilder.DependencyType.OPTIONAL, TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, helper.getTransactionManagerInjector());
        configBuilder.addDependency(ServiceBuilder.DependencyType.OPTIONAL, TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, helper.getTransactionSynchronizationRegistryInjector());
        // builder.addDependency(ServiceBuilder.DependencyType.OPTIONAL, TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, XAResourceRecoveryRegistry.class, helper.getXAResourceRecoveryRegistryInjector());
        // add in any additional dependencies resulting from ModelNode parsing
        for (AdditionalDependency dep : additionalDeps) {
            if (dep.hasInjector()) {
                configBuilder.addDependency(dep.getName(), dep.getType(), dep.getTarget()) ;
            } else {
                configBuilder.addDependency(dep.getName());
            }
        }
        // cache configuration service always started on demand
        configBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND);
        // add an alias for the default cache
        if (cacheName.equals(defaultCache)) {
            configBuilder.addAliases(CacheConfigurationService.getServiceName(containerName, null));
        }
        newControllers.add(configBuilder.install());
        log.debug("cache configuration service for " + cacheName + " installed for container " + containerName);

        // now install the corresponding cache service (starts a configured cache)
        CacheService<Object, Object> cacheService = new CacheService<Object, Object>(cacheName);

        ServiceBuilder<Cache<Object,Object>> cacheBuilder = target.addService(cacheServiceName, cacheService) ;
        cacheBuilder.addDependency(containerServiceName, CacheContainer.class, cacheService.getCacheContainerInjector()) ;
        cacheBuilder.addDependency(cacheConfigurationServiceName);
        cacheBuilder.setInitialMode(startMode.getMode());

        // add an alias for the default cache
        if (cacheName.equals(defaultCache)) {
            cacheBuilder.addAliases(CacheService.getServiceName(containerName,  null));
        }

        // blah
        if (startMode.getMode() == ServiceController.Mode.ACTIVE) {
            cacheBuilder.addListener(verificationHandler);
        }

        newControllers.add(cacheBuilder.install());
        log.debugf("Cache service for cache %s installed for container %s", cacheName, containerName);
    }

    /*
     * Allows us to store dependency requirements for later processing.
     */
    protected class AdditionalDependency<I> {
        private final ServiceName name ;
        private final Class<I> type ;
        private final Injector<I> target ;
        private final boolean hasInjector ;

        AdditionalDependency(ServiceName name) {
            this.name = name;
            this.type = null;
            this.target = null;
            this.hasInjector = false ;
        }

        AdditionalDependency(ServiceName name, Class<I> type, Injector<I> target) {
            this.name = name ;
            this.type = type ;
            this.target = target ;
            this.hasInjector = true ;
        }

        ServiceName getName() {
            return name ;
        }
        public boolean hasInjector() {
            return hasInjector;
        }
        public Class<I> getType() {
            return type;
        }

        public Injector<I> getTarget() {
            return target;
        }
    }
}
