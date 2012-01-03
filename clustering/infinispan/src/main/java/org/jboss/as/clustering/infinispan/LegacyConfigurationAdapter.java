package org.jboss.as.clustering.infinispan;

import java.util.Properties;

import org.infinispan.commons.hash.Hash;
import org.infinispan.config.Configuration;
import org.infinispan.config.Configuration.CacheMode;
import org.infinispan.config.CustomInterceptorConfig;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.config.FluentConfiguration.CustomInterceptorPosition;
import org.infinispan.config.FluentConfiguration.IndexingConfig;
import org.infinispan.config.parsing.XmlConfigHelper;
import org.infinispan.configuration.cache.AbstractLoaderConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.FileCacheStoreConfiguration;
import org.infinispan.configuration.cache.InterceptorConfiguration;
import org.infinispan.configuration.cache.InterceptorConfiguration.Position;
import org.infinispan.configuration.cache.InterceptorConfigurationBuilder;
import org.infinispan.configuration.cache.LoaderConfiguration;
import org.infinispan.configuration.cache.LoaderConfigurationBuilder;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.loaders.AbstractCacheLoaderConfig;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.loaders.CacheLoader;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderMetadata;
import org.infinispan.loaders.CacheStoreConfig;
import org.infinispan.loaders.file.FileCacheStoreConfig;
import org.infinispan.remoting.ReplicationQueue;
import org.infinispan.util.Util;

