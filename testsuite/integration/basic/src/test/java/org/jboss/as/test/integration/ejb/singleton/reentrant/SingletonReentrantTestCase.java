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

package org.jboss.as.test.integration.ejb.singleton.reentrant;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.ejb.IllegalLoopbackException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing reentrant calls on Singleton for container-managed concurrency (EJB3.1 4.8.5.1.1)
 *
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class SingletonReentrantTestCase {
    private static final String ARCHIVE_NAME = "reentrant-test";
    private static final int WAITING_S = 5;

    @ArquillianResource
    private InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(SingletonReentrantTestCase.class.getPackage());
        // Needed for ThreadPoolExecutor.shutdown()
        jar.addAsManifestResource(createPermissionsXmlAsset(new RuntimePermission("modifyThread")), "permissions.xml");
        return jar;
    }

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(ctx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testWriteCall() throws Exception {
        final SingletonBean singleton = lookup(SingletonBean.class.getSimpleName(), SingletonBean.class);
        singleton.resetCalled();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        ExecutorService pool2 = Executors.newSingleThreadExecutor();
        final CountDownLatch waitForOtherOne = new CountDownLatch(1);
        CountDownLatch letsWait = new CountDownLatch(1);

        Future<?> firstOne = pool.submit(new SingletonCallableWrite(letsWait, waitForOtherOne));
        letsWait.await(WAITING_S, TimeUnit.SECONDS); //first thread has to be first in singleton method
        letsWait = new CountDownLatch(1);
        Future<?> otherOne = pool2.submit(new SingletonCallableWrite(letsWait, null));

        // first one could proceed - other one has to wait for end of work of first one
        waitForOtherOne.countDown();
        Assert.assertEquals(new Integer(1), firstOne.get(WAITING_S, TimeUnit.SECONDS));
        Assert.assertEquals(new Integer(2), otherOne.get(WAITING_S, TimeUnit.SECONDS));
        pool.shutdown();
        pool2.shutdown();
    }

    @Test
    public void testReadCall() throws Exception {
        final SingletonBean singleton = lookup(SingletonBean.class.getSimpleName(), SingletonBean.class);
        singleton.resetCalled();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        ExecutorService pool2 = Executors.newSingleThreadExecutor();
        final CountDownLatch oneWaiting = new CountDownLatch(1);
        final CountDownLatch twoWaiting = new CountDownLatch(1);

        Future<?> firstOne = pool.submit(new SingletonCallableRead(oneWaiting));
        Future<?> otherOne = pool2.submit(new SingletonCallableRead(twoWaiting));

        // first one could proceed - other one has to wait for end of work of first one
        oneWaiting.countDown();
        Assert.assertEquals(new Integer(1), firstOne.get(WAITING_S, TimeUnit.SECONDS));
        twoWaiting.countDown();
        Assert.assertEquals(new Integer(2), otherOne.get(WAITING_S, TimeUnit.SECONDS));

        // Expecting exception - calling reentrant write method with read lock
        try {
            firstOne = pool.submit(new SingletonCallableRead(null));
            firstOne.get();
            Assert.fail("Supposing " + IllegalLoopbackException.class.getName());
        } catch (IllegalLoopbackException ile) {
            // OK - supposed
        } catch (Exception e) {
            if (!hasCause(e, IllegalLoopbackException.class)) {
                Assert.fail("Supposed caused exception is " + IllegalLoopbackException.class.getName());
            }
        }

        pool.shutdown();
        pool2.shutdown();
    }


    private final class SingletonCallableWrite implements Callable<Integer> {
        private CountDownLatch latch;
        private CountDownLatch downMe;

        SingletonCallableWrite(CountDownLatch downMe, CountDownLatch latch) {
            this.latch = latch;
            this.downMe = downMe;
        }

        @Override
        public Integer call() {
            try {
                final SingletonBean singleton = lookup(SingletonBean.class.getSimpleName(), SingletonBean.class);
                return singleton.methodWithWriteLock(downMe, latch);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final class SingletonCallableRead implements Callable<Integer> {
        private CountDownLatch latch;

        SingletonCallableRead(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public Integer call() {
            try {
                final SingletonBean singleton = lookup(SingletonBean.class.getSimpleName(), SingletonBean.class);
                if (latch != null) {
                    return singleton.methodWithReadLock(latch);
                } else {
                    return singleton.methodWithReadLockException();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean hasCause(Throwable causeException, Class<? extends Throwable> searchedException) {
        Throwable currentException = causeException;
        do {
            if (currentException.getClass() == searchedException) {
                return true;
            }
            currentException = currentException.getCause();
        } while ((currentException != null));
        return false;
    }
}
