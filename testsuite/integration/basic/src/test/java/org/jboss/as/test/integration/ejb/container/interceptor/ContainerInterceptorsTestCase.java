/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.container.interceptor;

import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import static org.hamcrest.CoreMatchers.containsString;
import org.jboss.arquillian.container.test.api.Deployer;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ejb.container.interceptor.incorrect.IncorrectContainerInterceptor;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the <code>container-interceptors</code> configured in jboss-ejb3.xml and processed and applied correctly to the
 * relevant EJBs.
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class ContainerInterceptorsTestCase {

    private static final String EJB_JAR_NAME = "ejb-container-interceptors";

    @ArquillianResource
    Deployer deployer;

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EJB_JAR_NAME + ".jar");
        jar.addPackage(ContainerInterceptorsTestCase.class.getPackage());
        jar.addAsManifestResource(ContainerInterceptorsTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

    /**
     * Deployment which should fail. It contains an interceptor with 2 methods annotated with the @AroundInvoke.
     *
     * @return
     */
    @Deployment(name = "incorrect-deployment", managed = false)
    public static JavaArchive createIncorrectDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "incorrect-deployment.jar");
        jar.addPackage(IncorrectContainerInterceptor.class.getPackage());
        jar.addAsManifestResource(IncorrectContainerInterceptor.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

    @Test
    public void testMultipleAnnotatedInvokeAroundClass() throws Throwable {
        try {
            deployer.deploy("incorrect-deployment");
            fail("Deployment should fail");
        } catch (Exception ex) {
            final StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            assertThat(sw.toString(), containsString("WFLYEE0109"));
    }
}

/**
 * Tests that the container-interceptor(s) are invoked when an EJB method on a local view is invoked
 *
 * @throws Exception
 */
@Test
        public void testInvocationOnLocalView() throws Exception {
        final FlowTrackingBean

bean = InitialContext.doLookup("java:module/" + FlowTrackingBean.class

    .getSimpleName() + "!"
                + FlowTrackingBean.class

        .getName());
        final String message = "foo";
        // all interceptors (container-interceptor and Java EE interceptor) are expected to be invoked.
        final String expectedResultForFirstInvocation = ContainerInterceptorOne.class.getName() + " "
                + NonContainerInterceptor.class.getName() + " " + FlowTrackingBean.class.getName() + " " + message;
        final String firstResult = bean.echo(message);

        Assert.assertEquals (
        "Unexpected result after first invocation on bean", expectedResultForFirstInvocation, firstResult);

        final String secondMessage = "bar";
        // all interceptors (container-interceptor and Java EE interceptor) are expected to be invoked.
        final String expectedResultForSecondInvocation = ContainerInterceptorOne.class.getName() + " "
                + NonContainerInterceptor.class.getName() + " " + FlowTrackingBean.class.getName() + " " + secondMessage;
        final String secondResult = bean.echo(secondMessage);

        Assert.assertEquals (


    "Unexpected result after second invocation on bean", expectedResultForSecondInvocation,
                secondResult);
    }

    /**
     * Tests that the container-interceptor(s) are invoked when an EJB method on a remote view is invoked
     *
     * @throws Exception
     */
    @Test
    public void testInvocationOnRemoteView() throws Exception {
        final FlowTracker bean = InitialContext.doLookup("java:module/" + FlowTrackingBean.class.getSimpleName() + "!"
                + FlowTracker.class.getName());
        final String message = "foo";
        // all interceptors (container-interceptor and Java EE interceptor) are expected to be invoked.
        final String expectedResultForFirstInvocation = ContainerInterceptorOne.class.getName() + " "
                + NonContainerInterceptor.class.getName() + " " + FlowTrackingBean.class.getName() + " " + message;
        final String firstResult = bean.echo(message);
        Assert.assertEquals("Unexpected result after first invocation on remote view of bean",
                expectedResultForFirstInvocation, firstResult);

        final String secondMessage = "bar";
        // all interceptors (container-interceptor and Java EE interceptor) are expected to be invoked.
        final String expectedResultForSecondInvocation = ContainerInterceptorOne.class.getName() + " "
                + NonContainerInterceptor.class.getName() + " " + FlowTrackingBean.class.getName() + " " + secondMessage;
        final String secondResult = bean.echo(secondMessage);
        Assert.assertEquals("Unexpected result after second invocation on remote view of bean",
                expectedResultForSecondInvocation, secondResult);

    }

    /**
     * Tests that the container-interceptor(s) have access to the data that's passed by a remote client via the
     * {@link javax.interceptor.InvocationContext#getContextData()}
     */
    @Test
    // force real remote invocation so that the RemotingConnectionEJBReceiver is used instead of a LocalEJBReceiver
    @RunAsClient
    public void testDataPassingForContainerInterceptorsOnRemoteView() throws Exception {
        // create some data that the client side interceptor will pass along during the EJB invocation
        final Map<String, Object> interceptorData = new HashMap<String, Object>();
        interceptorData.put(FlowTrackingBean.CONTEXT_DATA_KEY, ContainerInterceptorOne.class.getName());
        final SimpleEJBClientInterceptor clientInterceptor = new SimpleEJBClientInterceptor(interceptorData);
        // get hold of the EJBClientContext and register the client side interceptor
        EJBClientContext ejbClientContext = EJBClientContext.getCurrent().withAddedInterceptors(clientInterceptor);

        final Hashtable<String, Object> jndiProps = new Hashtable<String, Object>();
        jndiProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context jndiCtx = new InitialContext(jndiProps);
        ejbClientContext.runCallable(() -> {
            final FlowTracker bean = (FlowTracker) jndiCtx.lookup("ejb:/" + EJB_JAR_NAME + "/"
                    + FlowTrackingBean.class.getSimpleName() + "!" + FlowTracker.class.getName());
            final String message = "foo";
            // we passed ContainerInterceptorOne as the value of the context data for the invocation, which means that we want the ContainerInterceptorOne
            // to be skipped, so except that interceptor, the rest should be invoked.
            final String expectedResultForFirstInvocation = NonContainerInterceptor.class.getName() + " "
                    + FlowTrackingBean.class.getName() + " " + message;
            final String firstResult = bean.echo(message);
            Assert.assertEquals("Unexpected result invoking on bean when passing context data via EJB client interceptor",
                    expectedResultForFirstInvocation, firstResult);

            // Now try another invocation, this time skip a different interceptor
            interceptorData.clear();
            interceptorData.put(FlowTrackingBean.CONTEXT_DATA_KEY, NonContainerInterceptor.class.getName());
            final String secondMessage = "bar";
            // we passed NonContainerInterceptor as the value of the context data for the invocation, which means that we want the NonContainerInterceptor
            // to be skipped, so except that interceptor, the rest should be invoked.
            final String expectedResultForSecondInvocation = ContainerInterceptorOne.class.getName() + " "
                    + FlowTrackingBean.class.getName() + " " + secondMessage;
            final String secondResult = bean.echo(secondMessage);
            Assert.assertEquals("Unexpected result invoking on bean when passing context data via EJB client interceptor",
                    expectedResultForSecondInvocation, secondResult);
            return null;
        });
    }

    /**
     * Tests that class level and method level container-interceptors (and other eligible interceptors) are invoked during an
     * EJB invocation
     *
     * @throws Exception
     */
    @Test
    public void testClassAndMethodLevelContainerInterceptor() throws Exception {
        final AnotherFlowTrackingBean bean = InitialContext.doLookup("java:module/"
                + AnotherFlowTrackingBean.class.getSimpleName() + "!" + AnotherFlowTrackingBean.class.getName());
        final String message = "foo";
        // all interceptors (container-interceptor and Java EE interceptor) are expected to be invoked.
        final String expectedResultForFirstInvocation = ContainerInterceptorOne.class.getName() + " "
                + ClassLevelContainerInterceptor.class.getName() + " " + MethodSpecificContainerInterceptor.class.getName()
                + " " + NonContainerInterceptor.class.getName() + " " + AnotherFlowTrackingBean.class.getName() + " " + message;

        // invoke on the specific method which has the container-interceptor eligible
        final String firstResult = bean.echoWithMethodSpecificContainerInterceptor(message);
        Assert.assertEquals("Unexpected result after invocation on bean", expectedResultForFirstInvocation, firstResult);

        // now let's invoke on a method for which that method level container interceptor isn't applicable
        final String expectedResultForSecondInvocation = ContainerInterceptorOne.class.getName() + " "
                + ClassLevelContainerInterceptor.class.getName() + " " + NonContainerInterceptor.class.getName() + " "
                + AnotherFlowTrackingBean.class.getName() + " " + message;

        final String secondResult = bean.echo(message);
        Assert.assertEquals("Unexpected result after invocation on bean", expectedResultForSecondInvocation, secondResult);

    }

    /**
     * Tests that the explicit ordering specified for container-interceptors in the jboss-ejb3.xml is honoured
     *
     * @throws Exception
     */
    @Test
    public void testContainerInterceptorOrdering() throws Exception {
        final AnotherFlowTrackingBean bean = InitialContext.doLookup("java:module/"
                + AnotherFlowTrackingBean.class.getSimpleName() + "!" + AnotherFlowTrackingBean.class.getName());
        final String message = "foo";
        // all interceptors (container-interceptor and Java EE interceptor) are expected to be invoked in the order specified in the jboss-ejb3.xml
        final String expectedResultForFirstInvocation = ClassLevelContainerInterceptor.class.getName() + " "
                + MethodSpecificContainerInterceptor.class.getName() + " " + ContainerInterceptorOne.class.getName() + " "
                + NonContainerInterceptor.class.getName() + " " + AnotherFlowTrackingBean.class.getName() + " " + message;

        // invoke on the specific method which has the interceptor-order specified
        final String firstResult = bean.echoInSpecificOrderOfContainerInterceptors(message);
        Assert.assertEquals("Unexpected result after invocation on bean", expectedResultForFirstInvocation, firstResult);

    }

    /**
     * Tests that exception thrown in a container-interceptor is propagated.
     *
     * @throws Exception
     */
    @Test
    public void testFailingContainerInterceptor() throws Exception {
        final AnotherFlowTrackingBean bean = InitialContext.doLookup("java:module/"
                + AnotherFlowTrackingBean.class.getSimpleName() + "!" + AnotherFlowTrackingBean.class.getName());
        try {
            bean.failingEcho("test");
            fail("Should fail");
        } catch (IllegalArgumentException e) {
            //OK
        }
    }
}
