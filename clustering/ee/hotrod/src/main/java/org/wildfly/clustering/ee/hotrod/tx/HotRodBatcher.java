/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.hotrod.tx;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.wildfly.clustering.ee.cache.tx.TransactionalBatcher;

/**
 * @author Paul Ferraro
 */
public class HotRodBatcher extends TransactionalBatcher<HotRodClientException> {

    public HotRodBatcher(RemoteCache<?, ?> cache) {
        super(cache.getTransactionManager(), HotRodClientException::new);
    }
}
