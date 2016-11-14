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

package org.jboss.as.test.integration.ejb.stateful.exception;

import javax.ejb.EJBException;
import javax.ejb.NoSuchEJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that post construct callbacks are not called on system exception,
 * and that the bean is destroyed
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
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

    @Before
    public void before() throws NamingException {
        isPreDestroy = getMarker();
    }

    /**
     * Ensure that a system exception destroys the bean.
     */
    @Test
    public void testSystemExceptionDestroysBean() throws Exception {

        SFSB1Interface sfsb1 = getBean();
        Assert.assertFalse(isPreDestroy.is());
        try {
            sfsb1.systemException();
            Assert.fail("It was expected a RuntimeException being thrown");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains(SFSB1.MESSAGE));
        }
        Assert.assertFalse(isPreDestroy.is());
        try {
            sfsb1.systemException();
            Assert.fail("Expecting NoSuchEjbException");
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
        Assert.assertFalse(isPreDestroy.is());
        try {
            sfsb1.ejbException();
            Assert.fail("It was expected a EJBException being thrown");
        } catch (EJBException e) {
            Assert.assertTrue(e.getMessage().contains(SFSB1.MESSAGE));
        }
        Assert.assertFalse("Thrown exception removes SFS but does not call any callback",
                isPreDestroy.is());

        try {
            sfsb1.ejbException();
            Assert.fail("Expecting NoSuchEjbException");
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
        Assert.assertFalse(isPreDestroy.is());
        try {
            sfsb1.userException();
            Assert.fail("It was expected a user exception being thrown");
        } catch (TestException e) {
            Assert.assertTrue(e.getMessage().contains(SFSB1.MESSAGE));
        }
        Assert.assertFalse(isPreDestroy.is());

        try {
            sfsb1.userException();
            Assert.fail("It was expected a user exception being thrown");
        } catch (TestException e) {
            Assert.assertTrue(e.getMessage().contains(SFSB1.MESSAGE));
        }
        sfsb1.remove();
        Assert.assertTrue("As remove was called preDestroy callback is expected", isPreDestroy.is());
    }
}
