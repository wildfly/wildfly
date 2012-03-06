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

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test OSGi management operations
 *
 * @author thomas.diesler@jboss.com
 * @since 06-Mar-2012
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OSGiManagementTestCase extends AbstractCliTestBase {

    @BeforeClass
    public static void before() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
    }

    @Test
    public void testFrameworkActivation() throws Exception {

        // Get the current startlevel
        String startLevel = getFrameworkStartLevel();
        
        // If the startlevel is not defined the subsystem is down
        // [TODO] define a more explicit runtime atttribute
        if ("undefined".equals(startLevel)) {
            cli.sendLine("/subsystem=osgi:activate");
            CLIOpResult cliresult = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
            assertTrue(cliresult.isIsOutcomeSuccess());
            assertEquals("1", getFrameworkStartLevel());
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

    private String getFrameworkStartLevel() throws Exception {
        cli.sendLine("/subsystem=osgi:read-attribute(name=startlevel)");
        CLIOpResult cliresult = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);

        assertTrue(cliresult.isIsOutcomeSuccess());
        return (String)cliresult.getResult();
    }
}
