/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inheritorder;

import jakarta.ejb.Stateless;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;

/**
 * @author Ondrej Chaloupka
 */
@Stateless
@Interceptors(CChildInterceptor.class)
public class CClass extends BClass {

    @Override
    @Interceptors({ CMethodInterceptor.class })
    public String run() {
        return super.run() + CClass.class.getSimpleName();
    }

    @AroundInvoke
    // does not override aroundinvoke method of BClass - both will be run
    public Object aroundInvokeCClass(final InvocationContext context) throws Exception {
        return CClass.class.getSimpleName() + ".method " + context.proceed().toString();
    }
}
