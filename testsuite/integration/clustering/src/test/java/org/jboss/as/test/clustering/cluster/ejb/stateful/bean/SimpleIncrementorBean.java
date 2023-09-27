/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.stateful.bean;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Resource;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;

@Stateful
public class SimpleIncrementorBean implements Incrementor {
    @Resource
    private SessionContext context;

    private final AtomicInteger count = new AtomicInteger(0);
    private transient boolean activated = false;
    private boolean passivated = false;

    @Override
    public int increment() {
        if (this.count.get() > 0) {
            assert this.passivated;
            assert this.activated;
            this.passivated = false;
        }
        return this.count.incrementAndGet();
    }

    @PrePassivate
    public void prePassivate() {
        this.passivated = true;
    }

    @PostActivate
    public void postActivate() {
        this.activated = true;
    }
}
