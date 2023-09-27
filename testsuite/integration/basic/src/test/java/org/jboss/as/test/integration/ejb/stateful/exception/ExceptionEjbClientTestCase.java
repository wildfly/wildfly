/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.exception;

import static org.junit.Assert.assertTrue;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.codehaus.plexus.util.ExceptionUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.ejb.client.RequestSendFailedException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

/**
 * The same bundle of tests as runs at {@link ExceptionTestCase} but these ones
 * are managed at client mode - all calls runs over ejb remoting.
 *
 *  @author Ondrej Chaloupka
 */
@RunAsClient
@RunWith(Arquillian.class)
public class ExceptionEjbClientTestCase extends ExceptionTestCase {
    private static Context context;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        props.put(Context.PROVIDER_URL, "remote+http://" + TestSuiteEnvironment.getServerAddress() + ":" + 8080);
        context = new InitialContext(props);
    }

    private <T> T lookup(Class<? extends T> beanType, Class<T> interfaceType,boolean isStateful) throws NamingException {
        String ejbLookup = String.format("ejb:/%s/%s!%s%s", ARCHIVE_NAME, beanType.getSimpleName(), interfaceType.getName(),
                (isStateful ? "?stateful" : ""));
        return interfaceType.cast(context.lookup(ejbLookup));
    }

    protected SFSB1Interface getBean() throws NamingException {
        return lookup(SFSB1.class, SFSB1Interface.class, true);
    }

    protected DestroyMarkerBeanInterface getMarker() throws NamingException {
        return lookup(DestroyMarkerBean.class, DestroyMarkerBeanInterface.class, false);
    }

    /**
     * Test exception contains destination when there is no server running (wrong server)
     */
    @Test
    public void testConnectException() throws Exception {
        try {
            Properties props = new Properties();
            props.put(Context.INITIAL_CONTEXT_FACTORY,  "org.wildfly.naming.client.WildFlyInitialContextFactory");
            //Wrong server so an exception will be thrown
            props.put(Context.PROVIDER_URL, "remote+http://localhost:1000");
            Context ctx = new InitialContext(props);

            DestroyMarkerBeanInterface destroyM = (DestroyMarkerBeanInterface) ctx.lookup(String.format("ejb:/%s/%s!%s", ARCHIVE_NAME, DestroyMarkerBean.class.getSimpleName(), DestroyMarkerBeanInterface.class.getName()));
            destroyM.is();
            Assert.fail("It was expected a RequestSendFailedException being thrown");
        } catch (RequestSendFailedException e) {
            assertTrue("Destination should be displayed", ExceptionUtils.getFullStackTrace(e).contains("remote+http://localhost:1000"));
        }
    }

}
