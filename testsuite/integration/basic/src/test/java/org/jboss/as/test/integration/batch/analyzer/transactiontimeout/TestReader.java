/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.analyzer.transactiontimeout;

import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.inject.Named;

@Named
public class TestReader extends AbstractItemReader {

    private int numItems = 10;

    @Override
    public Object readItem() throws Exception {
        if (numItems > 0) {
            numItems--;
            Thread.sleep(1000);
            return numItems;
        }
        return null;
    }
}
