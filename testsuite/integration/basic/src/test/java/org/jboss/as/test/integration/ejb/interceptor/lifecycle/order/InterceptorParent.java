/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.order;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.InvocationContext;

import org.junit.Assert;

/**
 * @author Stuart Douglas
 */
public class InterceptorParent {

    public static boolean parentPostConstructCalled = false;

    @PostConstruct
    public void parent(InvocationContext ctx) throws Exception {
        parentPostConstructCalled = true;
        Assert.assertFalse(InterceptorChild.childPostConstructCalled);
        Assert.assertTrue(FirstInterceptor.postConstructCalled);
        Assert.assertFalse(LastInterceptor.postConstructCalled);
        Assert.assertFalse(SFSBParent.parentPostConstructCalled);
        Assert.assertFalse(SFSBChild.childPostConstructCalled);
        ctx.proceed();
    }

}
