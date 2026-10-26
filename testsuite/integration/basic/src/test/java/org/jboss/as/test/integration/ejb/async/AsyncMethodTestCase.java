/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that a simple async annotation works.
 * Enhanced test by migration [ JIRA JBQA-5483 ].
 *
 * @author Stuart Douglas, Ondrej Chaloupka
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertFalse(AsyncBean.voidMethodCalled);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        bean.asyncMethod(latch, latch2);
        latch.countDown();
        latch2.await();
        Assertions.assertTrue(AsyncBean.voidMethodCalled);
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
        Assertions.assertTrue(AsyncBean.futureMethodCalled);
        Assertions.assertTrue(result);
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
        Assertions.assertEquals(20, result);
    }

    /**
     * Stateful - void returned
     */
    @Test
    public void testVoidAsyncStatefulMethod() throws Exception {
        AsyncStateful bean = lookup(AsyncStateful.class);
        Assertions.assertFalse(AsyncStateful.voidMethodCalled);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        bean.asyncMethod(latch, latch2);
        latch.countDown();
        latch2.await();
        Assertions.assertTrue(AsyncStateful.voidMethodCalled);
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
        Assertions.assertTrue(AsyncStateful.futureMethodCalled);
        Assertions.assertTrue(result);
    }

    /**
     * Singleton - void returned
     */
    @Test
    public void testVoidAsyncSingletonMethod() throws Exception {
        AsyncSingleton singleton = lookup(AsyncSingleton.class);
        Assertions.assertFalse(AsyncSingleton.voidMethodCalled);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        singleton.asyncMethod(latch, latch2);
        latch.countDown();
        latch2.await();
        Assertions.assertTrue(AsyncSingleton.voidMethodCalled);
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
        Assertions.assertTrue(AsyncSingleton.futureMethodCalled);
        Assertions.assertTrue(result);
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
        Assertions.assertFalse(future.isDone()); // we are in async method
        Assertions.assertFalse(future.isCancelled());
        boolean wasCanceled = future.cancel(true); // we are running - task can't be canceled
        if (wasCanceled) {
            Assertions.assertTrue(future.isDone(), "isDone() was expected to return true after a call to cancel() with mayBeInterrupting = true, returned true");
            Assertions.assertTrue(future.isCancelled(), "isCancelled() was expected to return true after a call to cancel() returned true");
        }
        latch2.countDown();
        String result = future.get();
        Assertions.assertFalse(wasCanceled); // this should be false because task was not cancelled
        Assertions.assertEquals("false;true", result); // the bean knows that it was cancelled
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
        Assertions.assertFalse(future.isDone(), "isDone() was expected to return false because the method is still active"); // we are in async method
        Assertions.assertFalse(future.isCancelled(), "isCancelled() was expected to return false because the method is still active");
        boolean wasCanceled = future.cancel(true); // we are running - task can't be canceled
        if (wasCanceled) {
            Assertions.assertTrue(future.isDone(), "isDone() was expected to return true after a call to cancel() with mayBeInterrupting = true, returned true");
            Assertions.assertTrue(future.isCancelled(), "isCancelled() was expected to return true after a call to cancel() returned true");
        }
        String result = future.get();
        Assertions.assertFalse(wasCanceled); // this should be false because task was not cancelled
        Assertions.assertEquals("false;true", result); // the bean knows that it was cancelled
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
            Assertions.fail("ExecutionException was expected");
        } catch (ExecutionException ee) {
            // expecting this and we are able to get caused exception
            Assertions.assertNotNull(ee.getCause());
        } catch (Exception e) {
            Assertions.fail("ExecutionException was expected and not " + e.getClass());
        }
    }

    /**
     * Asynchronous inherited from parent
     */
    @Test
    public void testVoidParentAsyncMethod() throws Exception {
        AsyncChildBean bean = lookup(AsyncChildBean.class);
        Assertions.assertFalse(AsyncParentClass.voidMethodCalled);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        bean.asyncMethod(latch, latch2);
        latch.countDown();
        latch2.await();
        Assertions.assertTrue(AsyncParentClass.voidMethodCalled);
    }

    /**
     * Async declaration in descriptor
     */
    @Test
    public void testAsyncDescriptor() throws Exception {
        AsyncBeanDescriptor bean = lookup(AsyncBeanDescriptor.class);
        Assertions.assertFalse(AsyncBeanDescriptor.futureMethodCalled);
        final CountDownLatch latch = new CountDownLatch(1);
        bean.futureMethod(latch);
        latch.await(WAIT_TIME_S, TimeUnit.SECONDS);
        Assertions.assertTrue(AsyncBeanDescriptor.futureMethodCalled);
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
        Assertions.assertTrue(future.get(), "Supposing that future.get() method returns TRUE but it returned FALSE");
    }
}
