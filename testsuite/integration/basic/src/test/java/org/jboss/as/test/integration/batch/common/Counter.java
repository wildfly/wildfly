/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Named
@Singleton
public class Counter implements Serializable {

    private final AtomicInteger counter = new AtomicInteger(0);

    public int increment() {
        return counter.incrementAndGet();
    }

    public int increment(final int i) {
        return counter.addAndGet(i);
    }

    public int get() {
        return counter.get();
    }
}
