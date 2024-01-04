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
public class BClass extends AClass {
    @Override
    @Interceptors({ BMethodInterceptor.class })
    // won't be invoked - overriden in CClass
    public String run() {
        return super.run() + BClass.class.getSimpleName();
    }

    @AroundInvoke
    // override aroundinvoke method of AClass - only this will be run
    public Object aroundInvoke(final InvocationContext context) throws Exception {
        return BClass.class.getSimpleName() + ".method " + context.proceed().toString();
    }
}
