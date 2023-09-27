/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee.infinispan;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.transaction.TransactionMode;
import org.junit.Test;
import org.wildfly.clustering.ee.Mutator;

/**
 * Unit test for {@link CacheEntryMutator}.
 *
 * @author Paul Ferraro
 */
public class CacheEntryMutatorTestCase {

    @Test
    public void mutateTransactional() {
        AdvancedCache<Object, Object> cache = mock(AdvancedCache.class);
        Object id = new Object();
        Object value = new Object();
        Configuration config = new ConfigurationBuilder().transaction().transactionMode(TransactionMode.TRANSACTIONAL).build();

        when(cache.getCacheConfiguration()).thenReturn(config);

        Mutator mutator = new CacheEntryMutator<>(cache, id, value);

        when(cache.getAdvancedCache()).thenReturn(cache);
        when(cache.withFlags(Flag.IGNORE_RETURN_VALUES, Flag.FAIL_SILENTLY)).thenReturn(cache);

        mutator.mutate();

        verify(cache).put(same(id), same(value));

        mutator.mutate();

        verify(cache, times(1)).put(same(id), same(value));

        mutator.mutate();

        verify(cache, times(1)).put(same(id), same(value));
    }

    @Test
    public void mutateNonTransactional() {
        AdvancedCache<Object, Object> cache = mock(AdvancedCache.class);
        Object id = new Object();
        Object value = new Object();
        Configuration config = new ConfigurationBuilder().transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL).build();

        when(cache.getCacheConfiguration()).thenReturn(config);

        Mutator mutator = new CacheEntryMutator<>(cache, id, value);

        when(cache.getAdvancedCache()).thenReturn(cache);
        when(cache.withFlags(Flag.IGNORE_RETURN_VALUES, Flag.FAIL_SILENTLY)).thenReturn(cache);

        mutator.mutate();

        verify(cache).put(same(id), same(value));

        mutator.mutate();

        verify(cache, times(2)).put(same(id), same(value));

        mutator.mutate();

        verify(cache, times(3)).put(same(id), same(value));
    }
}
