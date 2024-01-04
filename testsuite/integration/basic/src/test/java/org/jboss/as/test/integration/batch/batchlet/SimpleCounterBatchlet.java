/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.batchlet;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Named
public class SimpleCounterBatchlet extends AbstractBatchlet {
    @Inject
    private RequestScopeCounter counter;

    @Inject
    @BatchProperty
    private int count;

    @Override
    public String process() throws Exception {
        final StringBuilder exitStatus = new StringBuilder();
        int current = 0;
        while (current < count) {
            current = counter.incrementAndGet();
            exitStatus.append(current);
            if (current < count) {
                exitStatus.append(',');
            }
        }
        return exitStatus.toString();
    }
}
