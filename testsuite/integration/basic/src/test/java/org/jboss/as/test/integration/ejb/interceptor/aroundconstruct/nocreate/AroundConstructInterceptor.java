/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.aroundconstruct.nocreate;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
public class AroundConstructInterceptor {

    @AroundConstruct
    private void aroundConstrct(InvocationContext ctx) throws Exception {
    }


    @AroundInvoke
    private Object aroundInvoke(InvocationContext ctx) throws Exception {
        return "Intercepted";
    }

}
