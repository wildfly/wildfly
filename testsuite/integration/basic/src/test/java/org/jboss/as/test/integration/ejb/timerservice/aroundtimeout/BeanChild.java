/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.aroundtimeout;

import jakarta.ejb.Stateless;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
@Stateless
@Interceptors(InterceptorChild.class)
@Intercepted
public class BeanChild extends BeanParent {

    @AroundTimeout
    public Object aroundTimeoutChild(final InvocationContext context) throws Exception {
        InterceptorOrder.intercept(BeanChild.class);
        return context.proceed();
    }

}
