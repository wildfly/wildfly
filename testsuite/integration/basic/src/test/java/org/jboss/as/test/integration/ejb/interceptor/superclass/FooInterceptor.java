/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.superclass;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class FooInterceptor {

    public static boolean invoked = false;

    @AroundInvoke
    public Object process(InvocationContext inv) throws Exception {
        invoked = true;
        return null;
    }

}
