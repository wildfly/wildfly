/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.mockito.Matchers.same;
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
