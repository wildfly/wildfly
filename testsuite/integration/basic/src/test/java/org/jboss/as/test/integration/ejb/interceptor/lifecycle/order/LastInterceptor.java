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
public class LastInterceptor {

    public static boolean postConstructCalled;

    @PostConstruct
    public void child(InvocationContext ctx) throws Exception {
        postConstructCalled = true;
        Assert.assertTrue(InterceptorParent.parentPostConstructCalled);
        Assert.assertTrue(InterceptorChild.childPostConstructCalled);
        Assert.assertTrue(FirstInterceptor.postConstructCalled);
        Assert.assertFalse(SFSBParent.parentPostConstructCalled);
        Assert.assertFalse(SFSBChild.childPostConstructCalled);
        ctx.proceed();
    }

}
