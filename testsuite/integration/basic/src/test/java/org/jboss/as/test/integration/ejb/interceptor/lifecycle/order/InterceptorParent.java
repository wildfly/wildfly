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
public class InterceptorParent {

    public static boolean parentPostConstructCalled = false;

    @PostConstruct
    public void parent(InvocationContext ctx) throws Exception {
        parentPostConstructCalled = true;
        Assertions.assertFalse(InterceptorChild.childPostConstructCalled);
        Assertions.assertTrue(FirstInterceptor.postConstructCalled);
        Assertions.assertFalse(LastInterceptor.postConstructCalled);
        Assertions.assertFalse(SFSBParent.parentPostConstructCalled);
        Assertions.assertFalse(SFSBChild.childPostConstructCalled);
        ctx.proceed();
    }

}
