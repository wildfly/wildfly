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

package org.jboss.as.test.integration.ejb.stateful.remove;

import javax.ejb.EJB;
import javax.ejb.NoSuchEJBException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the {@link javax.ejb.Remove @Remove} methods on Stateful session beans.
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class RemoveMethodOnSFSBTestCase {

    private static final Logger log = Logger.getLogger(RemoveMethodOnSFSBTestCase.class.getName());

    @Deployment
    public static JavaArchive createDeployment() {
        // create the ejb jar
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "sfsb-remove-method-test.jar");
        jar.addPackage(SFSBWithRemoveMethods.class.getPackage());
        return jar;
    }

    @EJB (mappedName = "java:module/SFSBWithRemoveMethods!org.jboss.as.test.integration.ejb.stateful.remove.SFSBWithRemoveMethods")
    private SFSBWithRemoveMethods sfsbWithRemoveMethods;

    /**
     * Tests that an invocation on a SFSB method annotated with @Remove results in the removal of the bean instance
     */
    @Test
    public void testSimpleRemoveOnSFSB() {
        // remove the SFSB
        sfsbWithRemoveMethods.remove();
        // try invoking again. we should expect a NoSuchEJBException
        try {
            sfsbWithRemoveMethods.remove();
            Assert.fail("SFSB was expected to be removed after a call to the @Remove method");
        } catch (NoSuchEJBException nsee) {
            // expected
            log.trace("Got the expected NoSuchEJBException after invoking remove on the SFSB");
        }
    }

    /**
     * Tests that an invocation on SFSB method annotated with @Remove and with "retainIfException = true" *doesn't* result
     * in removal of the bean when an application exception is thrown.
     */
    @Test
    public void testRemoveWithRetainIfExceptionOnSFSB() {
        // invoke the remove method which throws an app exception
        try {
            sfsbWithRemoveMethods.retainIfAppException();
            Assert.fail("Did not get the expected app exception");
        } catch (SimpleAppException sae) {
            // expected
        }

        // invoke again and it should *not* throw NoSuchEJBException
        try {
            sfsbWithRemoveMethods.retainIfAppException();
            Assert.fail("Did not get the expected app exception on second invocation on SFSB");
        } catch (SimpleAppException sae) {
            // expected
        }

    }

    /**
     * Tests that an invocation on SFSB method annotated with @Remove (and without the retainIfException set to true) results in
     * removal of the bean even in case of application exception.
     */
    @Test
    public void testRemoveEvenIfAppExceptionOnSFSB() throws Exception {

        // invoke the remove method which throws an app exception
        try {
            sfsbWithRemoveMethods.removeEvenIfAppException();
            Assert.fail("Did not get the expected app exception");
        } catch (SimpleAppException sae) {
            // expected
        }

        // invoke again and it *must* throw NoSuchEJBException
        try {
            sfsbWithRemoveMethods.removeEvenIfAppException();
            Assert.fail("Did not get the expected NoSuchEJBException on second invocation on SFSB");
        } catch (NoSuchEJBException nsee) {
            // expected
            log.trace("Got the expected NoSuchEJBException on second invocation on SFSB");
        }

    }

    /**
     * Tests that an invocation on a simple SFSB method which doesn't have the @Remove on it, *doesn't* result in the bean
     * instance removal.
     */
    @Test
    public void testSimpleNonRemoveMethodOnSFSB() {
        sfsbWithRemoveMethods.doNothing();
        sfsbWithRemoveMethods.doNothing();
        sfsbWithRemoveMethods.doNothing();
    }

    /**
     * Tests that an invocation on SFSB base class method annotated with @Remove (and without the retainIfException set to true) results in
     * removal of the bean even in case of application exception.
     */
    @Test
    public void testRemoveEvenIfAppExceptionOnSFSBBaseClass() {
        // invoke the remove method which throws an app exception
        try {
            sfsbWithRemoveMethods.baseRemoveEvenIfAppException();
            Assert.fail("Did not get the expected app exception");
        } catch (SimpleAppException sae) {
            // expected
        }

        // invoke again and it *must* throw NoSuchEJBException
        try {
            sfsbWithRemoveMethods.doNothing();
            Assert.fail("Did not get the expected NoSuchEJBException on second invocation on SFSB");
        } catch (NoSuchEJBException nsee) {
            // expected
            log.trace("Got the expected NoSuchEJBException on second invocation on SFSB");
        }
    }

    /**
     * Tests that an invocation on a SFSB base class method annotated with @Remove results in the removal of the bean instance
     */
    @Test
    public void testSimpleRemoveOnSFSBBaseClass() {
        // remove the SFSB
        sfsbWithRemoveMethods.baseJustRemove();
        // try invoking again. we should expect a NoSuchEJBException
        try {
            sfsbWithRemoveMethods.doNothing();
            Assert.fail("SFSB was expected to be removed after a call to the @Remove method");
        } catch (NoSuchEJBException nsee) {
            // expected
            log.trace("Got the expected NoSuchEJBException after invoking remove on the SFSB");
        }
    }

    /**
     * Tests that an invocation on SFSB base class method annotated with @Remove and with "retainIfException = true" *doesn't* result
     * in removal of the bean when an application exception is thrown.
     */
    @Test
    public void testRemoveWithRetainIfExceptionOnSFSBBaseClass() {
        // invoke the remove method which throws an app exception
        try {
            sfsbWithRemoveMethods.baseRetainIfAppException();
            Assert.fail("Did not get the expected app exception");
        } catch (SimpleAppException sae) {
            // expected
        }

        // invoke again and it should *not* throw NoSuchEJBException
        try {
            sfsbWithRemoveMethods.baseRetainIfAppException();
            Assert.fail("Did not get the expected app exception on second invocation on SFSB");
        } catch (SimpleAppException sae) {
            // expected
        }

    }
}
