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

package org.jboss.as.test.integration.ejb.singleton.concurrency.inheritance;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.ejb.ConcurrentAccessTimeoutException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that methods that are overriden no not used the annotations of the methods that they override
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class SingletonConcurrencyInheritanceTestCase {

    private static final String ARCHIVE_NAME = "ConcurrencyTestCase";

    @Deployment
    public static Archive<?> deploy() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addPackage(SingletonConcurrencyInheritanceTestCase.class.getPackage());
        return war;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    /**
     * Test overriden method uses correct lock type
     */
    @Test
    public void testOverridenMethodNoAnnotation() throws Exception {
        final SingletonBaseBean singleton = lookup(SingletonChildBean.class.getSimpleName(), SingletonChildBean.class);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        final CountDownLatch latch = new CountDownLatch(2);
        final CountDownLatch entered = new CountDownLatch(1);
        //call a method with a write lock
        //this will block till we hit the latch
        Future<?> future = pool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    singleton.readLockOverriddenByParent(latch, entered);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        entered.await();
        try {
            singleton.readLockOverriddenByParent(latch, entered);
            throw new RuntimeException("Expecting a concurrency access exception");
        } catch (ConcurrentAccessTimeoutException e) {
            //expected
        }
        latch.countDown();
        future.get();
    }

    @Test
    public void testOverridenMethodWithAnnotation() throws Exception {
        final SingletonBaseBean singleton = lookup(SingletonChildBean.class.getSimpleName(), SingletonChildBean.class);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        final CountDownLatch latch = new CountDownLatch(2);
        final CountDownLatch entered = new CountDownLatch(1);
        //call a method with a write lock
        //this will block till we hit the latch
        Future<?> future = pool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    singleton.writeLockOverriddenByParent(latch, entered);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        entered.await();
        singleton.writeLockOverriddenByParent(latch, entered);
        future.get();
    }


    @Test
    public void testWriteLockMethodNotOverridden() throws Exception {
        final SingletonBaseBean singleton = lookup(SingletonChildBean.class.getSimpleName(), SingletonChildBean.class);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        final CountDownLatch latch = new CountDownLatch(2);
        final CountDownLatch entered = new CountDownLatch(1);
        //call a method with a write lock
        //this will block till we hit the latch
        Future<?> future = pool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    singleton.writeLock(latch, entered);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        entered.await();
        try {
            singleton.writeLock(latch, entered);
            throw new RuntimeException("Expecting a concurrency access exception");
        } catch (ConcurrentAccessTimeoutException e) {
            //expected
        }
        latch.countDown();
        future.get();
    }

    @Test
    public void testImplicitWriteLockMethodNotOverridden() throws Exception {
        final SingletonBaseBean singleton = lookup(SingletonChildBean.class.getSimpleName(), SingletonChildBean.class);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        final CountDownLatch latch = new CountDownLatch(2);
        final CountDownLatch entered = new CountDownLatch(1);
        //call a method with a write lock
        //this will block till we hit the latch
        Future<?> future = pool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    singleton.impliedWriteLock(latch, entered);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        entered.await();
        try {
            singleton.impliedWriteLock(latch, entered);
            throw new RuntimeException("Expecting a concurrency access exception");
        } catch (ConcurrentAccessTimeoutException e) {
            //expected
        }
        latch.countDown();
        future.get();
    }


    @Test
    public void testReadLockNotOverridden() throws Exception {
        final SingletonBaseBean singleton = lookup(SingletonChildBean.class.getSimpleName(), SingletonChildBean.class);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        final CountDownLatch latch = new CountDownLatch(2);
        final CountDownLatch entered = new CountDownLatch(1);
        //call a method with a write lock
        //this will block till we hit the latch
        Future<?> future = pool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    singleton.readLock(latch, entered);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        entered.await();
        singleton.readLock(latch, entered);
        future.get();
    }

    @Test
    public void testWritersBlockReader() throws Exception {
        final SingletonBaseBean singleton = lookup(SingletonChildBean.class.getSimpleName(), SingletonChildBean.class);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        final CountDownLatch latch = new CountDownLatch(2);
        final CountDownLatch entered = new CountDownLatch(1);
        //call a method with a write lock
        //this will block till we hit the latch
        Future<?> future = pool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    singleton.writeLock(latch, entered);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        entered.await();
        try {
            singleton.readLock(latch, entered);
            throw new RuntimeException("Expecting a concurrency access exception");
        } catch (ConcurrentAccessTimeoutException e) {
            //expected
        }
        latch.countDown();
        future.get();
    }


}
