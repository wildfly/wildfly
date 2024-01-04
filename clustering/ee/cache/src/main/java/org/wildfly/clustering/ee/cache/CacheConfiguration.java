/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache;

import java.util.Map;

import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;

/**
 * @author Paul Ferraro
 */
public interface CacheConfiguration {

    <K, V> Map<K, V> getCache();

    CacheProperties getCacheProperties();

    Batcher<TransactionBatch> getBatcher();
}
