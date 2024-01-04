/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import org.jboss.as.ee.concurrent.ConcurrentContext;

/**
 * A handle to propagate EE concurrency context to batch thread.
 */
class ConcurrentContextHandle implements ContextHandle {
    private final ConcurrentContext concurrentContext;

    ConcurrentContextHandle() {
        this.concurrentContext = ConcurrentContext.current();
    }

    @Override
    public Handle setup() {
        ConcurrentContext.pushCurrent(concurrentContext);

        return new Handle() {
            @Override
            public void tearDown() {
                ConcurrentContext.popCurrent();
            }
        };
    }
}
