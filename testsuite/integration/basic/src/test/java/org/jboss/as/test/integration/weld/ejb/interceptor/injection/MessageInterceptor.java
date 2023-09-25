/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.injection;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * EJB interceptor that uses CDI injection
 * @author Stuart Douglas
 */
public class MessageInterceptor {

    @Inject
    private Message message;

    @AroundInvoke
    public Object invoke(InvocationContext ctx) throws Exception {
        return ctx.proceed().toString() + message.getMessage();
    }

}
