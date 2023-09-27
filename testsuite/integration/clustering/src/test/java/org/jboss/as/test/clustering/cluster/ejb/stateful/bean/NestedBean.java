/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.stateful.bean;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ejb.Stateful;
import jakarta.inject.Inject;

/**
 * @author Paul Ferraro
 */
@Stateful
public class NestedBean implements Nested {

    private final AtomicInteger count = new AtomicInteger(0);

    @Inject
    private Counter counter;

    @Override
    public int increment() {
        return this.count.incrementAndGet() + this.counter.getCount();
    }
}
