/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.preview.vertx;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.webapp25.WebAppDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

/**
 * Smoke Test of vertx subsystem.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(VertxSubsystemSmokeTestCase.Setup.class)
public class VertxSubsystemSmokeTestCase {

    private static final String VERTX_EXTENSION = "org.wildfly.extension.vertx";
    private static final String VERTX_SUBSYSTEM = "vertx";

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive createDeployment() {
        Descriptors.create(WebAppDescriptor.class);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test-vertx-smoke.war");
        war.addClasses(EchoService.class, RestApp.class, ServiceEndpoint.class);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Test
    public void test() throws Exception {
        String res = HttpRequest.get(url.toExternalForm() + "rest/echo/Hello", 4, TimeUnit.SECONDS);
        Assert.assertEquals("Hello", res);
    }

    /**
     * Tests that it should fail to inject a Vertx instance with a not-existed qualifier provided by vertx subsystem.
     */
    @Test
    public void testFail() throws Exception {
        try {
            HttpRequest.get(url.toExternalForm() + "rest/echo/fail/Hello", 4, TimeUnit.SECONDS);
            Assert.fail("Should have failed");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("WELD-001334"));
        }
    }

    public static class Setup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) {
            if (!Boolean.getBoolean("ts.layers") && !Boolean.getBoolean("ts.bootable.preview")) {
                ModelNode op = Util.createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
                ModelNode steps = op.get(STEPS);
                steps.add(Util.createAddOperation(PathAddress.pathAddress(EXTENSION, VERTX_EXTENSION)));
                steps.add(Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM, VERTX_SUBSYSTEM)));
                executeManagementOperation(managementClient, op);
            }
            addVertxInstance(managementClient);
        }

        private void addVertxInstance(ManagementClient managementClient) {
            ModelNode addOp = Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM, VERTX_SUBSYSTEM).append("vertx", "vertx"));
            executeManagementOperation(managementClient, addOp);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            removeVertxInstance(managementClient);
            if (!Boolean.getBoolean("ts.layers")) {
                ModelNode op = Util.createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
                ModelNode steps = op.get(STEPS);
                steps.add(Util.createRemoveOperation(PathAddress.pathAddress(SUBSYSTEM, VERTX_EXTENSION)));
                steps.add(Util.createRemoveOperation(PathAddress.pathAddress(EXTENSION, VERTX_SUBSYSTEM)));
                executeManagementOperation(managementClient, op);
            }
        }

        private void removeVertxInstance(ManagementClient managementClient) {
            ModelNode addOp = Util.createRemoveOperation(PathAddress.pathAddress(SUBSYSTEM, VERTX_SUBSYSTEM).append("vertx", "vertx"));
            executeManagementOperation(managementClient, addOp);
        }

        private void executeManagementOperation(ManagementClient managementClient, ModelNode operation) {
            try {
                ModelNode response = managementClient.getControllerClient().execute(operation);
                if (!SUCCESS.equals(response.get(OUTCOME).asString())) {
                    throw new RuntimeException(String.format("%s response -- %s", operation, response));
                }
                ServerReload.executeReloadAndWaitForCompletion(managementClient);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
