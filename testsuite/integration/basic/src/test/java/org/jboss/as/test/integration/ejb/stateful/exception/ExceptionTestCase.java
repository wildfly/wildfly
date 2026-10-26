/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.exception;

import jakarta.ejb.EJBException;
import jakarta.ejb.NoSuchEJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that post construct callbacks are not called on system exception,
 * and that the bean is destroyed
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
public class ExceptionTestCase {

    protected static final String ARCHIVE_NAME = "ExceptionTestCase";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(ExceptionTestCase.class.getPackage());
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    private DestroyMarkerBeanInterface isPreDestroy;

    private <T> T lookup(Class<T> beanType) throws NamingException {
        return beanType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanType.getSimpleName() + "!" + beanType.getName()));
    }

    protected SFSB1Interface getBean() throws NamingException {
        return lookup(SFSB1.class);
    }

    protected DestroyMarkerBeanInterface getMarker() throws NamingException {
        return lookup(DestroyMarkerBean.class);
    }

    @BeforeEach
    public void before() throws NamingException {
        isPreDestroy = getMarker();
    }

    /**
     * Ensure that a system exception destroys the bean.
     */
    @Test
    public void testSystemExceptionDestroysBean() throws Exception {

        SFSB1Interface sfsb1 = getBean();
        Assertions.assertFalse(isPreDestroy.is());
        try {
            sfsb1.systemException();
            Assertions.fail("It was expected a RuntimeException being thrown");
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.getMessage().contains(SFSB1.MESSAGE));
        }
        Assertions.assertFalse(isPreDestroy.is());
        try {
            sfsb1.systemException();
            Assertions.fail("Expecting NoSuchEjbException");
        } catch (NoSuchEJBException expected) {
        }
    }

    /**
     * Throwing EJBException which as child of RuntimeException causes
     * SFSB to be removed.
     */
    @Test
    public void testEjbExceptionDestroysBean() throws Exception {

        SFSB1Interface sfsb1 = getBean();
        Assertions.assertFalse(isPreDestroy.is());
        try {
            sfsb1.ejbException();
            Assertions.fail("It was expected a EJBException being thrown");
        } catch (EJBException e) {
            Assertions.assertTrue(e.getMessage().contains(SFSB1.MESSAGE));
        }
        Assertions.assertFalse(isPreDestroy.is(),
                "Thrown exception removes SFS but does not call any callback");

        try {
            sfsb1.ejbException();
            Assertions.fail("Expecting NoSuchEjbException");
        } catch (NoSuchEJBException expected) {
        }
    }

    /**
     * Throwing non {@link RuntimeException} which does not cause
     * SFSB being removed.
     */
    @Test
    public void testUserExceptionDoesNothing() throws Exception {

        SFSB1Interface sfsb1 = getBean();
        Assertions.assertFalse(isPreDestroy.is());
        try {
            sfsb1.userException();
            Assertions.fail("It was expected a user exception being thrown");
        } catch (TestException e) {
            Assertions.assertTrue(e.getMessage().contains(SFSB1.MESSAGE));
        }
        Assertions.assertFalse(isPreDestroy.is());

        try {
            sfsb1.userException();
            Assertions.fail("It was expected a user exception being thrown");
        } catch (TestException e) {
            Assertions.assertTrue(e.getMessage().contains(SFSB1.MESSAGE));
        }
        sfsb1.remove();
        Assertions.assertTrue(isPreDestroy.is(), "As remove was called preDestroy callback is expected");
    }
}
