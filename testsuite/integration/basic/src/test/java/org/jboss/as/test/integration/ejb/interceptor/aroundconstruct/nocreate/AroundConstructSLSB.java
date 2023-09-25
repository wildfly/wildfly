/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.aroundconstruct.nocreate;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author Stuart Douglas
 */
@Stateless
@Interceptors(AroundConstructInterceptor.class)
public class AroundConstructSLSB {

    public static volatile boolean constructed = false;

    public AroundConstructSLSB() {
        if(getClass() == AroundConstructSLSB.class) {
            constructed = true;
        }
    }

    private String message = "";


    public void append(String m) {
        message += m;
    }

    public String getMessage() {
        return message;
    }
}
