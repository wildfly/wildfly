/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.ItemWriter;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Named
//@Singleton
public class CountingItemWriter implements ItemWriter {

    @Inject
    private Counter counter;

    @Inject
    @BatchProperty(name = "writer.sleep.time")
    private long sleep;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        counter.increment(items.size());
        if (sleep > 0) {
            TimeUnit.MILLISECONDS.sleep(sleep);
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return counter;
    }

    public int getWrittenItemSize() {
        return counter.get();
    }
}
