/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.order;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.InvocationContext;

import org.junit.Assert;

/**
 *
 * @author Stuart Douglas
 */
public class InterceptorChild extends InterceptorParent {

    public static boolean childPostConstructCalled;

    @PostConstruct
    public void child(InvocationContext ctx) throws Exception {
        Assert.assertTrue(InterceptorParent.parentPostConstructCalled);
        Assert.assertTrue(FirstInterceptor.postConstructCalled);
        Assert.assertFalse(LastInterceptor.postConstructCalled);
        Assert.assertFalse(SFSBParent.parentPostConstructCalled);
        Assert.assertFalse(SFSBChild.childPostConstructCalled);
        childPostConstructCalled = true;
        ctx.proceed();
    }

}
