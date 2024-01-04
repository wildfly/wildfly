/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.stateful.bean;

import java.io.Serializable;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;

/**
 * @author Stuart Douglas
 */
@Decorator
public class CounterDecorator implements Serializable, Counter {
    private static final long serialVersionUID = -3924798173306389949L;

    @Inject
    @Delegate
    private Counter counter;

    @Override
    public int getCount() {
        return this.counter.getCount() + 10000000;
    }
}
