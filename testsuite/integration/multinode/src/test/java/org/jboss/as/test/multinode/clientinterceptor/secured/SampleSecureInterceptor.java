/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.clientinterceptor.secured;

import java.util.concurrent.CountDownLatch;
import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;

public class SampleSecureInterceptor implements EJBClientInterceptor {
    public static final CountDownLatch latch = new CountDownLatch(4);


    @Override
    public void handleInvocation(EJBClientInvocationContext context) throws Exception {
        latch.countDown();
        context.sendRequest();
    }

    @Override
    public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
        latch.countDown();
        return context.getResult();
    }
}
