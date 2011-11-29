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

package org.jboss.as.test.integration.ejb.async;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**
 * Tests that a simple async annotation works.
 * Enhanced test by migration [JIRA JBQA-5483].
 * 
 * @author Stuart Douglas, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class AsyncMethodTestCase {
    private static final Logger log = Logger.getLogger(AsyncMethodTestCase.class);
    private static final String ARCHIVE_NAME = "AsyncTestCase";

    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(AsyncMethodTestCase.class.getPackage());
        log.info(jar.toString(true));
        return jar;
    }

    protected <T> T lookup(Class<T> beanType) throws NamingException {
        return beanType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanType.getSimpleName() + "!"
                + beanType.getName()));
    }

    // Stateless
    @Test
    public void testVoidAsyncStatelessMethod() throws Exception {
        AsyncBean bean = lookup(AsyncBean.class);
        Assert.assertFalse(AsyncBean.voidMethodCalled);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        bean.asyncMethod(latch, latch2);
        latch.countDown();
        latch2.await();
        Assert.assertTrue(AsyncBean.voidMethodCalled);
    }

    @Test
    public void testFutureAsyncStatelessMethod() throws Exception {
        AsyncBean bean = lookup(AsyncBean.class);
        final CountDownLatch latch = new CountDownLatch(1);
        final Future<Boolean> future = bean.futureMethod(latch);
        latch.countDown();
        boolean result = future.get();
        Assert.assertTrue(AsyncBean.futureMethodCalled);
        Assert.assertTrue(result);
    }

    // Stateful
    @Test
    public void testVoidAsyncStatefulMethod() throws Exception {
        AsyncStateful bean = lookup(AsyncStateful.class);
        Assert.assertFalse(AsyncStateful.voidMethodCalled);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        bean.asyncMethod(latch, latch2);
        latch.countDown();
        latch2.await();
        Assert.assertTrue(AsyncStateful.voidMethodCalled);
    }

    @Test
    public void testFutureAsyncStatefulMethod() throws Exception {
        AsyncStateful bean = lookup(AsyncStateful.class);
        final CountDownLatch latch = new CountDownLatch(1);
        final Future<Boolean> future = bean.futureMethod(latch);
        latch.countDown();
        boolean result = future.get();
        Assert.assertTrue(AsyncStateful.futureMethodCalled);
        Assert.assertTrue(result);
    }    
    
    // Singleton
    @Test
    public void testVoidAsyncSingletonMethod() throws Exception {
        AsyncSingleton singleton = lookup(AsyncSingleton.class);
        Assert.assertFalse(AsyncSingleton.voidMethodCalled);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        singleton.asyncMethod(latch, latch2);
        latch.countDown();
        latch2.await();
        Assert.assertTrue(AsyncSingleton.voidMethodCalled);
    }

    @Test
    public void testFutureAsyncSingletonMethod() throws Exception {
        AsyncSingleton singleton = lookup(AsyncSingleton.class);
        final CountDownLatch latch = new CountDownLatch(1);
        final Future<Boolean> future = singleton.futureMethod(latch);
        latch.countDown();
        boolean result = future.get();
        Assert.assertTrue(AsyncSingleton.futureMethodCalled);
        Assert.assertTrue(result);
    }
}
