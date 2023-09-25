/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.aroundtimeout;

import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
public class MethodInterceptorParent {

    @AroundTimeout
    public Object aroundTimeout(final InvocationContext context) throws Exception {
        InterceptorOrder.intercept(MethodInterceptorParent.class);
        return  context.proceed();
    }

}
