/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ejb.EJB;
import javax.ejb.EJBException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * AS7-1317
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
public class AppExceptionTestCase {
    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class, "ejb3-app-exception.jar")
            .addPackage(Beanie.class.getPackage())
            .addAsManifestResource(AppExceptionTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
    }

    @EJB(mappedName = "java:global/ejb3-app-exception/Beanie")
    private BeanieLocal bean;

    @Test
    public void testAppException() {
        try {
            bean.callThrowException();
        } catch (EJBException e) {
            // EJB 3.1 FR 14.3.1: a system exception is rethrown as an EJBException
            final Exception cause1 = e.getCausedByException();
            assertNotNull(cause1);
            assertEquals(RuntimeException.class, cause1.getClass());
            final Throwable cause2 = cause1.getCause();
            assertNotNull(cause2);
            assertEquals(Exception.class, cause2.getClass());
            assertEquals("This is an app exception", cause2.getMessage());
        }
    }

    /**
     *AS7-5926
     */
    @Test
    public void testAppExceptionNever() {
        try {
            bean.callThrowExceptionNever();
        } catch (EJBException e) {
            // EJB 3.1 FR 14.3.1: a system exception is rethrown as an EJBException
            final Exception cause1 = e.getCausedByException();
            assertNotNull(cause1);
            assertEquals(RuntimeException.class, cause1.getClass());
            final Throwable cause2 = cause1.getCause();
            assertNotNull(cause2);
            assertEquals(Exception.class, cause2.getClass());
            assertEquals("This is an app exception", cause2.getMessage());
        }
    }

    @Test
    public void testXmlAppException() {
        try {
            bean.throwXmlAppException();
            Assert.fail("should have thrown exception");
        } catch (XmlAppException e) {

        }
    }
}
