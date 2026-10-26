/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.order;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Assertions;

/**
 *
 * @author Stuart Douglas
 */
public class InterceptorChild extends InterceptorParent {

    public static boolean childPostConstructCalled;

    @PostConstruct
    public void child(InvocationContext ctx) throws Exception {
        Assertions.assertTrue(InterceptorParent.parentPostConstructCalled);
        Assertions.assertTrue(FirstInterceptor.postConstructCalled);
        Assertions.assertFalse(LastInterceptor.postConstructCalled);
        Assertions.assertFalse(SFSBParent.parentPostConstructCalled);
        Assertions.assertFalse(SFSBChild.childPostConstructCalled);
        childPostConstructCalled = true;
        ctx.proceed();
    }

}
