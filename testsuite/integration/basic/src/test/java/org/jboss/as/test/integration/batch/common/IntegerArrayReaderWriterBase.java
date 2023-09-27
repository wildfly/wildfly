/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import java.io.Serializable;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;

public abstract class IntegerArrayReaderWriterBase {
    @Inject
    protected StepContext stepContext;

    @Inject
    @BatchProperty(name = "data.count")
    protected Integer dataCount;

    @Inject
    @BatchProperty(name = "partition.start")
    protected int partitionStart;

    @Inject
    @BatchProperty(name = "partition.end")
    protected Integer partitionEnd;

    @Inject
    @BatchProperty(name = "reader.fail.at")
    protected Integer readerFailAt;

    @Inject
    @BatchProperty(name = "writer.fail.at")
    protected Integer writerFailAt;

    @Inject
    @BatchProperty(name = "writer.sleep.time")
    protected long writerSleepTime;

    protected Integer[] data;
    protected int cursor;

    /**
     * Creates the data array without filling the data.
     */
    protected void initData() {
        if (dataCount == null) {
            throw new IllegalStateException("data.count property is not injected.");
        }
        data = new Integer[dataCount];
        if (readerFailAt == null) {
            readerFailAt = -1;
        }
        if (writerFailAt == null) {
            writerFailAt = -1;
        }
        if (partitionEnd == null) {
            partitionEnd = dataCount - 1;
        }
    }

    public void open(final Serializable checkpoint) throws Exception {
        if (data == null) {
            initData();
        }
        cursor = checkpoint == null ? partitionStart : (Integer) checkpoint;
    }

    public Serializable checkpointInfo() throws Exception {
        return cursor;
    }

    public void close() throws Exception {
    }
}
