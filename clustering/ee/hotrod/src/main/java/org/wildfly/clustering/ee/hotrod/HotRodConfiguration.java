/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.hotrod;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.CacheConfiguration;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.hotrod.tx.HotRodBatcher;

/**
 * @author Paul Ferraro
 */
public interface HotRodConfiguration extends CacheConfiguration {

    @Override
    <CK, CV> RemoteCache<CK, CV> getCache();

    @Override
    default CacheProperties getCacheProperties() {
        return new RemoteCacheProperties(this.getCache());
    }

    @Override
    default Batcher<TransactionBatch> getBatcher() {
        return new HotRodBatcher(this.getCache());
    }
}
