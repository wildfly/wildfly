/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.transaction;

import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.inject.Named;
import java.util.List;

@Named
public class TransactedWriter extends AbstractItemWriter {
    @Override
    public void writeItems(List<Object> list) throws Exception {
        // do nothing
    }
}
