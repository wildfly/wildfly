/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.binding;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

import org.junit.Assert;

/**
 * @author Stuart Douglas
 */
@Stateless
@Binding
@Interceptors(EjbInterceptor.class)
public class SimpleSLSB {

    public String sayHello() {
        Assert.assertTrue(CdiInterceptor.invoked);
        Assert.assertTrue(EjbInterceptor.invoked);
        return "Hello";
    }
}
