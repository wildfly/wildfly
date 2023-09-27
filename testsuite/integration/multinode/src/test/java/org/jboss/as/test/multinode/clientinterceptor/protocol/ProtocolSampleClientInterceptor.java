/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.clientinterceptor.protocol;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;

public class ProtocolSampleClientInterceptor implements EJBClientInterceptor {
    static final int COUNT = 10;

    @Override
    public void handleInvocation(EJBClientInvocationContext context) throws Exception {
        context.sendRequest();
    }

    @Override
    public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
        Object result = context.getResult();
        if (result instanceof Integer) {
            return COUNT + (int) result;
        }
        return result;
    }
}
