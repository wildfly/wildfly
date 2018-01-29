/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

        Configuration config = new ConfigurationBuilder().clustering().cacheMode(CacheMode.LOCAL).persistence().passivation(false).addSingleFileStore().build();
        Assert.assertTrue(new InfinispanCacheProperties(config).isMarshalling());

        Configuration passivating = new ConfigurationBuilder().read(config).persistence().passivation(true).build();
        Assert.assertTrue(new InfinispanCacheProperties(passivating).isMarshalling());

        Configuration noStore = new ConfigurationBuilder().read(config).persistence().clearStores().build();
        Assert.assertFalse(new InfinispanCacheProperties(noStore).isMarshalling());
    }

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

        Configuration config = new ConfigurationBuilder().clustering().cacheMode(CacheMode.LOCAL).persistence().passivation(false).addSingleFileStore().build();
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
