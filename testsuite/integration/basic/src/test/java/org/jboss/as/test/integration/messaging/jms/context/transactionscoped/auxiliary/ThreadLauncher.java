/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.messaging.jms.context.transactionscoped.auxiliary;

import static org.jboss.as.test.integration.messaging.jms.context.transactionscoped.TransactionScopedJMSContextTestCase.QUEUE_NAME;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.Stateful;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.inject.Inject;
import javax.jms.JMSDestinationDefinition;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@JMSDestinationDefinition(
        name = QUEUE_NAME,
        interfaceName = "javax.jms.Queue",
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
