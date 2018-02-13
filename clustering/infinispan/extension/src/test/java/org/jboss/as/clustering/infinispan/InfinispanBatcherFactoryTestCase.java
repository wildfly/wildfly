/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.transaction.TransactionMode;
import org.junit.Test;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;

/**
 * Unit test for {@link InfinispanBatcherFactory}.
 * @author Paul Ferraro
 */
public class InfinispanBatcherFactoryTestCase {

    private final BatcherFactory factory = new InfinispanBatcherFactory();

    @Test
    public void transactional() {
        @SuppressWarnings("unchecked")
        AdvancedCache<Object, Object> cache = mock(AdvancedCache.class);
        Configuration configuration = new ConfigurationBuilder().transaction().transactionMode(TransactionMode.TRANSACTIONAL).build();

        when(cache.getAdvancedCache()).thenReturn(cache);
        when(cache.getCacheConfiguration()).thenReturn(configuration);

        Batcher<TransactionBatch> batcher = this.factory.createBatcher(cache);

        assertNotNull(batcher);
    }

    @Test
    public void nonTransactional() {
        @SuppressWarnings("unchecked")
        AdvancedCache<Object, Object> cache = mock(AdvancedCache.class);
        Configuration configuration = new ConfigurationBuilder().transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL).build();

        when(cache.getAdvancedCache()).thenReturn(cache);
        when(cache.getCacheConfiguration()).thenReturn(configuration);

        Batcher<TransactionBatch> batcher = this.factory.createBatcher(cache);

        assertNull(batcher);
    }
}
