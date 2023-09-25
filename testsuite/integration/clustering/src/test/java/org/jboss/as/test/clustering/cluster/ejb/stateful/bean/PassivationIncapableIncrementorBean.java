/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.stateful.bean;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Stateful;

@Stateful(passivationCapable = false)
public class PassivationIncapableIncrementorBean implements Incrementor {

    private final AtomicInteger count = new AtomicInteger(0);
    private boolean passivated = false;
    private transient boolean activated = false;

    @Override
    public int increment() {
        assert !this.passivated;
        assert !this.activated;
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
