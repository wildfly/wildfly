/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inheritorder;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;

/**
 * @author Ondrej Chaloupka
 */
@Interceptors({ A1Interceptor.class, A2Interceptor.class })
public class AClass {
    public String run() {
        return AClass.class.getSimpleName();
    }

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext context) throws Exception {
        return AClass.class.getSimpleName() + ".method " + context.proceed().toString();
    }
}
