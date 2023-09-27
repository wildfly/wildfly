/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.ejb.interceptor.interceptorsorderwithexclusions;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * @author Marius Bogoevici
 */
public class EjbInterceptor2 {
    static int count;

    @AroundInvoke
    public Object doInTransaction(InvocationContext invocationContext) throws Exception {
        count = Counter.next();
        return invocationContext.proceed();
    }
}
