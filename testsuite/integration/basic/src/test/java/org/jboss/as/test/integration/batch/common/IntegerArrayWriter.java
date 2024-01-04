/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import java.util.List;
import jakarta.batch.api.chunk.ItemWriter;
import jakarta.inject.Named;

@Named("integerArrayWriter")
public final class IntegerArrayWriter extends IntegerArrayReaderWriterBase implements ItemWriter {
    @Override
    public void writeItems(final List<Object> items) throws Exception {
        if (items == null) {
            return;
        }

        /*if (Metric.getMetric(stepContext, Metric.MetricType.WRITE_COUNT) + items.size() >= writerFailAt
                && writerFailAt >= 0) {
            throw new ArithmeticException("Failing at writer.fail.at point " + writerFailAt);
        }*/
        if (writerSleepTime > 0) {
            Thread.sleep(writerSleepTime);
        }
        for (final Object o : items) {
            data[cursor] = (Integer) o;
            cursor++;
        }
    }

    @Override
    protected void initData() {
        super.initData();
        cursor = partitionStart;
    }
}
