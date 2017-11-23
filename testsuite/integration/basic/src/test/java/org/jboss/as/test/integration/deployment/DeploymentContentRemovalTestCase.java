/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.shared.ServerReload;
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
 * @author Tomas Hofman (thofman@redhat.com)
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DeploymentContentRemovalTestCase extends ContainerResourceMgmtTestBase {

    private static PathAddress DEPLOYMENT_ADDRESS = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, "deployment-one"));

    private static final String tempDir = System.getProperty("java.io.tmpdir");
    private File deployDir;

    @Before
    public void before() throws Exception {
        deployDir = new File(tempDir + File.separator + "tempDeployments");
        if (deployDir.exists()) {
            FileUtils.deleteDirectory(deployDir);
        }
        Assert.assertTrue("Unable to create deployment scanner directory.", deployDir.mkdir());
    }

    @After
    public void after() throws Exception {
        FileUtils.deleteDirectory(deployDir);
    }

    @Test
    public void testContentRemovedInNormalMode() throws IOException {
        testContentRemovedAfterUndeploying();
    }

    @Test
    public void testContentRemovedInAdminMode() throws IOException {
        ServerReload.executeReloadAndWaitForCompletion(getModelControllerClient(), true);
        testContentRemovedAfterUndeploying();
    }

    private void testContentRemovedAfterUndeploying() throws IOException {
        final ModelControllerClient client = getModelControllerClient();

        final Operation deployOp = deployOperation();
        ModelNode result = client.execute(deployOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        final ModelNode readResourceOp = readDeploymentOperation();
        result = client.execute(readResourceOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        String hash = extractHashString(result);
        assertDataExists(hash, true);

        final ModelNode undeployOp = undeployOperation();
        result = client.execute(undeployOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        assertDataExists(hash, false);
    }

    private void createDeployment(final File file) throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
        archive.add(new StringAsset(""), "META-INF/MANIFEST.MF");
        archive.as(ZipExporter.class).exportTo(file);
    }

    private Operation deployOperation() throws IOException {
        final File deploymentFile = new File(deployDir, "deployment.jar");
        createDeployment(deploymentFile);

        final ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).set(DEPLOYMENT_ADDRESS.toModelNode());
        op.get(ENABLED).set(true);
        op.get(CONTENT).add().get(INPUT_STREAM_INDEX).set(0);

        return OperationBuilder.create(op, true)
                .addFileAsAttachment(deploymentFile)
                .build();
    }

    private ModelNode undeployOperation() throws IOException {
        final ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).set(DEPLOYMENT_ADDRESS.toModelNode());
        return op;
    }

    private ModelNode readDeploymentOperation() {
        final ModelNode op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).set(DEPLOYMENT_ADDRESS.toModelNode());
        op.get("recursive").set(true);
        op.get(NAME).set("content");
        return op;
    }

    private String extractHashString(ModelNode result) {
        byte[] hash = result.get(RESULT).asList().get(0).get("hash").asBytes();
        Assert.assertEquals(20, hash.length);
        return HashUtil.bytesToHexString(hash);
    }

    private void assertDataExists(String hash, boolean exists) {
        Path dataRepository = Paths.get(System.getProperty("jboss.home"), "standalone/data/content");
        Path deploymentContent = dataRepository.resolve(hash.substring(0, 2)).resolve(hash.substring(2)).resolve("content");
        Assert.assertEquals(exists, deploymentContent.toFile().exists());
    }

}
