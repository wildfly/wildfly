/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.stateful.bean;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class IncrementorBean implements Incrementor {

    private final AtomicInteger count = new AtomicInteger();

    @Override
    public Result<Integer> increment() {
        return new Result<>(this.count.incrementAndGet());
    }
}
