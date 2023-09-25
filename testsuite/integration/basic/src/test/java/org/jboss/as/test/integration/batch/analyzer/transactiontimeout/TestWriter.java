/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.analyzer.transactiontimeout;

import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.inject.Named;

@Named
public class TestWriter extends AbstractItemWriter {
    public void writeItems(java.util.List<Object> items) throws Exception {
        // do nothing - if the items are delivered to writer test passes
    }
}
