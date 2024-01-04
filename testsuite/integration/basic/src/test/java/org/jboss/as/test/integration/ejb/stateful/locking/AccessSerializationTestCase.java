/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.locking;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.naming.InitialContext;

import org.junit.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that multiple calls to a SFSB are serialized correctly in both the presence and the absence of a transaction
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class AccessSerializationTestCase {

    private static final String ARCHIVE_NAME = "AccessSerializationTestCase";

    private static final int NUM_THREADS = 10;
    public static final int SUM_AMOUNT = 10;

    @ArquillianResource
    private InitialContext initialContext;

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(AccessSerializationTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void testConcurrentAccessTransaction() throws Exception {
        ConcurrencySFSB sfsb = (ConcurrencySFSB)initialContext.lookup("java:module/" + ConcurrencySFSB.class.getSimpleName() );
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
        Future[] results = new Future[NUM_THREADS];
        for(int i = 0; i < NUM_THREADS; ++i) {
            results[i] = executorService.submit(new CallingClassTransaction(sfsb));
        }
        for(int i = 0; i < NUM_THREADS; ++i) {
            results[i].get();
        }
        Assert.assertEquals(NUM_THREADS * SUM_AMOUNT, sfsb.getCounter());
    }

    @Test
    public void testConcurrentAccessNoTransaction() throws Exception {
        ConcurrencySFSB sfsb = (ConcurrencySFSB)initialContext.lookup("java:module/" + ConcurrencySFSB.class.getSimpleName() );
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
        Future[] results = new Future[NUM_THREADS];
        for(int i = 0; i < NUM_THREADS; ++i) {
            results[i] = executorService.submit(new CallingClassNoTransaction(sfsb));
        }
        for(int i = 0; i < NUM_THREADS; ++i) {
            results[i].get();
        }
        Assert.assertEquals(NUM_THREADS * SUM_AMOUNT, sfsb.getCounter());
    }

    private class CallingClassTransaction implements Runnable {

        private final ConcurrencySFSB sfsb;

        private CallingClassTransaction(final ConcurrencySFSB sfsb) {
            this.sfsb = sfsb;
        }

        @Override
        public void run() {
            try {
                sfsb.addTx(SUM_AMOUNT);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }

    private class CallingClassNoTransaction implements Runnable {

        private final ConcurrencySFSB sfsb;

        private CallingClassNoTransaction(final ConcurrencySFSB sfsb) {
            this.sfsb = sfsb;
        }

        @Override
        public void run() {
            try {
                sfsb.addNoTx(SUM_AMOUNT);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }

}
