/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.osgi.management;

import static org.jboss.as.test.osgi.OSGiManagementOperations.getFrameworkStartLevel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.osgi.OSGiManagementOperations;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test OSGi management operations
 *
 * @author thomas.diesler@jboss.com
 * @since 06-Mar-2012
 */
@RunAsClient
@RunWith(Arquillian.class)
public class OSGiManagementTestCase {

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        archive.addClass(OSGiManagementTestCase.class);
        return archive;
    }

    @Test
    public void testFrameworkActivation() throws Exception {

        // Get the current startlevel
        String startLevel = getFrameworkStartLevel(getControllerClient());

        // If the startlevel is not defined the subsystem is down
        // [TODO] define a more explicit runtime atttribute
        if ("undefined".equals(startLevel)) {
            OSGiManagementOperations.activateFramework(getControllerClient());
            assertEquals("1", getFrameworkStartLevel(getControllerClient()));
        } else if ("1".equals(startLevel)) {
            // Nothing to do
        } else {
            fail("Unexpected startlevel: " + startLevel);
        }
    }

    @Test
    @Ignore
    public void testFrameworkProperties() throws Exception {
        // [TODO] testFrameworkProperties
    }

    @Test
    @Ignore
    public void testAddRemoveCapabilities() throws Exception {
        // [TODO] testAddRemoveCapabilities
    }

    @Test
    @Ignore
    public void testChangeCapabilityStartlevel() throws Exception {
        // [TODO] testChangeCapabilityStartlevel
    }

    @Test
    @Ignore
    public void testConfigAdminOperations() throws Exception {
        // [TODO] testConfigAdminOperations
    }

    @Test
    @Ignore
    public void testRuntimeOperations() throws Exception {
        // [TODO] testRuntimeOperations
    }

    // [TODO] any other management operations that need testing?

    private ModelControllerClient getControllerClient() {
        return managementClient.getControllerClient();
    }
}
