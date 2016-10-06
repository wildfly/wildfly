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
package org.jboss.as.test.integration.osgi.configadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.configadmin.ConfigAdminManagementOperations;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.osgi.configadmin.bundle.TestBundleActivator;
import org.jboss.as.test.integration.osgi.configadmin.bundle.TestBundleActivator2;
import org.jboss.as.test.osgi.OSGiManagementOperations;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Test Config Admin management operations.
 *
 * @author David Bosschaert
 * @author Thomas Diesler
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ConfigAdminManagementTestCase extends AbstractCliTestBase {
    @ContainerResource
    ManagementClient managementClient;

    // The testable attribute is set to false to avoid the Arquillian enhancers from enhancing this deployment, which is
    // not needed here as the test itself is run outside of the OSGi framework (@RunAsClient).
    @Deployment(name = "test-config-admin", testable = false)
    public static JavaArchive createConfigAdminBundle() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "test-config-admin");
        archive.addClasses(TestBundleActivator.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleVersion(new Version(1, 0, 0));
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(TestBundleActivator.class);
                builder.addImportPackages(BundleActivator.class, ConfigurationAdmin.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = "test-config-admin2", testable = false)
    public static JavaArchive createTestBundle2() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "test-config-admin2");
        archive.addClasses(TestBundleActivator2.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(TestBundleActivator2.class);
                builder.addImportPackages(BundleActivator.class, ConfigurationAdmin.class, ServiceTracker.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    /**
     * Tests that configuration Admin Changes made from the DMR arrive in the OSGi Config Admin Managed Service.
     */
    @Test
    public void testConfigAdminWriteFromDMR() throws Exception {
        Long bundleId = OSGiManagementOperations.getBundleId(getControllerClient(), "test-config-admin", Version.parseVersion("1.0"));
        assertTrue(bundleId > 0);

        assertTrue(OSGiManagementOperations.bundleStart(getControllerClient(), bundleId));
        assertEquals("ACTIVE", OSGiManagementOperations.getBundleState(getControllerClient(), bundleId));

        // The ManagedService will write the info it receives back to this file.
        File f = File.createTempFile("ConfigAdminManagementTest", ".tmp");

        try {
            String configName = TestBundleActivator.class.getName();
            Map<String, String> entries = new HashMap<String, String>();
            entries.put("file", f.getAbsolutePath());
            entries.put("value", "initial");
            assertTrue(ConfigAdminManagementOperations.addConfiguration(getControllerClient(), configName, entries));

            // listConfigurations
            Map<String, String> config = ConfigAdminManagementOperations.readConfiguration(getControllerClient(), configName);
            assertEquals(entries, config);

            // Check the specified file for the content specified in the configuration
            assertEquals("The managed service in the deployed bundle should have received the configuration and updated the file",
                    "initial", readTextFile(f));

            assertTrue(ConfigAdminManagementOperations.listConfigurations(getControllerClient()).contains(configName));
            assertTrue(ConfigAdminManagementOperations.removeConfiguration(getControllerClient(), configName));
            assertFalse(ConfigAdminManagementOperations.listConfigurations(getControllerClient()).contains(configName));
        } finally {
            // Delete the temporary file.
            f.delete();
        }
    }

    /**
     * Test that write to the OSGi Config Admin Service from a bundle are visible in the AS7 DMR interface.
     */
    @Test
    public void testConfigAdminWriteFromBundle() throws Exception {
        String pid = TestBundleActivator2.class.getName();
        assertFalse("Precondition", ConfigAdminManagementOperations.listConfigurations(getControllerClient()).contains(pid));

        long bundleId = OSGiManagementOperations.getBundleId(getControllerClient(), "test-config-admin2", new Version(0, 0, 0));
        assertTrue(bundleId > 0);

        assertTrue(OSGiManagementOperations.bundleStart(getControllerClient(), bundleId));
        assertEquals("ACTIVE", OSGiManagementOperations.getBundleState(getControllerClient(), bundleId));

        Map<String, String> config = ConfigAdminManagementOperations.readConfiguration(getControllerClient(), pid);
        assertEquals("hi from a bundle activator", config.get("from.activator"));

        assertTrue(ConfigAdminManagementOperations.removeConfiguration(getControllerClient(), pid));
        assertFalse(ConfigAdminManagementOperations.listConfigurations(getControllerClient()).contains(pid));
    }

    private ModelControllerClient getControllerClient() {
        return managementClient.getControllerClient();
    }

    private String readTextFile(File file) throws FileNotFoundException {
        // The following reads the file into a string
        return new Scanner(file).useDelimiter("\\A").next();
    }
}
