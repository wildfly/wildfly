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

package org.jboss.as.test.integration.legacy.ejb.remote.client.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.ejb.EJBException;
import javax.ejb.NoSuchEJBException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientTransactionContext;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the various common use cases of the EJB remote client API
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EJBClientAPIUsageTestCase {

    private static final Logger logger = Logger.getLogger(EJBClientAPIUsageTestCase.class);

    private static final String APP_NAME = "ejb-remote-client-api-test";

    private static final String MODULE_NAME = "ejb";

    /**
     * Creates an EJB deployment
     *
     * @return
     */
    @Deployment
    public static Archive<?> createDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(EJBClientAPIUsageTestCase.class.getPackage());
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        ear.addAsModule(jar);

        return ear;
    }


    /**
     * Create and setup the EJB client context backed by the remoting receiver
     *
     * @throws Exception
     */
    @Before
    public void beforeTest() throws Exception {
        final EJBClientTransactionContext localUserTxContext = EJBClientTransactionContext.createLocal();
        // set the tx context
        EJBClientTransactionContext.setGlobalContext(localUserTxContext);

    }

    /**
     * Test a simple invocation on a remote view of a Stateless session bean method
     *
     * @throws Exception
     */
    @Test
    public void testRemoteSLSBInvocation() throws Exception {
        final StatelessEJBLocator<EchoRemote> locator = new StatelessEJBLocator(EchoRemote.class, APP_NAME, MODULE_NAME, EchoBean.class.getSimpleName(), "");
        final EchoRemote proxy = EJBClient.createProxy(locator);
        Assert.assertNotNull("Received a null proxy", proxy);
        final String message = "Hello world from a really remote client";
        final String echo = proxy.echo(message);
        Assert.assertEquals("Unexpected echo message", message, echo);
    }

    /**
     * Test bean returning a value object with a transient field.  Will test that the transient field is set to null (just like java serialization would do)
     * instead of a non-null value (non-null came ValueWrapper class initializer if this fails).
     *
     * @throws Exception
     */
    @Test
    public void testValueObjectWithTransientField() throws Exception {
        final StatelessEJBLocator<EchoRemote> locator = new StatelessEJBLocator(EchoRemote.class, APP_NAME, MODULE_NAME, EchoBean.class.getSimpleName(), "");
        final EchoRemote proxy = EJBClient.createProxy(locator);
        String shouldBeNil = proxy.getValue().getShouldBeNilAfterUnmarshalling();
        Assert.assertNull("transient field should be serialized as null but was '" + shouldBeNil + "'",
                shouldBeNil);
    }


    /**
     * Test an invocation on the remote view of a stateless bean which is configured for user interceptors
     *
     * @throws Exception
     */
    @Test
    public void testRemoteSLSBWithInterceptors() throws Exception {
        final StatelessEJBLocator<EchoRemote> locator = new StatelessEJBLocator(EchoRemote.class, APP_NAME, MODULE_NAME, InterceptedEchoBean.class.getSimpleName(), "");
        final EchoRemote proxy = EJBClient.createProxy(locator);
        Assert.assertNotNull("Received a null proxy", proxy);
        final String message = "Hello world from a really remote client";
        final String echo = proxy.echo(message);
        final String expectedEcho = message + InterceptorTwo.MESSAGE_SEPARATOR + InterceptorOne.class.getSimpleName() + InterceptorOne.MESSAGE_SEPARATOR + InterceptorTwo.class.getSimpleName();
        Assert.assertEquals("Unexpected echo message", expectedEcho, echo);
    }

    /**
     * Test an invocation on a stateless bean method which accepts and returns custom objects
     *
     * @throws Exception
     */
    @Test
    public void testRemoteSLSBWithCustomObjects() throws Exception {
        final StatelessEJBLocator<EmployeeManager> locator = new StatelessEJBLocator(EmployeeManager.class, APP_NAME, MODULE_NAME, EmployeeBean.class.getSimpleName(), "");
        final EmployeeManager proxy = EJBClient.createProxy(locator);
        Assert.assertNotNull("Received a null proxy", proxy);
        final String[] nickNames = new String[]{"java-programmer", "ruby-programmer", "php-programmer"};
        final Employee employee = new Employee(1, "programmer");
        // invoke on the bean
        final AliasedEmployee employeeWithNickNames = proxy.addNickNames(employee, nickNames);

        // check the id of the returned employee
        Assert.assertEquals("Unexpected employee id", 1, employeeWithNickNames.getId());
        // check the name of the returned employee
        Assert.assertEquals("Unexpected employee name", "programmer", employeeWithNickNames.getName());
        // check the number of nicknames
        Assert.assertEquals("Unexpected number of nick names", nickNames.length, employeeWithNickNames.getNickNames().size());
        // make sure the correct nick names are present
        for (int i = 0; i < nickNames.length; i++) {
            Assert.assertTrue("Employee was expected to have nick name: " + nickNames[i], employeeWithNickNames.getNickNames().contains(nickNames[i]));
        }
    }

    /**
     * Tests that invocations on a stateful session bean work after a session is created and the stateful
     * session bean really acts as a stateful bean
     *
     * @throws Exception
     */
    @Test
    public void testSFSBInvocation() throws Exception {
        final StatefulEJBLocator<Counter> locator = EJBClient.createSession(Counter.class, APP_NAME, MODULE_NAME, CounterBean.class.getSimpleName(), "");
        final Counter counter = EJBClient.createProxy(locator);
        Assert.assertNotNull("Received a null proxy", counter);
        // invoke the bean
        final int initialCount = counter.getCount();
        logger.trace("Got initial count " + initialCount);
        Assert.assertEquals("Unexpected initial count from stateful bean", 0, initialCount);
        final int NUM_TIMES = 50;
        for (int i = 1; i <= NUM_TIMES; i++) {
            final int count = counter.incrementAndGetCount();
            logger.trace("Got next count " + count);
            Assert.assertEquals("Unexpected count after increment", i, count);
        }
        final int finalCount = counter.getCount();
        logger.trace("Got final count " + finalCount);
        Assert.assertEquals("Unexpected final count", NUM_TIMES, finalCount);
    }


    /**
     * Tests that invocation on a stateful session bean fails, if a session hasn't been created
     *
     * @throws Exception
     */
    @Test
    public void testSFSBAccessFailureWithoutSession() throws Exception {
        // create a locator without a session
        final StatelessEJBLocator<Counter> locator = new StatelessEJBLocator<Counter>(Counter.class, APP_NAME, MODULE_NAME, CounterBean.class.getSimpleName(), "", Affinity.NONE);
        final Counter counter = EJBClient.createProxy(locator);
        Assert.assertNotNull("Received a null proxy", counter);
        // invoke the bean without creating a session
        try {
            final int initialCount = counter.getCount();
            Assert.fail("Expected an EJBException for calling a stateful session bean without creating a session");
        } catch (EJBException ejbe) {
            // expected
            logger.trace("Received the expected exception", ejbe);

        }
    }

    /**
     * Tests that invoking a non-existent EJB leads to a {@link IllegalStateException} as a result of
     * no EJB receivers able to handle the invocation
     *
     * @throws Exception
     */
    @Test
    public void testNonExistentEJBAccess() throws Exception {
        final StatelessEJBLocator<NotAnEJBInterface> locator = new StatelessEJBLocator<NotAnEJBInterface>(NotAnEJBInterface.class, "non-existen-app-name", MODULE_NAME, "blah", "");
        final NotAnEJBInterface nonExistentBean = EJBClient.createProxy(locator);
        Assert.assertNotNull("Received a null proxy", nonExistentBean);
        // invoke on the (non-existent) bean
        try {
            nonExistentBean.echo("Hello world to a non-existent bean");
            Assert.fail("Expected an IllegalStateException");
        } catch (IllegalStateException ise) {
            // expected
            logger.trace("Received the expected exception", ise);
        }
    }

    /**
     * Tests that the invocation on a non-existent view of an (existing) EJB leads to a {@link NoSuchEJBException}
     *
     * @throws Exception
     */
    @Test
    public void testNonExistentViewForEJB() throws Exception {
        final StatelessEJBLocator<NotAnEJBInterface> locator = new StatelessEJBLocator<NotAnEJBInterface>(NotAnEJBInterface.class, APP_NAME, MODULE_NAME, EchoBean.class.getSimpleName(), "");
        final NotAnEJBInterface nonExistentBean = EJBClient.createProxy(locator);
        Assert.assertNotNull("Received a null proxy", nonExistentBean);
        // invoke on the (non-existent) view of a bean
        try {
            nonExistentBean.echo("Hello world to a non-existent view of a bean");
            Assert.fail("Expected an IllegalStateException");
        } catch (IllegalStateException | EJBException nsee) {
            // expected
            logger.trace("Received the expected exception", nsee);
        }
    }

    /**
     * Tests that an {@link javax.ejb.ApplicationException} thrown by a SLSB method is returned back to the
     * client correctly
     *
     * @throws Exception
     */
    @Test
    public void testApplicationExceptionOnSLSBMethod() throws Exception {
        final StatelessEJBLocator<ExceptionThrowingRemote> locator = new StatelessEJBLocator<ExceptionThrowingRemote>(ExceptionThrowingRemote.class, APP_NAME, MODULE_NAME, ExceptionThrowingBean.class.getSimpleName(), "");
        final ExceptionThrowingRemote exceptionThrowingBean = EJBClient.createProxy(locator);
        Assert.assertNotNull("Received a null proxy", exceptionThrowingBean);
        final String exceptionState = "2342348723Dsbjlfjal#";
        try {
            exceptionThrowingBean.alwaysThrowApplicationException(exceptionState);
            Assert.fail("Expected a " + StatefulApplicationException.class.getName() + " exception");
        } catch (StatefulApplicationException sae) {
            // expected
            logger.trace("Received the expected exception", sae);
            Assert.assertEquals("Unexpected state in the application exception", exceptionState, sae.getState());
        }
    }

    /**
     * Tests that a system exception thrown from a SLSB method is conveyed back to the client
     *
     * @throws Exception
     */
    @Test
    public void testSystemExceptionOnSLSBMethod() throws Exception {
        final StatelessEJBLocator<ExceptionThrowingRemote> locator = new StatelessEJBLocator<ExceptionThrowingRemote>(ExceptionThrowingRemote.class, APP_NAME, MODULE_NAME, ExceptionThrowingBean.class.getSimpleName(), "");
        final ExceptionThrowingRemote exceptionThrowingBean = EJBClient.createProxy(locator);
        Assert.assertNotNull("Received a null proxy", exceptionThrowingBean);
        final String exceptionState = "bafasfaj;l";
        try {
            exceptionThrowingBean.alwaysThrowSystemException(exceptionState);
            Assert.fail("Expected a " + EJBException.class.getName() + " exception");
        } catch (EJBException ejbe) {
            // expected
            logger.trace("Received the expected exception", ejbe);
            final Throwable cause = ejbe.getCause();
            Assert.assertTrue("Unexpected cause in EJBException", cause instanceof RuntimeException);
            Assert.assertEquals("Unexpected state in the system exception", exceptionState, cause.getMessage());
        }
    }

    /**
     * Tests that a SLSB method which is marked as asynchronous and returns a {@link java.util.concurrent.Future}
     * is invoked asynchronously and the client isn't blocked for the lifetime of the method
     *
     * @throws Exception
     */
    @Test
    public void testAsyncFutureMethodOnSLSB() throws Exception {
        final StatelessEJBLocator<EchoRemote> locator = new StatelessEJBLocator<EchoRemote>(EchoRemote.class, APP_NAME, MODULE_NAME, EchoBean.class.getSimpleName(), "");
        final EchoRemote echoRemote = EJBClient.createProxy(locator);
        Assert.assertNotNull("Received a null proxy", echoRemote);
        final String message = "You are supposed to be an asynchronous method";
        final long DELAY = 5000;
        final long start = System.currentTimeMillis();
        // invoke the asynchronous method
        final Future<String> futureEcho = echoRemote.asyncEcho(message, DELAY);
        final long end = System.currentTimeMillis();
        logger.trace("Asynchronous invocation returned a Future: " + futureEcho + " in " + (end - start) + " milliseconds");
        // test that the invocation did not act like a synchronous invocation and instead returned "immediately"
        Assert.assertFalse("Asynchronous invocation behaved like a synchronous invocation", (end - start) >= DELAY);
        Assert.assertNotNull("Future is null", futureEcho);
        // Check if the result is marked as complete (it shouldn't be this soon)
        Assert.assertFalse("Future result is unexpectedly completed", futureEcho.isDone());
        // wait for the result
        final String echo = futureEcho.get();
        Assert.assertEquals("Unexpected echo message", message, echo);
    }


    /**
     * Test a simple invocation on a remote view of a Stateless session bean method
     *
     * @throws Exception
     */
    @Test
    public void testGetBusinessObjectRemote() throws Exception {
        final StatelessEJBLocator<EchoRemote> locator = new StatelessEJBLocator(EchoRemote.class, APP_NAME, MODULE_NAME, EchoBean.class.getSimpleName(), "");
        final EchoRemote proxy = EJBClient.createProxy(locator);
        final EchoRemote getBusinessObjectProxy = proxy.getBusinessObject();
        Assert.assertNotNull("Received a null proxy", getBusinessObjectProxy);
        final String message = "Hello world from a really remote client";
        final String echo = getBusinessObjectProxy.echo(message);
        Assert.assertEquals("Unexpected echo message", message, echo);
    }

    /**
     * AS7-3129
     * <p/>
     * Make sure that the CDI request scope is activated for remote EJB invocations
     */
    @Test
    public void testCdiRequestScopeActive() {
        final StatelessEJBLocator<EchoRemote> locator = new StatelessEJBLocator(EchoRemote.class, APP_NAME, MODULE_NAME, EchoBean.class.getSimpleName(), "");
        final EchoRemote proxy = EJBClient.createProxy(locator);
        Assert.assertTrue(proxy.testRequestScopeActive());
    }

    /**
     * AS7-3402
     *
     * Tests that a NonSerializableException does not break the channel
     *
     */
    @Test
    public void testNonSerializableResponse() throws InterruptedException, ExecutionException {
        final StatelessEJBLocator<NonSerialiazableResponseRemote> locator = new StatelessEJBLocator(NonSerialiazableResponseRemote.class, APP_NAME, MODULE_NAME, NonSerializableResponseEjb.class.getSimpleName(), "");
        final NonSerialiazableResponseRemote proxy = EJBClient.createProxy(locator);


        Callable<Object> task = new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                try {
                    proxy.nonSerializable();
                    Assert.fail();
                } catch (Exception e) {
                    logger.trace("expected " + e);
                }
                Thread.sleep(1000);
                Assert.assertEquals("hello", proxy.serializable());
                return null;
            }
        };
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            final List<Future> tasks = new ArrayList<Future>();
            for (int i = 0; i < 100; ++i) {
                tasks.add(executor.submit(task));
            }

            for (Future result : tasks) {
                result.get();
            }
        } finally {
            executor.shutdown();
        }

    }
}
