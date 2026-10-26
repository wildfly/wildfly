/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.order;

import jakarta.annotation.PostConstruct;

import org.junit.jupiter.api.Assertions;

/**
 * @author Stuart Douglas
 */
public class SFSBParent {

    public static boolean parentPostConstructCalled = false;

    @PostConstruct
    public void parent() {
        parentPostConstructCalled = true;
        Assertions.assertTrue(InterceptorChild.childPostConstructCalled);
        Assertions.assertTrue(InterceptorParent.parentPostConstructCalled);
        Assertions.assertFalse(SFSBChild.childPostConstructCalled);
    }

}
