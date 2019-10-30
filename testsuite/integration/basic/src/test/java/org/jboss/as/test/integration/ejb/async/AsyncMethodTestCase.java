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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that a simple async annotation works.
 * Enhanced test by migration [ JIRA JBQA-5483 ].
 *
 * @author Stuart Douglas, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class AsyncMethodTestCase {
    private static final String ARCHIVE_NAME = "AsyncTestCase";
    private static final Integer WAIT_TIME_S = 10;

    @ArquillianResource
    private InitialContext iniCtx;

    @ContainerResource
    private InitialContext remoteContext;

    @Deployment(name = "asynctest")
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(AsyncMethodTestCase.class.getPackage());
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.addAsManifestResource(AsyncMethodTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    protected <T> T lookup(Class<T> beanType) throws NamingException {
        return beanType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanType.getSimpleName() + "!"
                + beanType.getName()));
    }

    /**
     * Stateless - void returned
     */
    @Test
    public void testVoidAsyncStatelessMethod() throws Exception {
        AsyncBean.voidMethodCalled = false;
        AsyncBean bean = lookup(AsyncBean.class);
        Assert.assertFalse(AsyncBean.voidMethodCalled);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        bean.asyncMethod(latch, latch2);
        latch.countDown();
        latch2.await();
        Assert.assertTrue(AsyncBean.voidMethodCalled);
    }

    /**
     * Stateless - future returned
     */
    @Test
    public void testFutureAsyncStatelessMethod() throws Exception {
        AsyncBean.futureMethodCalled = false;
        AsyncBean bean = lookup(AsyncBean.class);
        final CountDownLatch latch = new CountDownLatch(1);
        final Future<Boolean> future = bean.futureMethod(latch);
        latch.countDown();
        boolean result = future.get();
        Assert.assertTrue(AsyncBean.futureMethodCalled);
        Assert.assertTrue(result);
    }

    /**
     * Stateless request scope
     */
    @Test
    public void testRequestScopeActive() throws Exception {
        AsyncBean bean = lookup(AsyncBean.class);
        final CountDownLatch latch = new CountDownLatch(1);

        final Future<Integer> future = bean.testRequestScopeActive(latch);
        latch.countDown();
        int result = future.get();
        Assert.assertEquals(20, result);
    }

    /**
     * Stateful - void returned
     */
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

    /**
     * Stateful - future returned
     */
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

    /**
     * Singleton - void returned
     */
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

    /**
     * Singleton - future returned
     */
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

    /**
     * Cancelling
     */
    @Test
    public void testCancelAsyncMethod() throws Exception {
        AsyncBean bean = lookup(AsyncBean.class);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        final Future<String> future = bean.asyncCancelMethod(latch, latch2);
        latch.await(WAIT_TIME_S, TimeUnit.SECONDS);
        Assert.assertFalse(future.isDone()); // we are in async method
        Assert.assertFalse(future.isCancelled());
        boolean wasCanceled = future.cancel(true); // we are running - task can't be canceled
        if (wasCanceled) {
            Assert.assertTrue("isDone() was expected to return true after a call to cancel() with mayBeInterrupting = true, returned true", future.isDone());
            Assert.assertTrue("isCancelled() was expected to return true after a call to cancel() returned true", future.isCancelled());
        }
        latch2.countDown();
        String result = future.get();
        Assert.assertFalse(wasCanceled); // this should be false because task was not cancelled
        Assert.assertEquals("false;true", result); // the bean knows that it was cancelled
    }

    @Test
    @RunAsClient
    public void testCancelRemoteAsyncMethod() throws Exception {
        AsyncBeanCancelRemoteInterface bean = (AsyncBeanCancelRemoteInterface) remoteContext.lookup(ARCHIVE_NAME + "/" +
                AsyncBean.class.getSimpleName() + "!" + AsyncBeanCancelRemoteInterface.class.getName());
        AsyncBeanSynchronizeSingletonRemote singleton = (AsyncBeanSynchronizeSingletonRemote) remoteContext.lookup(ARCHIVE_NAME + "/" +
                AsyncBeanSynchronizeSingleton.class.getSimpleName() + "!" + AsyncBeanSynchronizeSingletonRemote.class.getName());

        singleton.reset();
        final Future<String> future = bean.asyncRemoteCancelMethod();
        singleton.latchAwaitSeconds(WAIT_TIME_S); // waiting for the bean method was already invocated
        Assert.assertFalse("isDone() was expected to return false because the method is still active", future.isDone()); // we are in async method
        Assert.assertFalse("isCancelled() was expected to return false because the method is still active", future.isCancelled());
        boolean wasCanceled = future.cancel(true); // we are running - task can't be canceled
        if (wasCanceled) {
            Assert.assertTrue("isDone() was expected to return true after a call to cancel() with mayBeInterrupting = true, returned true", future.isDone());
            Assert.assertTrue("isCancelled() was expected to return true after a call to cancel() returned true", future.isCancelled());
        }
        String result = future.get();
        Assert.assertFalse(wasCanceled); // this should be false because task was not cancelled
        Assert.assertEquals("false;true", result); // the bean knows that it was cancelled
    }

    /**
     * Exception thrown
     */
    @Test
    public void testExceptionThrown() throws NamingException {
        AsyncBean bean = lookup(AsyncBean.class);
        Future<String> future = bean.asyncMethodWithException(true);
        try {
            future.get();
            Assert.fail("ExecutionException was expected");
        } catch (ExecutionException ee) {
            // expecting this and we are able to get caused exception
            Assert.assertNotNull(ee.getCause());
        } catch (Exception e) {
            Assert.fail("ExecutionException was expected and not " + e.getClass());
        }
    }

    /**
     * Asynchronous inherited from parent
     */
    @Test
    public void testVoidParentAsyncMethod() throws Exception {
        AsyncChildBean bean = lookup(AsyncChildBean.class);
        Assert.assertFalse(AsyncParentClass.voidMethodCalled);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        bean.asyncMethod(latch, latch2);
        latch.countDown();
        latch2.await();
        Assert.assertTrue(AsyncParentClass.voidMethodCalled);
    }

    /**
     * Async declaration in descriptor
     */
    @Test
    public void testAsyncDescriptor() throws Exception {
        AsyncBeanDescriptor bean = lookup(AsyncBeanDescriptor.class);
        Assert.assertFalse(AsyncBeanDescriptor.futureMethodCalled);
        final CountDownLatch latch = new CountDownLatch(1);
        bean.futureMethod(latch);
        latch.await(WAIT_TIME_S, TimeUnit.SECONDS);
        Assert.assertTrue(AsyncBeanDescriptor.futureMethodCalled);
    }

    /**
     * Remote async void call
     */
    @Test
    @RunAsClient
    public void testRemoteAsynchronousVoidCall() throws Exception {
        AsyncBeanRemoteInterface bean = (AsyncBeanRemoteInterface) remoteContext.lookup(
                ARCHIVE_NAME + "/" + AsyncBeanRemote.class.getSimpleName() + "!" + AsyncBeanRemoteInterface.class.getName());
        bean.asyncMethod();
    }

    /**
     * Remote async return future call
     */
    @Test
    @RunAsClient
    public void testRemoteAsynchronousReturnFutureCall() throws Exception {
        AsyncBeanRemoteInterface bean = (AsyncBeanRemoteInterface) remoteContext.lookup(
                ARCHIVE_NAME + "/" + AsyncBeanRemote.class.getSimpleName() + "!" + AsyncBeanRemoteInterface.class.getName());
        Future<Boolean> future = bean.futureMethod();
        Assert.assertTrue("Supposing that future.get() method returns TRUE but it returned FALSE", future.get());
    }
}
