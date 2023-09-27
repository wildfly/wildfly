/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import jakarta.batch.api.chunk.ItemReader;
import jakarta.inject.Named;

@Named("integerArrayReader")
public final class IntegerArrayReader extends IntegerArrayReaderWriterBase implements ItemReader {
    @Override
    public Object readItem() throws Exception {
        if (cursor > partitionEnd || cursor < partitionStart) {
            return null;
        }
        if (cursor == readerFailAt) {
            throw new ArithmeticException("Failing at reader.fail.at point " + readerFailAt);
        }
        final Integer result = data[cursor];
        cursor++;
        return result;
    }

    @Override
    protected void initData() {
        super.initData();
        for (int i = 0; i < dataCount; i++) {
            data[i] = i;
        }
        //position the cursor according to partition start
        cursor = partitionStart;
    }
}
