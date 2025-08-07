/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.stateful.bean;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Stateful;
import jakarta.ejb.StatefulTimeout;

@Stateful
@StatefulTimeout(value = 2, unit = TimeUnit.SECONDS)
public class TimeoutIncrementorBean implements Incrementor {
    private final AtomicInteger count = new AtomicInteger(0);
    private volatile boolean active = false;

    @Override
    public int increment() {
        return this.count.incrementAndGet();
    }

    @PostActivate
    public void postActivate() {
        this.active = true;
    }

    @PrePassivate
    public void prePassivate() {
        this.active = false;
    }

    @PostConstruct
    public void postConstruct() {
        this.active = true;
    }

    @PreDestroy
    public void preDestroy() {
        if (!this.active) {
            throw new IllegalStateException("@PostActivate listener not triggered!");
        }
    }
}