/**
 * Duplicates org.infinispan.configuration.cache.LegacyConfigurationAdaptor with Workarounds
 * for ISPN-1684 and ISPN-1686
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
public class LegacyConfigurationAdapter {

    public org.infinispan.config.Configuration adapt(org.infinispan.configuration.cache.Configuration config) {

        // Handle the case that null is passed in
        if (config == null)
            return null;

        FluentConfiguration legacy = new Configuration().fluent();

        legacy.clustering().mode(CacheMode.valueOf(config.clustering().cacheMode().name()));

        if (!config.clustering().cacheMode().isSynchronous()) {
            legacy.clustering().async().asyncMarshalling(config.clustering().async().asyncMarshalling())
                    .replQueueClass(config.clustering().async().replQueue().getClass())
                    .replQueueInterval(config.clustering().async().replQueueInterval())
                    .replQueueMaxElements(config.clustering().async().replQueueMaxElements());
        }

        if (config.clustering().hash().consistentHash() != null) {
            legacy.clustering().hash().consistentHashClass(config.clustering().hash().consistentHash().getClass());

        }
        if (config.clustering().hash().hash() != null) {
            legacy.clustering().hash().hashFunctionClass(config.clustering().hash().hash().getClass());
        }
        legacy.clustering().hash().numOwners(config.clustering().hash().numOwners())
                .numVirtualNodes(config.clustering().hash().numVirtualNodes())
                .rehashEnabled(config.clustering().hash().rehashEnabled())
                .rehashRpcTimeout(config.clustering().hash().rehashRpcTimeout())
                .rehashWait(config.clustering().hash().rehashWait()).groups()
                .enabled(config.clustering().hash().groupsConfiguration().enabled())
                .groupers(config.clustering().hash().groupsConfiguration().groupers());

        if (config.clustering().l1().enabled()) {
            legacy.clustering().l1().invalidationThreshold(config.clustering().l1().invalidationThreshold())
                    .lifespan(config.clustering().l1().lifespan()).onRehash(config.clustering().l1().onRehash());
        } else {
            legacy.clustering().l1().disable();
        }

        legacy.clustering().stateRetrieval()
                .alwaysProvideInMemoryState(config.clustering().stateRetrieval().alwaysProvideInMemoryState())
                .fetchInMemoryState(config.clustering().stateRetrieval().fetchInMemoryState())
                .initialRetryWaitTime(config.clustering().stateRetrieval().initialRetryWaitTime())
                .logFlushTimeout(config.clustering().stateRetrieval().logFlushTimeout())
                .maxNonProgressingLogWrites(config.clustering().stateRetrieval().maxNonProgressingLogWrites())
                .numRetries(config.clustering().stateRetrieval().numRetries())
                .retryWaitTimeIncreaseFactor(config.clustering().stateRetrieval().retryWaitTimeIncreaseFactor())
                .timeout(config.clustering().stateRetrieval().timeout());

        if (config.clustering().cacheMode().isSynchronous()) {
            legacy.clustering().sync().replTimeout(config.clustering().sync().replTimeout());
        }

        for (InterceptorConfiguration interceptor : config.customInterceptors().interceptors()) {
            CustomInterceptorPosition position = legacy.clustering().customInterceptors()
            // TODO This seems to miss most of the parameters like after and before out?!?
                    .add(interceptor.interceptor());
            if (interceptor.after() != null)
                position.after(interceptor.after());
            if (interceptor.index() != -1)
                position.atIndex(interceptor.index());
            if (interceptor.before() != null)
                position.before(interceptor.before());
            if (interceptor.first())
                position.first();
            if (interceptor.last())
                position.last();
        }

        legacy.dataContainer().dataContainer(config.dataContainer().dataContainer())
                .withProperties(config.dataContainer().properties());

        if (config.deadlockDetection().enabled()) {
            legacy.deadlockDetection().spinDuration(config.deadlockDetection().spinDuration());
        } else {
            legacy.deadlockDetection().disable();
        }

        legacy.eviction().maxEntries(config.eviction().maxEntries()).strategy(config.eviction().strategy())
                .threadPolicy(config.eviction().threadPolicy());

        legacy.expiration().lifespan(config.expiration().lifespan()).maxIdle(config.expiration().maxIdle())
                .reaperEnabled(config.expiration().reaperEnabled()).wakeUpInterval(config.expiration().wakeUpInterval());

        if (config.indexing().enabled()) {
            IndexingConfig indexing = legacy.indexing();
            indexing.indexLocalOnly(config.indexing().indexLocalOnly());
            indexing.withProperties(config.indexing().properties());
        } else
            legacy.indexing().disable();

        if (config.invocationBatching().enabled())
            legacy.invocationBatching();

        if (config.jmxStatistics().enabled())
            legacy.jmxStatistics();

        // TODO lazy deserialization?

        legacy.loaders().passivation(config.loaders().passivation()).preload(config.loaders().preload())
                .shared(config.loaders().shared());

        for (AbstractLoaderConfiguration loader : config.loaders().cacheLoaders()) {
            CacheLoaderConfig clc = null;
            if (loader instanceof LoaderConfiguration) {
                CacheLoader cacheLoader = ((LoaderConfiguration) loader).cacheLoader();
                if (cacheLoader.getClass().isAnnotationPresent(CacheLoaderMetadata.class)) {
                    clc = Util
                            .getInstance(cacheLoader.getClass().getAnnotation(CacheLoaderMetadata.class).configurationClass());
                } else {
                    AbstractCacheStoreConfig acsc = new AbstractCacheStoreConfig();
                    acsc.setCacheLoaderClassName(((LoaderConfiguration) loader).cacheLoader().getClass().getName());
                    clc = acsc;
                }

            } else if (loader instanceof FileCacheStoreConfiguration) {
                FileCacheStoreConfig fcsc = new FileCacheStoreConfig();
                clc = fcsc;
                String location = loader.properties().getProperty("location");
                if (location != null)
                    fcsc.location(location);
            }
            if (clc instanceof CacheStoreConfig) {
                CacheStoreConfig csc = (CacheStoreConfig) clc;
                csc.fetchPersistentState(loader.fetchPersistentState());
                csc.ignoreModifications(loader.ignoreModifications());
                csc.purgeOnStartup(loader.purgeOnStartup());
                csc.setPurgeSynchronously(loader.purgeSynchronously());
                csc.getAsyncStoreConfig().setEnabled(loader.async().enabled());
                csc.getAsyncStoreConfig().flushLockTimeout(loader.async().flushLockTimeout());
                csc.getAsyncStoreConfig().modificationQueueSize(loader.async().modificationQueueSize());
                csc.getAsyncStoreConfig().shutdownTimeout(loader.async().shutdownTimeout());
                csc.getAsyncStoreConfig().threadPoolSize(loader.async().threadPoolSize());

                csc.getSingletonStoreConfig().enabled(loader.singletonStore().enabled());
                csc.getSingletonStoreConfig().pushStateTimeout(loader.singletonStore().pushStateTimeout());
                csc.getSingletonStoreConfig().pushStateWhenCoordinator(loader.singletonStore().pushStateWhenCoordinator());
            }
            if (clc instanceof AbstractCacheStoreConfig) {
                AbstractCacheStoreConfig acsc = (AbstractCacheStoreConfig) clc;
                Properties p = loader.properties();
                acsc.setProperties(p);
                if (p != null)
                    XmlConfigHelper.setValues(clc, p, false, true);
                if (loader instanceof LoaderConfiguration)
                    acsc.purgerThreads(((LoaderConfiguration) loader).purgerThreads());
            }

            legacy.loaders().addCacheLoader(clc);
        }

        legacy.locking().concurrencyLevel(config.locking().concurrencyLevel())
                .isolationLevel(config.locking().isolationLevel())
                .lockAcquisitionTimeout(config.locking().lockAcquisitionTimeout())
                .useLockStriping(config.locking().useLockStriping()).writeSkewCheck(config.locking().writeSkewCheck());

        if (config.storeAsBinary().enabled())
            legacy.storeAsBinary().storeKeysAsBinary(config.storeAsBinary().storeKeysAsBinary())
                    .storeValuesAsBinary(config.storeAsBinary().storeValuesAsBinary());
        else
            legacy.storeAsBinary().disable();

        legacy.transaction().autoCommit(config.transaction().autoCommit())
                .cacheStopTimeout((int) config.transaction().cacheStopTimeout())
                .eagerLockSingleNode(config.transaction().eagerLockingSingleNode())
                .lockingMode(config.transaction().lockingMode()).syncCommitPhase(config.transaction().syncCommitPhase())
                .syncRollbackPhase(config.transaction().syncRollbackPhase())
                .transactionManagerLookup(config.transaction().transactionManagerLookup())
                .transactionMode(config.transaction().transactionMode())
                .transactionSynchronizationRegistryLookup(config.transaction().transactionSynchronizationRegistryLookup())
                .useEagerLocking(config.transaction().useEagerLocking())
                .useSynchronization(config.transaction().useSynchronization());

        if (config.transaction().recovery().enabled()) {
            legacy.transaction().recovery().recoveryInfoCacheName(config.transaction().recovery().recoveryInfoCacheName());
        }

        legacy.unsafe().unreliableReturnValues(config.unsafe().unreliableReturnValues());

        if (config.versioning().enabled()) {
            legacy.versioning().enable().versioningScheme(config.versioning().scheme());
        }

        return legacy.build();
    }

    public org.infinispan.configuration.cache.Configuration adapt(org.infinispan.config.Configuration legacy) {

        // Handle the case that null is passed in
        if (legacy == null)
            return null;

        ConfigurationBuilder builder = new ConfigurationBuilder();

        builder.clustering().cacheMode(org.infinispan.configuration.cache.CacheMode.valueOf(legacy.getCacheMode().name()));

        if (!legacy.getCacheMode().isSynchronous()) {
            if (legacy.isUseAsyncMarshalling())
                builder.clustering().async().asyncMarshalling();
            else
                builder.clustering().async().syncMarshalling();
            builder.clustering().async()
                    .replQueue(Util.<ReplicationQueue> getInstance(legacy.getReplQueueClass(), legacy.getClassLoader()))
                    .replQueueInterval(legacy.getReplQueueInterval()).replQueueMaxElements(legacy.getReplQueueMaxElements());
        }

        if (legacy.getConsistentHashClass() != null) {
            builder.clustering()
                    .hash()
                    .consistentHash(Util.<ConsistentHash> getInstance(legacy.getConsistentHashClass(), legacy.getClassLoader()));

        }
        if (legacy.getHashFunctionClass() != null) {
            builder.clustering().hash().hash(Util.<Hash> getInstance(legacy.getHashFunctionClass(), legacy.getClassLoader()));
        }
        builder.clustering().hash().numOwners(legacy.getNumOwners()).numVirtualNodes(legacy.getNumVirtualNodes())
                .rehashEnabled(legacy.isRehashEnabled()).rehashRpcTimeout(legacy.getRehashRpcTimeout())
                .rehashWait(legacy.getRehashWaitTime()).groups().enabled(legacy.isGroupsEnabled())
                .withGroupers(legacy.getGroupers());

        if (legacy.isL1CacheEnabled()) {
            builder.clustering().l1().invalidationThreshold(legacy.getL1InvalidationThreshold())
                    .lifespan(legacy.getL1Lifespan()).onRehash(legacy.isL1OnRehash());
        } else {
            builder.clustering().l1().disable();
        }

        builder.clustering().stateRetrieval().alwaysProvideInMemoryState(legacy.isAlwaysProvideInMemoryState())
                .fetchInMemoryState(legacy.isFetchInMemoryState())
                .initialRetryWaitTime(legacy.getStateRetrievalInitialRetryWaitTime())
                .logFlushTimeout(legacy.getStateRetrievalLogFlushTimeout())
                .maxNonProgressingLogWrites(legacy.getStateRetrievalMaxNonProgressingLogWrites())
                .numRetries(legacy.getStateRetrievalNumRetries())
                .retryWaitTimeIncreaseFactor(legacy.getStateRetrievalRetryWaitTimeIncreaseFactor())
                .timeout(legacy.getStateRetrievalTimeout());

        if (legacy.getCacheMode().isSynchronous()) {
            builder.clustering().sync().replTimeout(legacy.getSyncReplTimeout());
        }

        for (CustomInterceptorConfig interceptor : legacy.getCustomInterceptors()) {
            InterceptorConfigurationBuilder interceptorConfigurationBuilder = builder.clustering().customInterceptors()
                    .addInterceptor();
            interceptorConfigurationBuilder.after(Util.<CommandInterceptor> loadClass(interceptor.getAfter(),
                    legacy.getClassLoader()));
            interceptorConfigurationBuilder.before(Util.<CommandInterceptor> loadClass(interceptor.getBefore(),
                    legacy.getClassLoader()));
            interceptorConfigurationBuilder.index(interceptor.getIndex());
            interceptorConfigurationBuilder.interceptor(interceptor.getInterceptor());
            interceptorConfigurationBuilder.position(Position.valueOf(interceptor.getPositionAsString()));
        }

        builder.dataContainer().dataContainer(legacy.getDataContainer()).withProperties(legacy.getDataContainerProperties());

        builder.deadlockDetection().enabled(legacy.isDeadlockDetectionEnabled()).spinDuration(legacy.getDeadlockDetectionSpinDuration());

        builder.eviction().maxEntries(legacy.getEvictionMaxEntries()).strategy(legacy.getEvictionStrategy())
                .threadPolicy(legacy.getEvictionThreadPolicy());

        builder.expiration().lifespan(legacy.getExpirationLifespan()).maxIdle(legacy.getExpirationMaxIdle())
                .reaperEnabled(legacy.isExpirationReaperEnabled()).wakeUpInterval(legacy.getExpirationWakeUpInterval());

        builder.indexing().enabled(legacy.isIndexingEnabled()).indexLocalOnly(legacy.isIndexLocalOnly());

        if (legacy.isInvocationBatchingEnabled()) {
            builder.invocationBatching().enable();
        } else {
            builder.invocationBatching().disable();
        }

        builder.jmxStatistics().enabled(legacy.isExposeJmxStatistics());

        // TODO lazy deserialization?

        builder.loaders()
                .passivation(legacy.isCacheLoaderPassivation())
                .preload(legacy.isCacheLoaderPreload())
                .shared(legacy.isCacheLoaderShared());

        for (CacheLoaderConfig clc : legacy.getCacheLoaders()) {
            LoaderConfigurationBuilder loaderBuilder = builder.loaders().addCacheLoader();
            loaderBuilder.cacheLoader(Util.<CacheLoader> getInstance(clc.getCacheLoaderClassName(), legacy.getClassLoader()));
            if (clc instanceof CacheStoreConfig) {
                CacheStoreConfig csc = (CacheStoreConfig) clc;
                loaderBuilder.fetchPersistentState(csc.isFetchPersistentState());
                loaderBuilder.ignoreModifications(csc.isIgnoreModifications());
                loaderBuilder.purgeOnStartup(csc.isPurgeOnStartup());
                loaderBuilder.purgerThreads(csc.getPurgerThreads());
                loaderBuilder.purgeSynchronously(csc.isPurgeSynchronously());
                loaderBuilder.async().enabled(csc.getAsyncStoreConfig().isEnabled());
                loaderBuilder.async().flushLockTimeout(csc.getAsyncStoreConfig().getFlushLockTimeout());
                loaderBuilder.async().modificationQueueSize(csc.getAsyncStoreConfig().getModificationQueueSize());
                loaderBuilder.async().shutdownTimeout(csc.getAsyncStoreConfig().getShutdownTimeout());
                loaderBuilder.async().threadPoolSize(csc.getAsyncStoreConfig().getThreadPoolSize());

                loaderBuilder.singletonStore().enabled(csc.getSingletonStoreConfig().isSingletonStoreEnabled());
                loaderBuilder.singletonStore().pushStateTimeout(csc.getSingletonStoreConfig().getPushStateTimeout());
                loaderBuilder.singletonStore().pushStateWhenCoordinator(csc.getSingletonStoreConfig().isPushStateWhenCoordinator());
            }
            if (clc instanceof AbstractCacheStoreConfig) {
                loaderBuilder.withProperties(((AbstractCacheLoaderConfig) clc).getProperties());
            }
        }

        builder.locking().concurrencyLevel(legacy.getConcurrencyLevel()).isolationLevel(legacy.getIsolationLevel())
                .lockAcquisitionTimeout(legacy.getLockAcquisitionTimeout()).useLockStriping(legacy.isUseLockStriping())
                .writeSkewCheck(legacy.isWriteSkewCheck());

        builder.storeAsBinary()
                .enabled(legacy.isStoreAsBinary())
                .storeKeysAsBinary(legacy.isStoreKeysAsBinary())
                .storeValuesAsBinary(legacy.isStoreValuesAsBinary());

        builder.transaction().autoCommit(legacy.isTransactionAutoCommit()).cacheStopTimeout((int) legacy.getCacheStopTimeout())
                .eagerLockingSingleNode(legacy.isEagerLockSingleNode()).lockingMode(legacy.getTransactionLockingMode())
                .syncCommitPhase(legacy.isSyncCommitPhase()).syncRollbackPhase(legacy.isSyncRollbackPhase())
                .transactionManagerLookup(legacy.getTransactionManagerLookup()).transactionMode(legacy.getTransactionMode())
                .transactionSynchronizationRegistryLookup(legacy.getTransactionSynchronizationRegistryLookup())
                .useEagerLocking(legacy.isUseEagerLocking()).useSynchronization(legacy.isUseSynchronizationForTransactions());

        if (legacy.isTransactionRecoveryEnabled()) {
            builder.transaction().recovery();
        }

        builder.unsafe().unreliableReturnValues(legacy.isUnsafeUnreliableReturnValues());

        return builder.build();
    }
}
