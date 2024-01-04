/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.aroundconstruct.simple;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author Stuart Douglas
 */
@Stateless
@Interceptors(AroundConstructInterceptor.class)
public class AroundConstructSLSB {

    private String message = "";


    public void append(String m) {
        message += m;
    }

    public String getMessage() {
        return message;
    }
}
