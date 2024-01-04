/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.context.transactionscoped.auxiliary;

import static org.jboss.as.test.integration.messaging.jms.context.transactionscoped.TransactionScopedJMSContextTestCase.QUEUE_NAME;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateful;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.inject.Inject;
import jakarta.jms.JMSDestinationDefinition;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@JMSDestinationDefinition(
        name = QUEUE_NAME,
        interfaceName = "jakarta.jms.Queue",
        destinationName = "InjectedJMSContextTestCaseQueue"
)
@Stateful(passivationCapable = false)
public class ThreadLauncher {

    @Resource
    private ManagedThreadFactory threadFactory;
    @Inject
    private AppScopedBean bean;

    public void start(int numThreads, int numMessages) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(numThreads);
        //System.out.println("starting threads");
        for (int i = 0; i < numThreads; i++) {
            threadFactory.newThread(new SendRunnable(latch, numMessages)).start();
        }
        //System.out.println("start finished");

        latch.await(30, TimeUnit.SECONDS);
        //System.out.println("DONE");
    }

    private class SendRunnable implements Runnable {

        private final CountDownLatch latch;
        private final int numMessages;

        SendRunnable(CountDownLatch latch, int numMessages) {

            this.latch = latch;
            this.numMessages = numMessages;
        }

        public void run() {
            //System.out.println("starting to send");
            for (int i = 0; i < numMessages; i++) {
                bean.sendMessage();
            }
            //System.out.println("done sending");

            latch.countDown();
        }

    }

}
