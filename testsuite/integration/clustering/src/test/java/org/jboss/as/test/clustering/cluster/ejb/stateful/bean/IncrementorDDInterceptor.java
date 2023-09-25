/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.stateful.bean;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
@Intercepted
@Interceptor
public class IncrementorDDInterceptor implements Serializable {
    private static final long serialVersionUID = -3706191491067801021L;

    private final AtomicInteger count = new AtomicInteger(0);

    @AroundInvoke
    public Object invoke(final InvocationContext context) throws Exception {
        return ((Integer) context.proceed()) + this.count.addAndGet(10000);
    }
}
