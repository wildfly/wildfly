/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.jndi;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

/**
 * @author Jaikiran Pai
 */
@Stateful
@Remote(RemoteCounter.class)
public class CounterBean implements RemoteCounter {

    private int count = 0;

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public void incrementCount() {
        count++;
    }

    @Override
    public void decrementCount() {
        count--;
    }
}
