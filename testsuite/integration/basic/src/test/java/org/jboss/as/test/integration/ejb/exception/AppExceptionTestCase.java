/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;

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
