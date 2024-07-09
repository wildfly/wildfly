/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.multipleinterceptors;

import jakarta.annotation.Resource;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
public class MyInterceptor {

    /**
     * This should create a binding for java:module/env/org.jboss.as.test.integration.injection.resource.multiple.MyInterceptor/simpleStatelessBean
     */
    @Resource(lookup="java:module/simpleStatelessBean")
    private SimpleStatelessBean simpleStatelessBean;

    @AroundInvoke
    public Object intercept(InvocationContext context ) throws Exception {
        return context.proceed();
    }
}
