/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.jndi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;

/**
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HttpRemoteEJBJndiBasedInvocationTestCase {

    private static final String APP_NAME = "";
    private static final String DISTINCT_NAME = "";
    private static final String MODULE_NAME = "remote-ejb-jndi-test-case";


    @ContainerResource
    private ManagementClient managementClient;

    @Deployment(testable = false) // the incorrectly named "testable" attribute tells Arquillian whether or not
    // it should add Arquillian specific metadata to the archive (which ultimately transforms it to a WebArchive).
    // We don't want that, so set that flag to false
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(RemoteCounter.class.getPackage());

        return jar;
    }

    private static AuthenticationContext old;

    @BeforeClass
    public static void setup() {
        AuthenticationConfiguration config = AuthenticationConfiguration.empty().useName("user1").usePassword("password1");
        AuthenticationContext context = AuthenticationContext.empty().with(MatchRule.ALL, config);
        old = AuthenticationContext.captureCurrent();
        AuthenticationContext.getContextManager().setGlobalDefault(context);
    }

    @AfterClass
    public static void after() {
        AuthenticationContext.getContextManager().setGlobalDefault(old);
    }


    public Context getContext() throws Exception {
        final Hashtable props = new Hashtable();
        props.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        props.put(Context.PROVIDER_URL, getHttpUri());
        return new InitialContext(props);
    }

    @Test
    public void testRemoteSLSBInvocation() throws Exception {
        final RemoteEcho remoteEcho = (RemoteEcho) getContext().lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME
                + "/" + EchoBean.class.getSimpleName() + "!" + RemoteEcho.class.getName());
        Assert.assertNotNull("Lookup returned a null bean proxy", remoteEcho);
        final String msg = "Hello world from a really remote client!!!";
        final String echo = remoteEcho.echo(msg);
        Assert.assertEquals("Unexpected echo returned from remote bean", msg, echo);

        // invoke a method which uses application specific type instead of primitives
        final EchoMessage message = new EchoMessage();
        message.setMessage("Hello");
        final EchoMessage echoResponse = remoteEcho.echo(message);
        Assert.assertNotNull("Echo response was null", echoResponse);
        Assert.assertEquals("Unexpected echo returned from the bean", message.getMessage(), echoResponse.getMessage());
    }

    @Test
    public void testRemoteSFSBInvocation() throws Exception {
        final RemoteCounter remoteCounter = (RemoteCounter) getContext().lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME
                + "/" + CounterBean.class.getSimpleName() + "!" + RemoteCounter.class.getName() + "?stateful");
        Assert.assertNotNull("Lookup returned a null bean proxy", remoteCounter);
        Assert.assertEquals("Unexpected initial count returned by bean", 0, remoteCounter.getCount());
        final int NUM_TIMES = 25;
        // test increment
        for (int i = 0; i < NUM_TIMES; i++) {
            remoteCounter.incrementCount();
            final int currentCount = remoteCounter.getCount();
            Assert.assertEquals("Unexpected count after increment", i + 1, currentCount);
        }
        Assert.assertEquals("Unexpected total count after increment", NUM_TIMES, remoteCounter.getCount());
        // test decrement
        for (int i = NUM_TIMES; i > 0; i--) {
            remoteCounter.decrementCount();
            final int currentCount = remoteCounter.getCount();
            Assert.assertEquals("Unexpected count after decrement", i - 1, currentCount);
        }
        Assert.assertEquals("Unexpected total count after decrement", 0, remoteCounter.getCount());

    }

    private URI getHttpUri() throws URISyntaxException {
        URI webUri = managementClient.getWebUri();
        return new URI("http", webUri.getUserInfo(), webUri.getHost(), webUri.getPort(), "/wildfly-services", "", "");
    }
}
