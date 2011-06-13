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

package org.jboss.as.test.spec.ejb3.stateful.exception;

import javax.ejb.NoSuchEJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that post construct callbacks are not called on system exception,
 * and that the bean is destroyed
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class SystemExceptionTestCase {

    private static final String ARCHIVE_NAME = "SystemExceptionTestCase";

    private static InitialContext iniCtx;

    @BeforeClass
    public static void beforeClass() throws NamingException {
        iniCtx = new InitialContext();
    }

    @Deployment
    public static JavaArchive deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClass(ExceptionSfsb.class);
        return jar;
    }

    protected static <T> T lookup(Class<T> beanType) throws NamingException {
        return beanType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanType.getSimpleName() + "!" + beanType.getName()));
    }

    /**
     * Ensure that a system exception destroys the bean.
     *
     * @throws Exception
     */
    @Test
    public void testSystemExceptionDestroysBean() throws Exception {

        ExceptionSfsb sfsb1 = lookup(ExceptionSfsb.class);
        Assert.assertFalse(sfsb1.preDestroy);
        try {
            sfsb1.systemException();
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains(ExceptionSfsb.MESSAGE));
        }
        Assert.assertFalse(sfsb1.preDestroy);
        try {
            sfsb1.systemException();
            throw new RuntimeException("Expecting NoSuchEjbException");
        } catch (NoSuchEJBException expected) {

        }

    }
}
