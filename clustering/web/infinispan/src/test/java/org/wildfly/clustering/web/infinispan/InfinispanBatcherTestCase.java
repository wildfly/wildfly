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

package org.wildfly.clustering.web.infinispan;

import static org.mockito.Mockito.*;

import org.infinispan.Cache;
import org.junit.Test;
import org.wildfly.clustering.web.Batch;
import org.wildfly.clustering.web.Batcher;

/**
 * Unit test for {@link InfinispanBatcher}
 * @author Paul Ferraro
 */
public class InfinispanBatcherTestCase {

    private final Cache<?, ?> cache = mock(Cache.class);

    private final Batcher batcher = new InfinispanBatcher(this.cache);

    @SuppressWarnings({ "resource", "unchecked" })
    @Test
    public void startBatch() {

        when(this.cache.startBatch()).thenReturn(true);

        Batch batch = this.batcher.startBatch();

        reset(this.cache);

        batch.close();

        verify(this.cache, only()).endBatch(true);
        reset(this.cache);

        batch.discard();

        verify(this.cache, only()).endBatch(false);
        reset(this.cache);
    }

    @SuppressWarnings({ "resource", "unchecked" })
    @Test
    public void startNestedBatch() {

        when(this.cache.startBatch()).thenReturn(false);

        Batch batch = this.batcher.startBatch();

        batch.close();

        verify(this.cache, never()).endBatch(true);
        reset(this.cache);

        batch.discard();

        verify(this.cache, never()).endBatch(false);
        reset(this.cache);
    }
}
