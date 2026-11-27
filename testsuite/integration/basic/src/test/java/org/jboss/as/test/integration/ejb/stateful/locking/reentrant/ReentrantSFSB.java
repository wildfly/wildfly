/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.locking.reentrant;

import java.io.Serializable;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;

/**
 * SFSB that reentrantly calls itself through its own EJB client proxy via
 * {@link SessionContext#getBusinessObject(Class)}, reproducing the scenario
 * from WFLY-22385.
 *
 * Each call to {@link #increment()} increments the counter and, unless the
 * counter is a multiple of 10, reentrantly calls itself again via the proxy.
 * This produces 10 total invocations (9 reentrant frames) before unwinding.
 */
@Stateful
public class ReentrantSFSB implements Serializable {

    private static final long serialVersionUID = 1L;

    @Resource
    private SessionContext sessionContext;

    private int count;

    /**
     * Mirrors the exact reproducer pattern from WFLY-22385:
     * increments the counter and recursively calls itself via the EJB proxy
     * until {@code count} is a multiple of 10.
     */
    public void increment() {
        this.count++;
        if (this.count % 10 == 0) {
            return;
        }
        sessionContext.getBusinessObject(ReentrantSFSB.class).increment();
    }

    public int getCount() {
        return count;
    }
}
