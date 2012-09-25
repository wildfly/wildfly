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

package org.jboss.as.test.integration.ejb.exception;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBException;
import javax.naming.InitialContext;

/**
 * Tests that invocations on EJB2.x beans which result in exceptions, have the original cause available
 * in the propagated exception
 *
 * @author Jaikiran Pai
 * @see https://issues.jboss.org/browse/AS7-5432
 */
@RunWith(Arquillian.class)
public class ExceptionPropagationTestCase {

    private static final Logger logger = Logger.getLogger(ExceptionPropagationTestCase.class);

    private static final String MODULE_NAME = "exception-propagation-ejb2x";

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(ExceptionPropagationTestCase.class.getPackage());
        ejbJar.addAsManifestResource(ExceptionPropagationTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return ejbJar;
    }

    /**
     * Tests that when an invocation on an EJB 2.x bean throws a runtime exception which triggers {@link javax.ejb.TransactionRolledbackLocalException}
     * then the original cause is retained in the exception that's returned back to the client
     *
     * @throws Exception
     */
    @Test
    public void testExceptionPropagation() throws Exception {
        final Session21LocalHome localHome = InitialContext.doLookup("java:module/" + Session21Bean.class.getSimpleName() + "!" + Session21LocalHome.class.getName());
        final Session21Local localEjb2xBean = localHome.create();
        try {
            localEjb2xBean.invokeOnSelfToThrowCustomRuntimeException();
            Assert.fail("Expected the invocation on the EJB 2.x bean to intentionally fail with exception, but it didn't");
        } catch (EJBException ejbex) {
            boolean foundExpectedException = false;
            Throwable cause = ejbex.getCause();
            Assert.assertNotNull("The underlying cause of the EJBException was expected to be non-null");
            while (cause != null) {
                if (cause instanceof CustomRuntimeException) {
                    foundExpectedException = true;
                    break;
                }
                cause = cause.getCause();
            }
            Assert.assertTrue("Did not find the expected " + CustomRuntimeException.class.getName() + " in the exception stacktrace", foundExpectedException);
        }
    }
}
