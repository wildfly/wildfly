/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.binding;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.junit.Assert;

/**
 * @author Stuart Douglas
 */
@Binding
@Interceptor
public class CdiInterceptor {

    public static boolean invoked = false;

    private String message = "";

    @PostConstruct
    public void postConstruct(InvocationContext context) {
        message = " World";
    }
    @AroundInvoke
    public Object invoke(InvocationContext ctx) throws Exception{
        invoked = true;
        Assert.assertTrue(EjbInterceptor.invoked);
        return ctx.proceed() + message;
    }

}
