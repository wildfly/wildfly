/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.ItemReader;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Named
public class CountingItemReader implements ItemReader {

    @Inject
    @BatchProperty(name = "reader.start")
    private int start;

    @Inject
    @BatchProperty(name = "reader.end")
    private int end;

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        if (end == 0) {
            end = 10;
        }
        counter.set(start);
    }

    @Override
    public void close() throws Exception {
        counter.set(0);
    }

    @Override
    public Object readItem() throws Exception {
        final int result = counter.incrementAndGet();
        if (result > end) {
            return null;
        }
        return result;
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return counter.get();
    }
}
