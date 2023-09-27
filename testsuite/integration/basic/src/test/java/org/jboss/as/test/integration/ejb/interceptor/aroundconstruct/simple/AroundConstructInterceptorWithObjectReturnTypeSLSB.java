/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.aroundconstruct.simple;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author Dmitrii Tikhomirov
 */
@Stateless
@Interceptors(AroundConstructInterceptorWithObjectReturnType.class)
public class AroundConstructInterceptorWithObjectReturnTypeSLSB {

    private String message = "";


    public void append(String m) {
        message += m;
    }

    public String getMessage() {
        return message;
    }
}
