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

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test OSGi management operations
 *
 * @author thomas.diesler@jboss.com
 * @author David Bosschaert
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
        boolean active = isFrameworkActive();
        if (!active) {
            callCLI("/subsystem=osgi:activate");
            assertTrue(isFrameworkActive());
        }
    }

    @Test
    public void testStartLevel() throws Exception {
        ensureFrameworkActive();

        String initial = getFrameworkStartLevel();
        try {
            assertEquals("1", initial);

            CLIOpResult b1Result = callCLI("/subsystem=osgi/bundle=1:read-resource(include-runtime=true)");
            Map<?, ?> resultMap = (Map<?, ?>) b1Result.getResult();
            assertEquals("ACTIVE", resultMap.get("state"));

            callCLI("/subsystem=osgi:write-attribute(name=startlevel,value=0");
            assertEquals("0", getFrameworkStartLevel());

            CLIOpResult b1Result2 = callCLI("/subsystem=osgi/bundle=1:read-resource(include-runtime=true)");
            Map<?, ?> resultMap2 = (Map<?, ?>) b1Result2.getResult();
            assertEquals("RESOLVED", resultMap2.get("state"));
        } finally {
            callCLI("/subsystem=osgi:write-attribute(name=startlevel,value=" + initial);
        }
    }

    @Test
    public void testBundleRuntimeOperations() throws Exception {
        // No need to ensure the framework is active. If it isn't deploying a bundle
        // should trigger it into active mode.

        // Create and deploy a test bundle
        File testBundle = File.createTempFile("testbundle", ".jar");
        JavaArchive bundleArchive = createTestBundle("test-bundle");
        bundleArchive.as(ZipExporter.class).exportTo(testBundle, true);

        cli.sendLine("deploy " + testBundle.getAbsolutePath());
        long testBundleId = findBundleId("test-bundle");
        assertTrue(testBundleId > 0);

        // Read the bundle information and check that it is correct
        String readCommand = "/subsystem=osgi/bundle=" + testBundleId + ":read-resource(include-runtime=true)";
        CLIOpResult cliresult = callCLI(readCommand);
        Map<?, ?> resultMap = (Map<?, ?>) cliresult.getResult();

        assertEquals(testBundleId + "L", resultMap.get("id"));
        assertEquals("ACTIVE", resultMap.get("state"));
        assertEquals("1", resultMap.get("startlevel"));
        assertEquals("bundle", resultMap.get("type"));
        assertEquals("test-bundle", resultMap.get("symbolic-name"));
        assertEquals("1.0.1", resultMap.get("version"));

        // Stop the bundle and start it again
        callCLI("/subsystem=osgi/bundle=" + testBundleId + ":stop");
        CLIOpResult cliresult2 = callCLI(readCommand);
        Map<?, ?> resultMap2 = (Map<?, ?>) cliresult2.getResult();
        assertEquals("RESOLVED", resultMap2.get("state"));

        callCLI("/subsystem=osgi/bundle=" + testBundleId + ":start");
        CLIOpResult cliresult3 = callCLI(readCommand);
        Map<?, ?> resultMap3 = (Map<?, ?>) cliresult3.getResult();
        assertEquals("ACTIVE", resultMap3.get("state"));

        // Create and deploy a test fragment
        File testFragment = File.createTempFile("testfragment", ".jar");
        JavaArchive fragmentArchive = createTestFragment();
        fragmentArchive.as(ZipExporter.class).exportTo(testFragment, true);

        cli.sendLine("deploy " + testFragment.getAbsolutePath());
        long testFragId = findBundleId("test-fragment");
        assertTrue(testFragId > 0);

        // Read the fragment information and check that it is correct
        CLIOpResult cliresult4 = callCLI("/subsystem=osgi/bundle=" + testFragId + ":read-resource(include-runtime=true)");
        Map<?, ?> resultMap4 = (Map<?, ?>) cliresult4.getResult();
        assertEquals(testFragId + "L", resultMap4.get("id"));
        assertEquals("fragment", resultMap4.get("type"));
        assertEquals("test-fragment", resultMap4.get("symbolic-name"));

        // Undeploy both the fragment and bundle and check that they're gone
        cli.sendLine("undeploy " + testFragment.getName());
        cli.sendLine("undeploy " + testBundle.getName());

        CLIOpResult cliresult5 = callCLI("/subsystem=osgi:read-children-names(child-type=bundle)");
        List<?> bundleIDs = (List<?>) cliresult5.getResult();
        assertFalse("The test bundle should have been removed, as it was undeployed",
                bundleIDs.contains("" + testBundleId));
        assertFalse("The test fragment should have been removed, as it was undeployed",
                bundleIDs.contains("" + testFragId));

        assertTrue("Unable to delete test bundle. Are all streams properly closed?", testBundle.delete());
        assertTrue("Unable to delete test fragment. Are all streams properly closed?", testFragment.delete());
    }

    @Test
    public void testFrameworkProperties() throws Exception {
        String propName = "testProp" + System.currentTimeMillis();
        callCLI("/subsystem=osgi/property=" + propName + ":add(value=testing123testing)");

        CLIOpResult cliresult = callCLI("/subsystem=osgi/property=" + propName + ":read-resource");
        assertEquals("testing123testing", ((Map<?,?>)cliresult.getResult()).get("value"));

        CLIOpResult cliresult1 = callCLI("/subsystem=osgi:read-children-names(child-type=property)");
        List<?> propertyNames = (List<?>) cliresult1.getResult();
        assertTrue(propertyNames.contains(propName));

        callCLI("/subsystem=osgi/property=" + propName + ":remove");

        CLIOpResult cliresult2 = callCLI("/subsystem=osgi:read-children-names(child-type=property)");
        List<?> propertyNames2 = (List<?>) cliresult2.getResult();
        assertFalse(propertyNames2.contains(propName));
    }

    @Test
    public void testAddRemoveCapabilities() throws Exception {
        ensureFrameworkActive();

        String capabilityName = "org.jboss.test.testcap" + System.currentTimeMillis();
        String basedir = System.getProperty("jboss.dist");
        File targetdir = new File(basedir + "/bundles/" + capabilityName.replace(".", "/") + "/main");
        targetdir.mkdirs();

        File bundleFile = new File(targetdir, "testcapabilitybundle.jar");
        JavaArchive capabilityBundle = createTestBundle("capability-bundle");
        capabilityBundle.as(ZipExporter.class).exportTo(bundleFile);

        callCLI("/subsystem=osgi/capability=" + capabilityName + ":add(startlevel=1)");

        CLIOpResult cliresult1 = callCLI("/subsystem=osgi:read-children-names(child-type=capability)");
        List<?> capNames = (List<?>) cliresult1.getResult();
        assertTrue(capNames.contains(capabilityName));

        long capBundleId = findBundleId("capability-bundle");
        assertTrue(capBundleId > 0);

        CLIOpResult cliresult2 = callCLI("/subsystem=osgi/bundle=" + capBundleId + ":read-resource(include-runtime=true)");
        Map<?, ?> resultMap = (Map<?, ?>) cliresult2.getResult();
        assertEquals("ACTIVE", resultMap.get("state"));

        callCLI("/subsystem=osgi/capability=" + capabilityName + ":remove");

        CLIOpResult cliresult3 = callCLI("/subsystem=osgi:read-children-names(child-type=capability)");
        List<?> capNames2 = (List<?>) cliresult3.getResult();
        assertFalse(capNames2.contains(capabilityName));

        // clean up
        bundleFile.delete();
    }

    @Test
    public void testActivationMode() throws Exception {
        CLIOpResult cliresult = callCLI("/subsystem=osgi:read-attribute(name=activation)");
        assertEquals("lazy", cliresult.getResult());

        try {
            callCLI("/subsystem=osgi:write-attribute(name=activation,value=eager)");

            CLIOpResult cliresult2 = callCLI("/subsystem=osgi:read-attribute(name=activation)");
            assertEquals("eager", cliresult2.getResult());
        } finally {
            callCLI("/subsystem=osgi:write-attribute(name=activation,value=lazy)");
        }
    }

    @SuppressWarnings("unchecked")
    private long findBundleId(String bsn) throws Exception {
        CLIOpResult cliresult = callCLI("/subsystem=osgi:read-children-resources(recursive=true,include-runtime=true,child-type=bundle)");
        long bundleId = -1;
        Map<String, Map<String, Object>> resultMap = (Map<String, Map<String, Object>>) cliresult.getResult();
        for (Map<String, Object> map : resultMap.values()) {
            if (bsn.equals(map.get("symbolic-name"))) {
                String id = ((String) map.get("id")).trim();
                if (id.endsWith("L"))
                    id = id.substring(0, id.length() - 1);
                bundleId = Long.parseLong(id);
                break;
            }
        }
        return bundleId;
    }

    private boolean isFrameworkActive() throws Exception {
        CLIOpResult cliresult = callCLI("/subsystem=osgi:read-children-names(child-type=bundle)");
        return ((List<?>) cliresult.getResult()).size() > 0;
    }

    private void ensureFrameworkActive() throws Exception {
        boolean active = isFrameworkActive();
        if (!active) {
            callCLI("/subsystem=osgi:activate");
            assertTrue(isFrameworkActive());
        }
    }

    private String getFrameworkStartLevel() throws Exception {
        CLIOpResult cliresult = callCLI("/subsystem=osgi:read-attribute(name=startlevel)");

        assertTrue(cliresult.isIsOutcomeSuccess());
        return (String)cliresult.getResult();
    }

    private CLIOpResult callCLI(String line) throws Exception {
        cli.sendLine(line);
        CLIOpResult cliresult = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertTrue(cliresult.isIsOutcomeSuccess());
        return cliresult;
    }

    private static JavaArchive createTestBundle(String bsn) {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, bsn);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleVersion("1.0.1");
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        return archive;
    }

    private static JavaArchive createTestFragment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "test-fragment");
        archive.setManifest(new Asset() {
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
}
