/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.jpa;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class MyListenerInterceptor {

    public static boolean wasCalled = false;

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        wasCalled = true;

        // change entity's ID to 2 before persisting
        // Employee emp = (Employee) ctx.getParameters()[0];
        // emp.setId(emp.getId() + 1);

        return ctx.proceed();
    }
}
