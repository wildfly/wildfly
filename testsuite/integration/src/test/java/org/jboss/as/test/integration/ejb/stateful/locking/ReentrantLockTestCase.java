/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.locking;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

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
