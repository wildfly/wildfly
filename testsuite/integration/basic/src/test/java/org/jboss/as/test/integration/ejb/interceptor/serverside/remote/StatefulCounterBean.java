/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.serverside.remote;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

@Stateful
@Remote(SimpleCounter.class)
public class StatefulCounterBean implements SimpleCounter {

    private int count = 0;

    @Override
    public String getSimpleName() {
        return StatefulCounterBean.class.getSimpleName();
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public void increment() {
        count++;
    }

    @Override
    public void decrement() {
        count--;
    }
}
