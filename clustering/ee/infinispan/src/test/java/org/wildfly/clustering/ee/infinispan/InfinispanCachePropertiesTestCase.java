/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan;

import java.util.EnumSet;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.concurrent.IsolationLevel;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ee.cache.CacheProperties;

/**
 * @author Paul Ferraro
 */
public class InfinispanCachePropertiesTestCase {

    @Test
    public void isLockOnRead() {
        Configuration config = new ConfigurationBuilder().transaction().transactionMode(TransactionMode.TRANSACTIONAL).lockingMode(LockingMode.PESSIMISTIC).locking().isolationLevel(IsolationLevel.REPEATABLE_READ).build();
        Assert.assertTrue(new InfinispanCacheProperties(config).isLockOnRead());

        Configuration optimistic = config = new ConfigurationBuilder().read(config).transaction().lockingMode(LockingMode.OPTIMISTIC).build();
        Assert.assertFalse(new InfinispanCacheProperties(optimistic).isLockOnRead());

        Configuration nonTx = new ConfigurationBuilder().read(config).transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL).build();
        Assert.assertFalse(new InfinispanCacheProperties(nonTx).isLockOnRead());

        Configuration readCommitted = config = new ConfigurationBuilder().read(config).locking().isolationLevel(IsolationLevel.READ_COMMITTED).build();
        Assert.assertFalse(new InfinispanCacheProperties(readCommitted).isLockOnRead());
    }

    @Test
    public void isLockOnWrite() {
        Configuration config = new ConfigurationBuilder().transaction().transactionMode(TransactionMode.TRANSACTIONAL).lockingMode(LockingMode.PESSIMISTIC).build();
        Assert.assertTrue(new InfinispanCacheProperties(config).isLockOnWrite());

        Configuration optimistic = config = new ConfigurationBuilder().read(config).transaction().lockingMode(LockingMode.OPTIMISTIC).build();
        Assert.assertFalse(new InfinispanCacheProperties(optimistic).isLockOnWrite());

        Configuration nonTx = new ConfigurationBuilder().read(config).transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL).build();
        Assert.assertFalse(new InfinispanCacheProperties(nonTx).isLockOnWrite());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void isMarshalling() {
        for (CacheMode mode : EnumSet.allOf(CacheMode.class)) {
            Configuration config = new ConfigurationBuilder().clustering().cacheMode(mode).build();
            CacheProperties configuration = new InfinispanCacheProperties(config);
            if (mode.isDistributed() || mode.isReplicated() || mode.isScattered()) {
                Assert.assertTrue(mode.name(), configuration.isMarshalling());
            } else {
                Assert.assertFalse(mode.name(), configuration.isMarshalling());
            }
        }

        Configuration config = new ConfigurationBuilder().clustering().cacheMode(CacheMode.LOCAL).persistence().passivation(false).addSoftIndexFileStore().build();
        Assert.assertTrue(new InfinispanCacheProperties(config).isMarshalling());

        Configuration passivating = new ConfigurationBuilder().read(config).persistence().passivation(true).build();
        Assert.assertTrue(new InfinispanCacheProperties(passivating).isMarshalling());

        Configuration noStore = new ConfigurationBuilder().read(config).persistence().clearStores().build();
        Assert.assertFalse(new InfinispanCacheProperties(noStore).isMarshalling());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void isPersistent() {
        for (CacheMode mode : EnumSet.allOf(CacheMode.class)) {
            Configuration config = new ConfigurationBuilder().clustering().cacheMode(mode).build();
            CacheProperties configuration = new InfinispanCacheProperties(config);
            if (mode.isDistributed() || mode.isReplicated() || mode.isScattered()) {
                Assert.assertTrue(mode.name(), configuration.isPersistent());
            } else {
                Assert.assertFalse(mode.name(), configuration.isPersistent());
            }
        }

        Configuration config = new ConfigurationBuilder().clustering().cacheMode(CacheMode.LOCAL).persistence().passivation(false).addSoftIndexFileStore().build();
        Assert.assertTrue(new InfinispanCacheProperties(config).isPersistent());

        Configuration passivating = new ConfigurationBuilder().read(config).persistence().passivation(true).build();
        Assert.assertFalse(new InfinispanCacheProperties(passivating).isPersistent());

        Configuration noStore = new ConfigurationBuilder().read(config).persistence().clearStores().build();
        Assert.assertFalse(new InfinispanCacheProperties(noStore).isPersistent());
    }

    @Test
    public void isTransactional() {
        Configuration config = new ConfigurationBuilder().transaction().transactionMode(TransactionMode.TRANSACTIONAL).build();
        Assert.assertTrue(new InfinispanCacheProperties(config).isTransactional());

        config = new ConfigurationBuilder().transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL).build();
        Assert.assertFalse(new InfinispanCacheProperties(config).isTransactional());
    }
}
