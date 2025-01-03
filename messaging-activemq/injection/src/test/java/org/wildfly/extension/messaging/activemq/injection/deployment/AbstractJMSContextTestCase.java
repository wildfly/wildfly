/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.injection.deployment;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Test class for AbstractJMSContextTes.
 *
 * @author <a href="http://fnbrandao.com.br/">Fabio Nascimento Brand&atilde;o</a> (c) 2021 Red Hat inc.
 */
public class AbstractJMSContextTestCase {

    @Test
    public void testGetContext() throws InterruptedException {
        AbstractJMSContext abstractJMSContext = new AbstractJMSContext() {
        };

        LongAdder adder = new LongAdder();

        // Mock info to force the context creation by createContext(sessionMode)
        JMSInfo info = Mockito.mock(JMSInfo.class);
        Mockito.when(info.getUserName()).thenReturn(null);

        // Mock the create context
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        Mockito.when(connectionFactory.createContext(0)).thenAnswer(new Answer<JMSContext>() {

            @Override
            public JMSContext answer(InvocationOnMock invocation) throws Throwable {
                // Count how many contexts were created
                adder.increment();
                // "Slow" processing
                Thread.sleep(1000);
                return Mockito.mock(JMSContext.class);
            }

        });

        // 100 threads will be trying to create contexts at the same time
        ExecutorService executor = Executors.newFixedThreadPool(100);
        List<Callable<JMSContext>> tasks = new LinkedList<>();
        for (int i = 0; i < 100; i++) {
            final int j = i;
            tasks.add(new Callable<JMSContext>() {

                @Override
                public JMSContext call() throws Exception {
                    // We will only use 10 differents injectionPointId's
                    final String injectionPointId = Integer.toString(j % 10);
                    return abstractJMSContext.getContext(injectionPointId, info, connectionFactory);
                }
            });
        }

        // Execute all tasks
        executor.invokeAll(tasks);

        // Shutdown the executor
        executor.shutdown();
        // Ok, lets wait for only 2 seconds.
        executor.awaitTermination(2, TimeUnit.SECONDS);
        // If the executor is terminated, we were able to create 10 contexts at the same time.
        // When using synchronized (old implementation) we need to wait for 10 seconds to create 10 contexts (serializable creation).
        Assert.assertTrue(executor.isTerminated());
        // Lets check if we only created 10 contexts.
        Assert.assertEquals(10, adder.intValue());
    }

}
