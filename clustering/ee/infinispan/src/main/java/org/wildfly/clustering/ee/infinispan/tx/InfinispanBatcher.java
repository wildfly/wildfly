/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.tx;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.wildfly.clustering.ee.cache.tx.TransactionalBatcher;

/**
 * @author Paul Ferraro
 */
public class InfinispanBatcher extends TransactionalBatcher<CacheException> {

    public InfinispanBatcher(Cache<?, ?> cache) {
        super(cache.getAdvancedCache().getTransactionManager(), CacheException::new);
    }
}
