/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.management;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.shared.logging.LoggingUtil.hasLogMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Set;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.logging.TestLogHandlerSetupTask;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ServerControl;

/**
 * Tests of behavior of servers provisioned using a channel.
 * <p>
 * Tests in this class use the 'wildfly-multi-channel' installation, which is provisioned using
 * three channels:
 * <ol>
 *     <li>A channel for WildFly itself</li>
 *     <li>A channel for Prospero</li>
 *     <li>A custom channel that is part of this testsuite, used to override artifact versions from the other channels</li>
 * </ol>
 */
@RunAsClient()
@RunWith(Arquillian.class)
@ServerControl(manual = true)
public class ChannelProvisioningTestCase {
    private static final String SERVER_CONFIG_NAME = "wildfly-multi-channel";

    private static final String OVERRIDE_PROD_NAME = "Just a Test";
    private static final String OVERRIDE_PROD_VERSION = "1.2 GA Update 1";

    @BeforeClass
    public static void beforeClass() {
        AssumeTestGroupUtil.assumeNotWildFlyPreview();
        if (AssumeTestGroupUtil.isBootableJar()
                || System.getProperty("external.wildfly.channels") != null
                || System.getProperty("internal.wildfly.channels") != null) {
            throw new AssumptionViolatedException("Unsuitable environment");
        }
    }

    @ArquillianResource
    private static volatile ContainerController container;

    @Before
    public void setup() {
        if (!container.isStarted(SERVER_CONFIG_NAME)) {
            container.start(SERVER_CONFIG_NAME);
        }
    }

    @AfterClass
    public static void tearDown() {
        if (container != null && container.isStarted(SERVER_CONFIG_NAME)) {
            container.stop(SERVER_CONFIG_NAME);
        }
    }

    /**
     * Tests that if a channel overrides the org.jboss.as.product artifact to use different
     * values in {@code ProductConfig} that those values are respected.
     * <p>
     * This test relies on the testsuite/test-product-conf module's org.wildfly:wildfly-ee-product-conf
     * artifact. It uses the custom channel used to provision the 'wildfly-multi-channel' installation
     * to override the default  org.wildfly:wildfly-ee-product-conf artifact.
     *
     * @throws Exception if there are problems managing the server or reading its logs
     */
    @Test
    public void testProductConfOverride() throws Exception {
        try (ModelControllerClient modelControllerClient = TestSuiteEnvironment.getModelControllerClient()) {

            // Validate the override settings in the management API root resource
            ModelNode op = Util.createEmptyOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
            ModelNode response = modelControllerClient.execute(op);
            assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());
            assertEquals(response.toString(), OVERRIDE_PROD_NAME, response.get(RESULT, PRODUCT_NAME).asString());
            assertEquals(response.toString(), OVERRIDE_PROD_VERSION, response.get(RESULT, PRODUCT_VERSION).asString());

            // Validate the override settings appears in the "started" log message.
            LogHandlerSetup logHandlerSetup = new LogHandlerSetup();
            logHandlerSetup.setup(modelControllerClient);
            try {
                // Reload to capture the log message
                ServerReload.executeReloadAndWaitForCompletion(modelControllerClient);
                assertTrue("Clean start message not found", hasLogMessage(modelControllerClient, "startup", "WFLYSRV0025"));
                assertTrue("Expected product name not found", hasLogMessage(modelControllerClient, "startup", "WFLYSRV0025",
                        line -> line.contains(OVERRIDE_PROD_NAME)));
                assertTrue("Expected product version not found", hasLogMessage(modelControllerClient, "startup", "WFLYSRV0025",
                        line -> line.contains(OVERRIDE_PROD_NAME),
                        line -> line.contains(OVERRIDE_PROD_VERSION)));
            } finally {
                logHandlerSetup.tearDown(modelControllerClient);
            }
        }
    }

    private static class LogHandlerSetup extends TestLogHandlerSetupTask {

        @Override
        public Collection<String> getCategories() {
            return Set.of("org.jboss.as");
        }

        @Override
        public String getLevel() {
            return "INFO";
        }

        @Override
        public String getHandlerName() {
            return "startup";
        }

        @Override
        public String getLogFileName() {
            return "startup.log";
        }
    }
}