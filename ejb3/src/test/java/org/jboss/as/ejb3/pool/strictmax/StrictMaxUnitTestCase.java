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

import junit.framework.TestCase;
import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.pool.StatelessObjectFactory;
import org.jboss.as.ejb3.pool.common.MockBean;
import org.jboss.as.ejb3.pool.common.MockFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class StrictMaxUnitTestCase extends TestCase {
    AtomicInteger used = new AtomicInteger(0);

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        MockBean.reset();
        used = new AtomicInteger(0);
    }

    public void test1() {
        StatelessObjectFactory<MockBean> factory = new MockFactory();
        Pool<MockBean> pool = new StrictMaxPool<MockBean>(factory, 10, 1, TimeUnit.SECONDS);
        pool.start();

        MockBean beans[] = new MockBean[10];
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
    public void testMultiThread() throws Exception {
        StatelessObjectFactory<MockBean> factory = new MockFactory();
        final Pool<MockBean> pool = new StrictMaxPool<MockBean>(factory, 10, 1, TimeUnit.SECONDS);
        pool.start();

        Callable<Void> task = new Callable<Void>() {
            public Void call() throws Exception {
                for (int i = 0; i < 20; i++) {
                    MockBean bean = pool.get();

                    Thread.sleep(50);

                    pool.release(bean);

                    bean = null;

                    used.incrementAndGet();
                }

                return null;
            }
        };

        ExecutorService service = Executors.newFixedThreadPool(20);
        Future<?> results[] = new Future<?>[20];
        for (int i = 0; i < results.length; i++) {
            results[i] = service.submit(task);
        }

        for (Future<?> result : results) {
            result.get(5, TimeUnit.SECONDS);
        }

        service.shutdown();

        pool.stop();

        assertEquals(400, used.intValue());
        assertEquals(10, MockBean.getPostConstructs());
        assertEquals(10, MockBean.getPreDestroys());
    }

    public void testTooMany() {
        StatelessObjectFactory<MockBean> factory = new MockFactory();
        Pool<MockBean> pool = new StrictMaxPool<MockBean>(factory, 10, 1, TimeUnit.SECONDS);
        pool.start();

        MockBean beans[] = new MockBean[10];
        for (int i = 0; i < beans.length; i++) {
            beans[i] = pool.get();
        }

        try {
            pool.get();
            fail("should have thrown an exception");
        } catch (Exception e) {
            assertEquals("Failed to acquire a permit within 1 SECONDS", e.getMessage());
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
