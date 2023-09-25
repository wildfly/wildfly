/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.batchlet;

import java.util.concurrent.atomic.AtomicInteger;
import jakarta.enterprise.context.RequestScoped;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RequestScoped
public class RequestScopeCounter {
    private final AtomicInteger counter = new AtomicInteger();

    public int get() {
        return counter.get();
    }

    public int incrementAndGet() {
        return counter.incrementAndGet();
    }
}
