/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

/**
 * User: jpai
 */
@Stateful
@Remote (Counter.class)
public class CounterBean implements Counter {

    private int count;

    @PostConstruct
    private void onConstruct() {
        this.count = 0;
    }

    @Override
    public int incrementAndGetCount() {
        this.count ++;
        return this.count;
    }

    @Override
    public int getCount() {
        return this.count;
    }
}
