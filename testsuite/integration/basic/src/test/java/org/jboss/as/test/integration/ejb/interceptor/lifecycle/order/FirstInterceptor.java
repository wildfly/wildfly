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
public class FirstInterceptor {

    public static boolean postConstructCalled;

    @PostConstruct
    public void child(InvocationContext ctx) throws Exception {
        postConstructCalled = true;
        Assert.assertFalse(InterceptorParent.parentPostConstructCalled);
        Assert.assertFalse(InterceptorChild.childPostConstructCalled);
        Assert.assertFalse(LastInterceptor.postConstructCalled);
        Assert.assertFalse(SFSBParent.parentPostConstructCalled);
        Assert.assertFalse(SFSBChild.childPostConstructCalled);
        ctx.proceed();
    }

}
