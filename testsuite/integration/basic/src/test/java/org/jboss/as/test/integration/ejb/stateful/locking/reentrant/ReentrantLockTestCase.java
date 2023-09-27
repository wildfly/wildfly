/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.locking.reentrant;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.UserTransaction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Tests that multiple calls to a SFSB in the same TX release the lock correctly.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class ReentrantLockTestCase {

    private static final String ARCHIVE_NAME = "ReentrantLockTestCase";

    private static InitialContext iniCtx;

    @Inject
    private UserTransaction userTransaction;

    @Inject
    private SimpleSFSB simpleSFSB;

    private static final int NUM_THREADS = 5;

    @BeforeClass
    public static void beforeClass() throws NamingException {
        iniCtx = new InitialContext();
    }

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(ReentrantLockTestCase.class.getPackage());
        jar.add(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        return jar;
    }

    @Test
    public void testStatefulTimeoutFromAnnotation() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
        Future[] results = new Future[NUM_THREADS];
        for(int i = 0; i < NUM_THREADS; ++i) {
            results[i] = executorService.submit(new CallingClass());
        }

        for(int i = 0; i < NUM_THREADS; ++i) {
            results[i].get();
        }
    }


    private class CallingClass implements Runnable {


        @Override
        public void run() {
            try {
                userTransaction.begin();
                simpleSFSB.doStuff();
                simpleSFSB.doStuff();
                userTransaction.commit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }



}
