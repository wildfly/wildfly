/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

@Stateful
@Remote(Incrementor.class)
public class SlowStatefulIncrementorBean extends IncrementorBean {

    @Override
    public Result<Integer> increment() {
        delay();
        return super.increment();
    }

    @PostConstruct
    public void init() {
        delay();
    }

    private static void delay() {
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
