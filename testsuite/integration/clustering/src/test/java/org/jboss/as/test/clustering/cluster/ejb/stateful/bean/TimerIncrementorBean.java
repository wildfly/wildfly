/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.stateful.bean;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateful;
import jakarta.ejb.TimerService;

@Stateful
public class TimerIncrementorBean implements Incrementor {

    @Resource
    private TimerService service;

    private final AtomicInteger count = new AtomicInteger(0);

    @Override
    public int increment() {
        return this.count.incrementAndGet();
    }
}
