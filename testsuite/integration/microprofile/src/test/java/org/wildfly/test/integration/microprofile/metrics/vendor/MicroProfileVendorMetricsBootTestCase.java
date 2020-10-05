/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.metrics.vendor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.SocketPermission;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.PropertyPermission;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestLogHandlerSetupTask;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.as.test.shared.util.LoggingUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.metrics.TestApplication;
import org.wildfly.test.integration.microprofile.metrics.application.resource.ResourceSimple;
import org.wildfly.test.integration.microprofile.metrics.secured.MicroProfileMetricsSecuredEndpointSetupTask;

/**
 * Test availability of vendor metrics during boot.
 *
 * @author Brian Stansberry 2020 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({MicroProfileMetricsSecuredEndpointSetupTask.class, MicroProfileVendorMetricsBootTestCase.TestLogHandlerSetup.class})
public class MicroProfileVendorMetricsBootTestCase {

    static final String FIRST = "MicroProfileVendorMetricsBootTestCase_first";
    private static final String SECOND = "MicroProfileVendorMetricsBootTestCase_second";
    private static final String TEST_HANDLER_NAME;
    private static final String TEST_LOG_FILE_NAME;
    private static final String LOG_MESSAGE;

    static {
        /*
         * Make both the test handler name and the test log file specific for this class and execution so that we do not
         * interfere with other test classes or multiple subsequent executions of this class against the same container
         */
        TEST_HANDLER_NAME = "test-" + MicroProfileVendorMetricsBootTestCase.class.getSimpleName();
        TEST_LOG_FILE_NAME = TEST_HANDLER_NAME + ".log";
        LOG_MESSAGE = "WFLYCTL0379";
    }

    public static class TestLogHandlerSetup extends TestLogHandlerSetupTask {

        @Override
        public Collection<String> getCategories() {
            return Arrays.asList("io.smallrye.metrics", MicroProfileVendorMetricsBootTestCase.class.getPackage().getName());
        }

        @Override
        public String getLevel() {
            return "INFO";
        }
        @Override
        public String getHandlerName() {
            return TEST_HANDLER_NAME;
        }
        @Override
        public String getLogFileName() {
            return TEST_LOG_FILE_NAME;
        }
    }

    @Deployment(name = FIRST, order = 1)
    public static Archive<?> deployFirst() {
        return ShrinkWrap.create(WebArchive.class, FIRST + ".war")
                .addClasses(TestApplication.class, MicroProfileMetricsSecuredEndpointSetupTask.class,
                        TestLogHandlerSetup.class, TestLogHandlerSetupTask.class)
                .addClass(ResourceSimple.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Deployment(name = SECOND, order = 2)
    public static Archive<?> deploySecond() throws URISyntaxException, MalformedURLException {
        URL url = getManagementURL();

        return ShrinkWrap.create(WebArchive.class, SECOND + ".war")
                // TODO why is including MicroProfileMetricsSecuredEndpointSetupTask.class in the war necessary?
                .addClasses(BootCheckApplication.class, MicroProfileMetricsSecuredEndpointSetupTask.class,
                        TestLogHandlerSetup.class, TestLogHandlerSetupTask.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset("managementURL=" + url.toExternalForm()), "microprofile-config.properties")
                .addAsManifestResource(new StringAsset("Dependencies: org.apache.httpcomponents.core\n"), "MANIFEST.MF")
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                        new SocketPermission(url.getHost() + ":" + url.getPort(), "connect"),
                        new PropertyPermission("managementURL", "read")
                ), "permissions.xml")
                .addAsManifestResource(MicroProfileVendorMetricsBootTestCase.class.getPackage(), "jboss-all.xml", "jboss-all.xml");
    }

    @ContainerResource
    static ManagementClient managementClient;

    @BeforeClass
    public static void assumeNoSecurityManager() {
        // HttpClientBuilder.build in the app wants to read a file from its jar which
        // is typically found in the local maven repo. Too much hassle to add a perm
        // to allow that; it would basically have to be granted perms to read everything
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    private static URL getManagementURL() throws URISyntaxException, MalformedURLException {
        return new URI("http", null, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort(), null, null, null).toURL();
    }

    @Test
    public void testVendorMetricsDeployAfterBoot(@ArquillianResource @OperateOnDeployment(SECOND) URL url) throws Exception {

        checkResponse(performCall(url), "true");

        Assert.assertFalse("'System boot is in process' warning found", LoggingUtil.hasLogMessage(managementClient, TEST_HANDLER_NAME, LOG_MESSAGE));
        // Sanity checks of the log handler setup
        //LoggingUtil.dumpTestLog(managementClient, TEST_HANDLER_NAME);
        Assert.assertTrue("Deployment's Initializing log not found", LoggingUtil.hasLogMessage(managementClient, TEST_HANDLER_NAME, "Initializing"));
        Assert.assertTrue("Deployment's Initialized log not found", LoggingUtil.hasLogMessage(managementClient, TEST_HANDLER_NAME, "Initialized"));

        ServerReload.executeReloadAndWaitForCompletion(managementClient);

        checkResponse(performCall(url), "false");

        Assert.assertFalse("'System boot is in process' warning found", LoggingUtil.hasLogMessage(managementClient, TEST_HANDLER_NAME, LOG_MESSAGE));
    }

    private static String performCall(URL url) throws Exception {
        URL appURL = new URL(url.toExternalForm() + "BootCheckServlet");
        return HttpRequest.get(appURL.toExternalForm(), 10, TimeUnit.SECONDS);
    }

    private static void checkResponse(String response, String sawVendor) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(response));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("connected=")) {
                Assert.assertEquals(response, "true", line.substring("connected=".length()));
            } else if (line.startsWith("cleanInit=")) {
                Assert.assertEquals(response, "true", line.substring("cleanInit=".length()));
            } else if (line.startsWith("overallResponseCode=")) {
                Assert.assertEquals(response, "200", line.substring("overallResponseCode=".length()));
            } else if (line.startsWith("overallSawVendor=")) {
                Assert.assertEquals(response, sawVendor, line.substring("overallSawVendor=".length()));
            } else if (line.startsWith("scopedResponseCode=")) {
                String respCode = line.substring("scopedResponseCode=".length());
                if ("false".equals(sawVendor)) {
                    // Different releases produce a different response code
                    Assert.assertTrue(response, "200".equals(respCode) || "204".equals(respCode));
                } else {
                    Assert.assertEquals(response, "200", respCode);
                }
            } else if (line.startsWith("scopedSawVendor=")) {
                Assert.assertEquals(response, sawVendor, line.substring("scopedSawVendor=".length()));
            }
        }
    }

}
