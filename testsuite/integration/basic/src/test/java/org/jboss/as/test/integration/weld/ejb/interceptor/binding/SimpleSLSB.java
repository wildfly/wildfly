/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.binding;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

import org.junit.jupiter.api.Assertions;

/**
 * @author Stuart Douglas
 */
@Stateless
@Binding
@Interceptors(EjbInterceptor.class)
public class SimpleSLSB {

    public String sayHello() {
        Assertions.assertTrue(CdiInterceptor.invoked);
        Assertions.assertTrue(EjbInterceptor.invoked);
        return "Hello";
    }
}
