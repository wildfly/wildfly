/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.externalizer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Non-serializable counter, to be externalized via {@link CounterExternalizer}.
 * @author Paul Ferraro
 */
public class Counter {
    private final AtomicInteger count;

    public Counter(int count) {
        this.count = new AtomicInteger(count);
    }

    int getValue() {
        return this.count.get();
    }

    public int increment() {
        return this.count.incrementAndGet();
    }
}
