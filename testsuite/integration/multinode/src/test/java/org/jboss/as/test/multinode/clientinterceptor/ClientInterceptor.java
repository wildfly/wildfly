/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.clientinterceptor;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;

import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

public class ClientInterceptor implements EJBClientInterceptor {

    public static CountDownLatch invocationLatch = new CountDownLatch(1);
    public static CountDownLatch resultLatch = new CountDownLatch(1);

    @Override
    public void handleInvocation(EJBClientInvocationContext context) throws Exception {
        invocationLatch.countDown();
        context.sendRequest();
    }

    @Override
    public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
        resultLatch.countDown();
        return context.getResult();
    }
}
