/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.pool.strictmax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.pool.StatelessObjectFactory;
import org.jboss.as.ejb3.pool.common.MockBean;
import org.jboss.as.ejb3.pool.common.MockFactory;
import org.junit.Test;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class StrictMaxUnitTestCase {
    AtomicInteger used = new AtomicInteger(0);

    protected void setUp() throws Exception {
        MockBean.reset();
        used = new AtomicInteger(0);
    }

    @Test
    public void test1() {
        MockBean.reset();
        StatelessObjectFactory<MockBean> factory = new MockFactory();
        Pool<MockBean> pool = new StrictMaxPool<MockBean>(factory, 10, 1, TimeUnit.SECONDS);
        pool.start();

        MockBean[] beans = new MockBean[10];
        for (int i = 0; i < beans.length; i++) {
            beans[i] = pool.get();
        }

        for (int i = 0; i < beans.length; i++) {
            pool.release(beans[i]);
            beans[i] = null;
        }

        pool.stop();

        assertEquals(10, MockBean.getPostConstructs());
        assertEquals(10, MockBean.getPreDestroys());
    }

    /**
     * More threads than the pool size.
     */
    @Test
    public void testMultiThread() throws Exception {
        MockBean.reset();
        StatelessObjectFactory<MockBean> factory = new MockFactory();
        final Pool<MockBean> pool = new StrictMaxPool<MockBean>(factory, 10, 60, TimeUnit.SECONDS);
        pool.start();

        final CountDownLatch in = new CountDownLatch(1);
        final CountDownLatch ready = new CountDownLatch(10);



        Callable<Void> task = new Callable<Void>() {
            public Void call() throws Exception {
                MockBean bean = pool.get();
                ready.countDown();
                in.await();
                pool.release(bean);

                bean = null;

                used.incrementAndGet();

                return null;
            }
        };

        ExecutorService service = Executors.newFixedThreadPool(20);
        Future<?>[] results = new Future<?>[20];
        for (int i = 0; i < results.length; i++) {
            results[i] = service.submit(task);
        }

        ready.await(120, TimeUnit.SECONDS);
        in.countDown();

        for (Future<?> result : results) {
            result.get(5, TimeUnit.SECONDS);
        }

        service.shutdown();

        pool.stop();

        assertEquals(20, used.intValue());
        assertEquals(10, MockBean.getPostConstructs());
        assertEquals(10, MockBean.getPreDestroys());
    }

    @Test
    public void testTooMany() {
        MockBean.reset();
        StatelessObjectFactory<MockBean> factory = new MockFactory();
        Pool<MockBean> pool = new StrictMaxPool<MockBean>(factory, 10, 1, TimeUnit.SECONDS);
        pool.start();

        MockBean[] beans = new MockBean[10];
        for (int i = 0; i < beans.length; i++) {
            beans[i] = pool.get();
        }

        try {
            pool.get();
            fail("should have thrown an exception");
        } catch (Exception e) {
            assertEquals(EjbLogger.ROOT_LOGGER.failedToAcquirePermit(1, TimeUnit.SECONDS).getMessage(), e.getMessage());
        }

        for (int i = 0; i < beans.length; i++) {
            pool.release(beans[i]);
            beans[i] = null;
        }

        pool.stop();

        assertEquals(10, MockBean.getPostConstructs());
        assertEquals(10, MockBean.getPreDestroys());
    }
}
