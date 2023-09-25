/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.packaging;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
@CdiInterceptorBinding
@Interceptor
public class CdiInterceptor {

    @AroundInvoke
    public Object invoke(final InvocationContext context ) throws Exception {
        return  context.proceed().toString() + " World";
    }

}
