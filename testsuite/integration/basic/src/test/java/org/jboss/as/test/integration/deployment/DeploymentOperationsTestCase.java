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

package org.jboss.as.test.integration.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.protocol.StreamUtils.safeClose;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
public class DeploymentOperationsTestCase extends ContainerResourceMgmtTestBase {

    private final PathAddress DEPLOYMENT_ONE = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, "deployment-one"));
    private final PathAddress DEPLOYMENT_TWO = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, "deployment-two"));

    private static final String tempDir = System.getProperty("java.io.tmpdir");
    private File deployDir;

    @Before
    public void before() throws Exception {
        deployDir = new File(tempDir + File.separator + "tempDeployments");
        if (deployDir.exists()) {
            FileUtils.deleteDirectory(deployDir);
        }
        assertTrue("Unable to create deployment scanner directory.", deployDir.mkdir());
    }

    @After
    public void after() throws Exception {
        FileUtils.deleteDirectory(deployDir);
    }

    @Test
    public void testDeploymentRollbackOnRuntimeFailure() throws Exception {
        final File deploymentOne = new File(deployDir, "deployment-one.jar");
        final File deploymentTwo = new File(deployDir, "deployment-two.jar");

        createDeployment(deploymentOne, "org.jboss.modules");
        createDeployment(deploymentTwo, "non.existing.dependency");

        final ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(OPERATION_HEADERS).get(ROLLBACK_ON_RUNTIME_FAILURE).set(false);

        final ModelNode nested = composite.get(STEPS).setEmptyList().add();
        nested.get(OP).set(COMPOSITE);
        nested.get(OP_ADDR).setEmptyList();

        final ModelNode steps = nested.get(STEPS).setEmptyList();

        final ModelNode deployOne = steps.add();
        deployOne.get(OP).set(ADD);
        deployOne.get(OP_ADDR).set(DEPLOYMENT_ONE.toModelNode());
        deployOne.get(ENABLED).set(true);
        deployOne.get(CONTENT).add().get(INPUT_STREAM_INDEX).set(0);

        final ModelNode deployTwo = steps.add();
        deployTwo.get(OP).set(ADD);
        deployTwo.get(OP_ADDR).set(DEPLOYMENT_TWO.toModelNode());
        deployTwo.get(ENABLED).set(true);
        deployTwo.get(CONTENT).add().get(INPUT_STREAM_INDEX).set(1);

        final Operation operation = OperationBuilder.create(composite, true)
                .addFileAsAttachment(deploymentOne)
                .addFileAsAttachment(deploymentTwo)
                .build();

        final ModelControllerClient client = getModelControllerClient();
        try {
            // Deploy
            final ModelNode overallResult = client.execute(operation);
            Assert.assertTrue(overallResult.asString(), SUCCESS.equals(overallResult.get(OUTCOME).asString()));

            final ModelNode result = overallResult.get(RESULT, "step-1");
            Assert.assertTrue(result.asString(), SUCCESS.equals(result.get(OUTCOME).asString()));

            final ModelNode step1 = result.get(RESULT, "step-1");
            Assert.assertEquals(SUCCESS, step1.get(OUTCOME).asString());

            final ModelNode step2 = result.get(RESULT, "step-2");
            Assert.assertEquals(FAILED, step2.get(OUTCOME).asString());

        } finally {
            safeClose(operation);
        }

        // Check if deployment-one and -two exist
        executeOperation(Util.createEmptyOperation(READ_RESOURCE_OPERATION, DEPLOYMENT_ONE));
        executeOperation(Util.createEmptyOperation(READ_RESOURCE_OPERATION, DEPLOYMENT_TWO));

        //do cleanup
        executeOperation(Util.createRemoveOperation(DEPLOYMENT_ONE));
        executeOperation(Util.createRemoveOperation(DEPLOYMENT_TWO));
    }

    protected void createDeployment(final File file, final String dependency) throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
        final String dependencies = "Dependencies: " + dependency;
        archive.add(new StringAsset(dependencies), "META-INF/MANIFEST.MF");
        archive.as(ZipExporter.class).exportTo(file);
    }

}
