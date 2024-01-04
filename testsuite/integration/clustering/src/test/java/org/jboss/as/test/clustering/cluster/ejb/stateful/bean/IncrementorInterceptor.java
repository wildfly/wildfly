/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.stateful.bean;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
public class IncrementorInterceptor implements Serializable {
    private static final long serialVersionUID = 2147419195941582392L;

    private final AtomicInteger count = new AtomicInteger(0);

    @AroundInvoke
    public Object invoke(final InvocationContext context) throws Exception {
        return ((Integer) context.proceed()) + this.count.addAndGet(100);
    }
}
