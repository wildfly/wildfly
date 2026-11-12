/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.order;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateful;
import jakarta.interceptor.Interceptors;

import org.junit.jupiter.api.Assertions;

/**
 * @author Stuart Douglas
 */
@Stateful(passivationCapable = false)
@Interceptors({ FirstInterceptor.class, InterceptorChild.class, LastInterceptor.class })
public class SFSBChild extends SFSBParent {

    public static boolean childPostConstructCalled = false;

    @PostConstruct
    public void child() {
        childPostConstructCalled = true;
        Assertions.assertTrue(SFSBParent.parentPostConstructCalled);
        Assertions.assertTrue(InterceptorParent.parentPostConstructCalled);
        Assertions.assertTrue(InterceptorChild.childPostConstructCalled);
    }

    public void doStuff() {
    }

}
