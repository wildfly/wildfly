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

package org.jboss.as.test.integration.batch.common;

import java.io.Serializable;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

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
