/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.binding;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Assertions;

/**
 * A normal non-cdi interceptor. This must be called before CDI interceptors
 *
 * @author Stuart Douglas
 */
public class EjbInterceptor {

    public static boolean invoked = false;

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        try {
            invoked = true;
            Assertions.assertFalse(CdiInterceptor.invoked);
            return context.proceed() +" Ejb";
        } finally {
            Assertions.assertTrue(CdiInterceptor.invoked);
        }
    }
}
