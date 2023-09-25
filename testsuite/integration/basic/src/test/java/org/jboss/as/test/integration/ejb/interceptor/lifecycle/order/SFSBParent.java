/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.order;

import jakarta.annotation.PostConstruct;

import org.junit.Assert;

/**
 * @author Stuart Douglas
 */
public class SFSBParent {

    public static boolean parentPostConstructCalled = false;

    @PostConstruct
    public void parent() {
        parentPostConstructCalled = true;
        Assert.assertTrue(InterceptorChild.childPostConstructCalled);
        Assert.assertTrue(InterceptorParent.parentPostConstructCalled);
        Assert.assertFalse(SFSBChild.childPostConstructCalled);
    }

}
