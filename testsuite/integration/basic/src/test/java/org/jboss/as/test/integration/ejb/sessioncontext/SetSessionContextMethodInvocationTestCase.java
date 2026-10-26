/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.sessioncontext;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that bean implementing the {@link jakarta.ejb.SessionBean} interface has its
 * {@link jakarta.ejb.SessionBean#setSessionContext(jakarta.ejb.SessionContext)} method invoked.
 * <p/>
 * User: Jaikiran Pai
 */
@ExtendWith(ArquillianExtension.class)
public class SetSessionContextMethodInvocationTestCase {

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addPackage(SLSBImplementingSessionBean.class.getPackage());

        return jar;
    }

    private<T> T lookup(Class<T> beanType) throws NamingException {
        return beanType.cast(ctx.lookup("java:module/" + beanType.getSimpleName() + "!" + beanType.getName()));
    }

    /**
     * Tests that {@link jakarta.ejb.SessionBean#setSessionContext(jakarta.ejb.SessionContext)} was invoked on a stateless
     * session bean, implementing the {@link jakarta.ejb.SessionBean} interface
     *
     * @throws Exception
     */
    @Test
    public void testSetSessionContextOnSLSB() throws Exception {
        final SLSBImplementingSessionBean slsb = lookup(SLSBImplementingSessionBean.class);
        Assertions.assertTrue(slsb.wasSetSessionContextMethodInvoked(),
                "setSessionContext(SessionContext) method was not invoked on a stateless bean implementing jakarta.ejb.SessionBean");
    }

    /**
     * Tests that {@link jakarta.ejb.SessionBean#setSessionContext(jakarta.ejb.SessionContext)} was invoked on a stateful
     * session bean, implementing the {@link jakarta.ejb.SessionBean} interface
     *
     * @throws Exception
     */
    @Test
    public void testSetSessionContextOnSFSB() throws Exception {
        final SFSBImplementingSessionBean sfsb = lookup(SFSBImplementingSessionBean.class);
        Assertions.assertTrue(sfsb.wasSetSessionContextMethodInvoked(),
                "setSessionContext(SessionContext) method was not invoked on a stateful bean implementing jakarta.ejb.SessionBean");
    }


    /**
     * Tests that a {@link jakarta.ejb.SessionContext} is injected into a stateless bean, via the @Resource annotation
     *
     * @throws Exception
     */
    @Test
    public void testSessionContextInjectionOnSLSB() throws Exception {
        final SLSBImplementingSessionBean slsb = lookup(SLSBImplementingSessionBean.class);
        Assertions.assertTrue(slsb.wasSessionContextInjected(), "SessionContext was not injectd in stateless bean");
    }

    /**
     * Tests that a {@link jakarta.ejb.SessionContext} is injected into a stateful bean, via the @Resource annotation
     *
     * @throws Exception
     */
    @Test
    public void testSessionContextInjectionOnSFSB() throws Exception {
        final SFSBImplementingSessionBean sfsb = lookup(SFSBImplementingSessionBean.class);
        Assertions.assertTrue(sfsb.wasSessionContextInjected(), "SessionContext was not injectd in stateful bean");
    }

    /**
     * Testing whether correct exception is called on {@link jakarta.ejb.SessionContext#wasCancelCalled()} is returned. Supposing IllegalStateException.
     */
    @Test
    public void testWasCancelledCalled() throws Exception {
        final SLSBImplementingSessionBean slsb = lookup(SLSBImplementingSessionBean.class);
        try {
            slsb.wasCanceledCalled();
        } catch (Exception e) {
            Assertions.assertEquals(IllegalStateException.class,
                    e.getCause().getClass(), "The SessionContext.wasCanceledCalled() method was called on not asynchronous method. We supposing " + IllegalStateException.class.getName());
            return; // we are fine with this
        }
        Assertions.fail("Expecting: " + IllegalStateException.class.getName() + " and it was not thrown.");
    }

}
