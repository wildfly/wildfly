/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jgroups.util.UUID;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Emanuel Muckenhuber
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DeploymentScannerUnitTestCase {

    public static final String CONTAINER = "default-jbossas";

    private static final PathAddress DEPLOYMENT_ONE = PathAddress.pathAddress(DEPLOYMENT, "deployment-one.jar");
    private static final PathAddress DEPLOYMENT_TWO = PathAddress.pathAddress(DEPLOYMENT, "deployment-two.jar");

    @ArquillianResource
    private static ContainerController container;

    private ModelControllerClient client;

    private static final String tempDir = System.getProperty("java.io.tmpdir");
    private static File deployDir;

    @Before
    public void before() throws IOException {
        deployDir = new File(tempDir + File.separator + "deployment-test-" + UUID.randomUUID().toString());
        if (deployDir.exists()) {
            FileUtils.deleteDirectory(deployDir);
        }
        assertTrue("Unable to create deployment scanner directory.", deployDir.mkdir());
    }

    @After
    public void after() throws IOException {
        FileUtils.deleteDirectory(deployDir);
    }

    @Test
    public void testStartup() throws Exception {

        container.start(CONTAINER);
        try {
            client = TestSuiteEnvironment.getModelControllerClient();
            try {

                final File deploymentOne = new File(deployDir, "deployment-one.jar");
                final File deploymentTwo = new File(deployDir, "deployment-two.jar");

                createDeployment(deploymentOne, "org.jboss.modules");
                createDeployment(deploymentTwo, "non.existing.dependency");

                // Add a new de
                addDeploymentScanner();
                try {
                    // Wait until deployed ...
                    while (!exists(DEPLOYMENT_ONE)) {
                        Thread.sleep(200);
                    }
                    Assert.assertTrue(exists(DEPLOYMENT_ONE));
                    Assert.assertEquals("OK", deploymentState(DEPLOYMENT_ONE));
                    Assert.assertTrue(exists(DEPLOYMENT_TWO));
                    Assert.assertEquals("FAILED", deploymentState(DEPLOYMENT_TWO));

                    final File oneDeployed = new File(deployDir, "deployment-one.jar.deployed");
                    final File twoFailed = new File(deployDir, "deployment-two.jar.failed");

                    // Restart ...
                    container.stop(CONTAINER);
                    container.start(CONTAINER);

                    // Wait until started ...
                    while (!isRunning()) {
                        Thread.sleep(200);
                    }

                    Assert.assertTrue(oneDeployed.exists());
                    Assert.assertTrue(twoFailed.exists());

                    Assert.assertTrue(exists(DEPLOYMENT_ONE));
                    Assert.assertEquals("OK", deploymentState(DEPLOYMENT_ONE));

                } finally {
                    removeDeploymentScanner();
                }

            } finally {
                StreamUtils.safeClose(client);
            }
        } finally {
            container.stop(CONTAINER);
        }
    }

    private ModelNode addDeploymentScanner() throws Exception {
        // add deployment scanner
        final ModelNode op = getAddDeploymentScannerOp();
        final ModelNode result = executeOperation(op);
        assertEquals("Unexpected outcome of adding the test deployment scanner: " + op, ModelDescriptionConstants.SUCCESS, result.get("outcome").asString());
        return result;
    }

    private ModelNode removeDeploymentScanner() throws Exception {
        // remove deployment scanner
        final ModelNode op = getRemoveDeploymentScannerOp();
        final ModelNode result = executeOperation(op);
        assertEquals("Unexpected outcome of removing the test deployment scanner: " + op, ModelDescriptionConstants.SUCCESS, result.get("outcome").asString());
        return result;
    }

    private ModelNode executeOperation(ModelNode op) throws IOException {
        return client.execute(op);
    }

    private ModelNode getAddDeploymentScannerOp() {
        final ModelNode op = Util.createAddOperation(getTestDeploymentScannerResourcePath());
        op.get("scan-interval").set(0);
        op.get("path").set(deployDir.getAbsolutePath());
        return op;
    }

    private ModelNode getRemoveDeploymentScannerOp() {
        return createOpNode("subsystem=deployment-scanner/scanner=testScanner", "remove");
    }

    private PathAddress getTestDeploymentScannerResourcePath() {
        return PathAddress.pathAddress(PathElement.pathElement("subsystem", "deployment-scanner"), PathElement.pathElement("scanner", "testScanner"));
    }

    protected void createDeployment(final File file, final String dependency) throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
        final String dependencies = "Dependencies: " + dependency;
        archive.add(new StringAsset(dependencies), "META-INF/MANIFEST.MF");
        archive.as(ZipExporter.class).exportTo(file);
    }

    protected boolean exists(PathAddress address) throws IOException {
        final ModelNode operation = Util.createEmptyOperation(READ_RESOURCE_OPERATION, address);
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(RECURSIVE).set(true);

        final ModelNode result = executeOperation(operation);
        if (SUCCESS.equals(result.get(OUTCOME).asString())) {
            return true;
        }
        return false;
    }

    protected String deploymentState(final PathAddress address) throws IOException {
        final ModelNode operation = Util.createEmptyOperation(READ_ATTRIBUTE_OPERATION, address);
        operation.get(NAME).set("status");

        final ModelNode result = executeOperation(operation);
        if (SUCCESS.equals(result.get(OUTCOME).asString())) {
            return result.get(RESULT).asString();
        }
        return "failed";
    }

    protected boolean isRunning() throws IOException {
        final ModelNode operation = Util.createEmptyOperation(READ_ATTRIBUTE_OPERATION, PathAddress.EMPTY_ADDRESS);
        operation.get(NAME).set("server-state");

        final ModelNode result = executeOperation(operation);
        if (SUCCESS.equals(result.get(OUTCOME).asString())) {
            return "running".equals(result.get(RESULT).asString());
        }
        return false;
    }

}
