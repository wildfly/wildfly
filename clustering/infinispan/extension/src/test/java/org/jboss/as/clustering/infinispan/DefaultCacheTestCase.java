/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.infinispan.AdvancedCache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.After;
import org.junit.Test;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;

/**
 * Unit test for {@link DefaultCache}.
 *
 * @author Paul Ferraro
 */
public class DefaultCacheTestCase {

    private final EmbeddedCacheManager manager = mock(EmbeddedCacheManager.class);
    private final BatcherFactory batcherFactory = mock(BatcherFactory.class);
    @SuppressWarnings("unchecked")
    private final AdvancedCache<Object, Object> cache = mock(AdvancedCache.class);

    @After
    public void init() {
        reset(this.manager, this.batcherFactory, this.cache);
    }

    @Test
    public void noBatcher() {
        when(this.batcherFactory.createBatcher(this.cache)).thenReturn(null);

        AdvancedCache<Object, Object> subject = new DefaultCache<>(this.manager, this.batcherFactory, this.cache);

        // Validate no-op batching logic
        boolean started = subject.startBatch();

        assertFalse(started);

        verify(this.cache, never()).startBatch();

        subject.endBatch(false);

        verify(this.cache, never()).endBatch(false);
    }

    @Test
    public void batcher() {
        @SuppressWarnings("unchecked")
        Batcher<TransactionBatch> batcher = mock(Batcher.class);
        TransactionBatch batch = mock(TransactionBatch.class);

        when(this.batcherFactory.createBatcher(this.cache)).thenReturn(batcher);

        AdvancedCache<Object, Object> subject = new DefaultCache<>(this.manager, this.batcherFactory, this.cache);

        // Validate batching logic
        when(batcher.createBatch()).thenReturn(batch);

        boolean started = subject.startBatch();

        assertTrue(started);

        started = subject.startBatch();

        assertFalse(started);

        // Verify commit
        subject.endBatch(true);

        verify(batch, never()).discard();
        verify(batch).close();
        reset(batch);

        // Verify re-commit is a no-op
        subject.endBatch(true);

        verify(batch, never()).close();
        verify(batch, never()).discard();

        // Verify rollback
        started = subject.startBatch();

        assertTrue(started);

        subject.endBatch(false);

        verify(batch).discard();
        verify(batch).close();

        reset(batch);

        // Verify re-rollback is a no-op
        subject.endBatch(true);

        verify(batch, never()).close();
        verify(batch, never()).discard();
    }
}
