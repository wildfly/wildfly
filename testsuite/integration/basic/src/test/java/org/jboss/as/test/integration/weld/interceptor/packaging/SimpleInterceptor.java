/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.interceptor.packaging;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
@Intercepted
@Interceptor
public class SimpleInterceptor {


    public static final String POST_CONSTRUCT_MESSAGE = "Post Const Intercepted";

    @PostConstruct
    public void postConstruct(InvocationContext context) {
        if(context.getTarget() instanceof SimpleEjb2) {
            ((SimpleEjb2)context.getTarget()).setPostConstructMessage(POST_CONSTRUCT_MESSAGE);
        }
    }

    @AroundInvoke
    public Object invoke(final InvocationContext context) throws Exception {
        return context.proceed() + " World";
    }
}
