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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.osgi.parser.ModelConstants;
import org.jboss.as.test.osgi.FrameworkManagement;
import org.jboss.dmr.ModelNode;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Version;

/**
 * Test OSGi management operations
 *
 * @author thomas.diesler@jboss.com
 * @author David Bosschaert
 * @since 06-Mar-2012
 */
@RunAsClient
@RunWith(Arquillian.class)
public class FrameworkManagementTestCase {
    @ContainerResource
    ManagementClient managementClient;

    @Deployment(name = "test-bundle", managed = false, testable = false)
    public static JavaArchive createTestBundle() {
        return createTestBundle("test-bundle", "999");
    }

    @Deployment(name = "test-bundle2", managed = false, testable = false)
    public static JavaArchive createTestBundle2() {
        return createTestBundle("test-bundle2", "1.2.3.something");
    }

    private static JavaArchive createTestBundle(final String bsn, final String version) {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, bsn);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(bsn);
                builder.addBundleVersion(version);
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = "test-fragment", managed = false, testable = false)
    public static JavaArchive createTestFragment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "test-fragment");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addFragmentHost("test-bundle");
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testActivationMode() throws Exception {
        boolean initialActivationState = isFrameworkActive();

        assertEquals("eager", FrameworkManagement.getActivationMode(getControllerClient()));
        try {
            assertTrue(FrameworkManagement.setActivationMode(getControllerClient(), "lazy"));
            assertEquals("lazy", FrameworkManagement.getActivationMode(getControllerClient()));

            assertEquals("This operation should not change the active status of the subsystem", initialActivationState, isFrameworkActive());
        } finally {
            // set it back
            assertTrue(FrameworkManagement.setActivationMode(getControllerClient(), "eager"));
        }
    }

    @Test
    public void testFrameworkProperties() throws Exception {
        boolean initialActivationState = isFrameworkActive();

        String propName = "testProp" + System.currentTimeMillis();
        assertTrue(FrameworkManagement.addProperty(getControllerClient(), propName, "testing123testing"));
        assertEquals("testing123testing", FrameworkManagement.readProperty(getControllerClient(), propName));
        assertTrue(FrameworkManagement.listProperties(getControllerClient()).contains(propName));
        assertTrue(FrameworkManagement.removeProperty(getControllerClient(), propName));
        assertFalse(FrameworkManagement.listProperties(getControllerClient()).contains(propName));

        assertEquals("This operation should not change the active status of the subsystem", initialActivationState, isFrameworkActive());
    }

    @Test
    public void testAddRemoveCapabilities() throws Exception {
        ensureFrameworkActive();

        String capabilityName = "org.jboss.test.testcap" + System.currentTimeMillis();
        String basedir = System.getProperty("jboss.dist");
        File targetdir = new File(basedir + "/bundles/" + capabilityName.replace(".", "/") + "/main");
        targetdir.mkdirs();

        File bundleFile = new File(targetdir, "testcapabilitybundle.jar");
        JavaArchive capabilityBundle = createTestBundle("capability-bundle", "1.0.1");
        capabilityBundle.as(ZipExporter.class).exportTo(bundleFile);

        assertTrue(FrameworkManagement.addCapability(getControllerClient(), capabilityName, 1));
        assertTrue(FrameworkManagement.listCapabilities(getControllerClient()).contains(capabilityName));

        Long capBundleId = FrameworkManagement.getBundleId(getControllerClient(), "capability-bundle", Version.parseVersion("1.0.1"));
        assertEquals("The capability bundle should not yet be added to the system, this requires a restart", new Long(-1), capBundleId);

        assertTrue(FrameworkManagement.removeCapability(getControllerClient(), capabilityName));
        assertFalse(FrameworkManagement.listCapabilities(getControllerClient()).contains(capabilityName));

        // clean up
        bundleFile.delete();
    }

    @Test
    public void testFrameworkActivation() throws Exception {
        boolean active = isFrameworkActive();
        if (!active) {
            FrameworkManagement.activateFramework(getControllerClient());
            assertTrue(isFrameworkActive());
        }
    }

    @Test
    public void testBundleRuntimeOperations(@ArquillianResource Deployer deployer) throws Exception {
        // No need to ensure the framework is active. If it isn't deploying a bundle
        // should trigger it into active mode.

        deployer.deploy("test-bundle");
        Long testBundleId = FrameworkManagement.getBundleId(getControllerClient(), "test-bundle", Version.parseVersion("999"));
        assertTrue(testBundleId > 0);

        ModelNode resultMap = FrameworkManagement.getBundleInfo(getControllerClient(), testBundleId);
        assertEquals(testBundleId.toString(), resultMap.get("id").asString());
        assertEquals("ACTIVE", resultMap.get("state").asString());
        assertEquals("1", resultMap.get("startlevel").asString());
        assertEquals("bundle", resultMap.get("type").asString());
        assertEquals("test-bundle", resultMap.get("symbolic-name").asString());
        assertEquals("999.0.0", resultMap.get("version").asString());

        assertTrue(FrameworkManagement.bundleStop(getControllerClient(), testBundleId));
        ModelNode resultMap3 = FrameworkManagement.getBundleInfo(getControllerClient(), testBundleId);
        assertEquals("RESOLVED", resultMap3.get("state").asString());

        assertTrue(FrameworkManagement.bundleStart(getControllerClient(), testBundleId));
        ModelNode resultMap2 = FrameworkManagement.getBundleInfo(getControllerClient(), testBundleId);
        assertEquals("ACTIVE", resultMap2.get("state").asString());

        deployer.deploy("test-fragment");
        Long testFragId = FrameworkManagement.getBundleId(getControllerClient(), "test-fragment", Version.parseVersion("0.0.0"));
        assertTrue(testFragId > 0);

        ModelNode resultMap4 = FrameworkManagement.getBundleInfo(getControllerClient(), testFragId);
        assertEquals(testFragId.toString(), resultMap4.get("id").asString());
        assertEquals("fragment", resultMap4.get("type").asString());
        assertEquals("test-fragment", resultMap4.get("symbolic-name").asString());
        assertEquals("0.0.0", resultMap4.get("version").asString());

        deployer.undeploy("test-fragment");
        deployer.undeploy("test-bundle");

        Long testBundleId2 = FrameworkManagement.getBundleId(getControllerClient(), "test-bundle", Version.parseVersion("999"));
        assertEquals("Bundle should have been undeployed", new Long(-1), testBundleId2);
        Long testFragId2 = FrameworkManagement.getBundleId(getControllerClient(), "test-fragment", Version.parseVersion("0.0.0"));
        assertEquals("Fragment should have been undeployed", new Long(-1), testFragId2);
    }

    @Test
    public void testStartLevel(@ArquillianResource Deployer deployer) throws Exception {
        ensureFrameworkActive();

        int initial = FrameworkManagement.getFrameworkStartLevel(getControllerClient());
        try {
            assertEquals(1, initial);

            deployer.deploy("test-bundle2");
            Long testBundleId = FrameworkManagement.getBundleId(getControllerClient(), "test-bundle2", Version.parseVersion("1.2.3.something"));
            assertTrue(testBundleId > 0);

            assertTrue(FrameworkManagement.bundleStart(getControllerClient(), testBundleId));
            ModelNode resultMap = FrameworkManagement.getBundleInfo(getControllerClient(), testBundleId);
            assertEquals("ACTIVE", resultMap.get(ModelConstants.STATE).asString());

            assertTrue(FrameworkManagement.setFrameworkStartLevel(getControllerClient(), 0));
            if (!waitForBundleStateAfterStartLevelChange("RESOLVED", testBundleId, 10000)) {
                fail("Bundle not RESOLVED");
            }
        } finally {
            assertTrue(FrameworkManagement.setFrameworkStartLevel(getControllerClient(), initial));
        }
    }

    private boolean waitForBundleStateAfterStartLevelChange(String state, long bundleId, int timeout) throws Exception {
        while (timeout > 0) {
            ModelNode node = FrameworkManagement.getBundleInfo(getControllerClient(), bundleId);
            if (state.equals(node.get(ModelConstants.STATE).asString())) {
                return true;
            }
            timeout -= 500;
            Thread.sleep(500);
        };
        return false;
    }

    private ModelControllerClient getControllerClient() {
        return managementClient.getControllerClient();
    }

    private boolean isFrameworkActive() throws Exception {
        return FrameworkManagement.listBundleIDs(getControllerClient()).size() > 0;
    }

    private void ensureFrameworkActive() throws Exception {
        boolean active = isFrameworkActive();
        if (!active) {
            FrameworkManagement.activateFramework(getControllerClient());
            assertTrue("Framework should be active", isFrameworkActive());
        }
    }
}
