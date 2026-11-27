/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.order;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Assertions;

/**
 * @author Stuart Douglas
 */
public class LastInterceptor {

    public static boolean postConstructCalled;

    @PostConstruct
    public void child(InvocationContext ctx) throws Exception {
        postConstructCalled = true;
        Assertions.assertTrue(InterceptorParent.parentPostConstructCalled);
        Assertions.assertTrue(InterceptorChild.childPostConstructCalled);
        Assertions.assertTrue(FirstInterceptor.postConstructCalled);
        Assertions.assertFalse(SFSBParent.parentPostConstructCalled);
        Assertions.assertFalse(SFSBChild.childPostConstructCalled);
        ctx.proceed();
    }

}
