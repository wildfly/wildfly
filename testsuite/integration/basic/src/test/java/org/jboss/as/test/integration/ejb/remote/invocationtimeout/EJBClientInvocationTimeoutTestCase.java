/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.invocationtimeout;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jakarta.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.network.NetworkUtils;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientConnection;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.protocol.remote.RemoteTransportProvider;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

/**
 * Test that it is possible to set invocation timeouts for EJB client invocations.
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EJBClientInvocationTimeoutTestCase {

    private static final String MODULE_NAME = "ejb-client-invocation-timeout";

    final String INVOCATION_URL = "remote+http://" +
            NetworkUtils.formatPossibleIpv6Address(System.getProperty("node0", "localhost")) + ":8080";

    @Deployment(testable = false)
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(EJBClientInvocationTimeoutTestCase.class.getPackage());
        return jar;
    }

    /**
     * Set the timeout on a single EJB proxy using EJBClient.setInvocationTimeout method
     */
    @Test
    public void testSettingTimeoutOnParticularProxy() throws NamingException {
        final Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        properties.put(Context.PROVIDER_URL, INVOCATION_URL);
        final InitialContext ejbCtx = new InitialContext(properties);
        try {
            LongRunningBeanRemote bean = lookup(ejbCtx);
            EJBClient.setInvocationTimeout(bean, 1, TimeUnit.SECONDS);
            try {
                bean.longRunningOperation();
                Assert.fail("Call shouldn't be allowed to finish without throwing an exception");
            } catch (EJBException e) {
                Assert.assertTrue("Call should fail with a TimeoutException, but was: ",
                        e.getCausedByException() instanceof TimeoutException);
            }
        } finally {
            ejbCtx.close();
        }
    }

    /**
     * Set the timeout for all invocations using a scoped context
     */
    @Test
    public void testScopedContext() throws NamingException {
        final Properties properties = new Properties();
        properties.put("org.jboss.ejb.client.scoped.context", "true");
        properties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        properties.put("remote.connections", "main");
        properties.put("remote.connection.main.host", System.getProperty("node0", "127.0.0.1"));
        properties.put("remote.connection.main.port", "8080");
        properties.put("invocation.timeout", "1");


        final InitialContext ejbCtx = new InitialContext(properties);
        try {
            LongRunningBeanRemote bean = lookup(ejbCtx);
            try {
                bean.longRunningOperation();
                Assert.fail("Call shouldn't be allowed to finish without throwing an exception");
            } catch (EJBException e) {
                Assert.assertEquals("Call should fail with a TimeoutException",
                        TimeoutException.class, e.getCausedByException().getClass());
            }
        } finally {
            ejbCtx.close();
        }
    }

    /**
     * Test setting the invocation timeout when initializing an EJB client context programmatically.
     */
    @Test
    public void testEjbClientContextBuilder() {
        final EJBClientContext.Builder builder = new EJBClientContext.Builder();
        builder.setInvocationTimeout(1);
        builder.addTransportProvider(new RemoteTransportProvider());
        builder.addClientConnection(
                new EJBClientConnection.Builder().setDestination(URI.create(INVOCATION_URL)).build()
        );
        builder.build().run(() -> {
            final Properties properties = new Properties();
            properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
            InitialContext ejbCtx = null;
            try {
                ejbCtx = new InitialContext(properties);
                LongRunningBeanRemote bean = lookup(ejbCtx);
                try {
                    bean.longRunningOperation();
                    Assert.fail("Call shouldn't be allowed to finish without throwing an exception");
                } catch (EJBException e) {
                    Assert.assertEquals("Call should fail with a TimeoutException",
                            TimeoutException.class, e.getCausedByException().getClass());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (ejbCtx != null) {
                    try {
                        ejbCtx.close();
                    } catch (NamingException e) {
                    }
                }
            }
        });
    }

    public LongRunningBeanRemote lookup(InitialContext ctx) throws NamingException {
        return (LongRunningBeanRemote)ctx.lookup(
                "ejb:/" + MODULE_NAME + "/" + LongRunningBean.class.getSimpleName() + "!"
                        + LongRunningBeanRemote.class.getName());
    }

}
