/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api.interceptor.annotation;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;

/**
 * A client interceptor which adds a value "bar" with key "foo" to the context data
 * @author Jan Martiska
 */
public class EJBClientInterceptorAddedByAnnotation implements EJBClientInterceptor {

    @Override
    public void handleInvocation(EJBClientInvocationContext context) throws Exception {
        context.getContextData().put("foo", "bar");
        context.sendRequest();
    }

    @Override
    public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
        return context.getResult();
    }
}
