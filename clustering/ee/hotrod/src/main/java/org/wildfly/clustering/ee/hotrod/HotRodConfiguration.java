/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.hotrod;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
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

    default Flag[] getIgnoreReturnFlags() {
        return this.getNearCacheMode().enabled() ? new Flag[0] : new Flag[] { Flag.SKIP_LISTENER_NOTIFICATION };
    }

    default Flag[] getForceReturnFlags() {
        return this.getNearCacheMode().enabled() ? new Flag[] { Flag.FORCE_RETURN_VALUE } : new Flag[] { Flag.FORCE_RETURN_VALUE, Flag.SKIP_LISTENER_NOTIFICATION };
    }

    default NearCacheMode getNearCacheMode() {
        RemoteCache<?, ?> cache = this.getCache();
        return cache.getRemoteCacheContainer().getConfiguration().remoteCaches().get(cache.getName()).nearCacheMode();
    }

    @Override
    default CacheProperties getCacheProperties() {
        return new RemoteCacheProperties(this.getCache());
    }

    @Override
    default Batcher<TransactionBatch> getBatcher() {
        return new HotRodBatcher(this.getCache());
    }
}
