/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.core.model.test.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.domain.controller.operations.deployment.DeploymentAddHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentRemoveHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadBytesHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadStreamAttachmentHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadURLHandler;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.server.deployment.DeploymentDeployHandler;
import org.jboss.as.server.deployment.DeploymentFullReplaceHandler;
import org.jboss.as.server.deployment.DeploymentRedeployHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainDeploymentTestCase extends AbstractCoreModelTest {
    @Test
    public void testCantHaveTwoSameDeploymentsWithSameName() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode content = getByteContent(1, 2, 3, 4, 5);
        ModelNode op = createOperation(kernelServices, "Test1", content);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        checkSingleDeployment(kernelServices, "Test1");

        content = getByteContent(1, 2, 3, 4, 5);
        op = createOperation(kernelServices, "Test1", content);
        kernelServices.executeForFailure(op);
    }

    @Test
    public void testCanHaveTwoDeploymentsWithDifferentNames() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode content = getByteContent(1, 2, 3, 4, 5);
        ModelNode op = createOperation(kernelServices, "Test1", content);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));

        content = getByteContent(1, 2, 3, 4, 5);
        op = createOperation(kernelServices, "Test2", content);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));

        ModelNode deployments = getDeploymentParentResource(kernelServices);
        Assert.assertEquals(2, deployments.keys().size());

        Assert.assertFalse(deployments.get("Test1", ENABLED).isDefined());
        Assert.assertFalse(deployments.get("Test1", PERSISTENT).isDefined());
        Assert.assertEquals("Test1", deployments.get("Test1", NAME).asString());
        Assert.assertEquals("Test1", deployments.get("Test1", RUNTIME_NAME).asString());
        Assert.assertFalse(deployments.get("Test1", SUBDEPLOYMENT).isDefined());
        ModelNode bytes1 = getContentHashOnly(deployments.get("Test1"));

        Assert.assertFalse(deployments.get("Test2", ENABLED).isDefined());
        Assert.assertFalse(deployments.get("Test2", PERSISTENT).isDefined());
        Assert.assertEquals("Test2", deployments.get("Test2", NAME).asString());
        Assert.assertEquals("Test2", deployments.get("Test2", RUNTIME_NAME).asString());
        Assert.assertFalse(deployments.get("Test2", SUBDEPLOYMENT).isDefined());
        ModelNode bytes2 = getContentHashOnly(deployments.get("Test2"));

        Assert.assertEquals(bytes1, bytes2);
    }

    @Test
    public void testDeploymentWithDifferentEnabledRuntimeNameAndPersistentSettings() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode content = getByteContent(1, 2, 3, 4, 5);
        ModelNode op = createAddOperation(kernelServices, "Test1", "ONE", content);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));

        content = getByteContent(1, 2, 3, 4, 5);
        op = createAddOperation(kernelServices, "Test2", "TWO",  content);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));

        ModelNode deployments = getDeploymentParentResource(kernelServices);
        Assert.assertEquals(2, deployments.keys().size());

        Assert.assertFalse(deployments.get("Test1", ENABLED).isDefined());
        Assert.assertFalse(deployments.get("Test1", PERSISTENT).isDefined());
        Assert.assertEquals("Test1", deployments.get("Test1", NAME).asString());
        Assert.assertEquals("ONE", deployments.get("Test1", RUNTIME_NAME).asString());
        Assert.assertFalse(deployments.get("Test1", SUBDEPLOYMENT).isDefined());
        ModelNode bytes1 = getContentHashOnly(deployments.get("Test1"));

        Assert.assertFalse(deployments.get("Test2", ENABLED).isDefined());
        Assert.assertFalse(deployments.get("Test2", PERSISTENT).isDefined());
        Assert.assertEquals("Test2", deployments.get("Test2", NAME).asString());
        Assert.assertEquals("TWO", deployments.get("Test2", RUNTIME_NAME).asString());
        Assert.assertFalse(deployments.get("Test2", SUBDEPLOYMENT).isDefined());
        ModelNode bytes2 = getContentHashOnly(deployments.get("Test2"));

        Assert.assertEquals(bytes1, bytes2);
    }

    @Test
    public void testAddRemoveManagedDeploymentsWithDifferentContentTypes() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode content = getInputStreamIndexContent();
        ModelNode op = createOperation(kernelServices, "Test1", content);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op, new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5})));
        ModelNode hashA = checkSingleDeployment(kernelServices, "Test1");
        removeDeployment(kernelServices, "Test1");
        checkNoDeployments(kernelServices);

        content = getByteContent(1, 2, 3, 4, 5);
        op = createOperation(kernelServices, "Test1", content);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        ModelNode hashB = checkSingleDeployment(kernelServices, "Test1");
        removeDeployment(kernelServices, "Test1");
        checkNoDeployments(kernelServices);

        Assert.assertEquals(hashA, hashB);

//        content = new ModelNode();
//        content.get(HASH).set(hashA);
//        op = createOperation(kernelServices, "Test1", content);
//        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
//        //This deployment does not actually exist, it was removed by the previous removeDeployment()
//        //so if that becomes a problem, clean this part of the test up
//        ModelNode hashC = checkSingleDeployment(kernelServices, "Test1", content);
//        removeDeployment(kernelServices, "Test1");
//        checkNoDeployments(kernelServices);
//
//        Assert.assertEquals(hashA, hashC);

        content = getFileUrl("Test1", 1, 2, 3, 4, 5);
        op = createOperation(kernelServices, "Test1", content);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        ModelNode hashD = checkSingleDeployment(kernelServices, "Test1");
        removeDeployment(kernelServices, "Test1");
        checkNoDeployments(kernelServices);

        Assert.assertEquals(hashA, hashD);
    }

    @Test
    public void testDeployManagedDeployment() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode content = getByteContent(1, 2, 3, 4, 5);
        ModelNode op = createOperation(kernelServices, "Test1", content);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        ModelNode hash = checkSingleDeployment(kernelServices, "Test1");

        op = Util.createOperation(DeploymentDeployHandler.OPERATION_NAME, getPathAddress("Test1"));
        kernelServices.executeForFailure(op);
    }

    @Test
    public void testUnmanagedDeploymentAbsolutePath() throws Exception {
        KernelServices kernelServices = createKernelServices();

        File file = writeToFile("test-file1", 1, 2, 3, 4, 5);
        ModelNode content = new ModelNode();
        content.get(PATH).set(file.getAbsolutePath());
        ModelNode op = createOperation(kernelServices, "Test1", content);
        kernelServices.executeForFailure(op);

        content.get(ARCHIVE).set(true);
        op = createOperation(kernelServices, "Test1", content);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        ModelNode deployedContent = checkSingleUnmanagedDeployment(kernelServices, "Test1");
        checkUnmanagedContents(file, deployedContent, true, true);

        op = Util.createOperation(DeploymentDeployHandler.OPERATION_NAME, getPathAddress("Test1"));
        kernelServices.executeForFailure(op);

        op = Util.createOperation(DeploymentRemoveHandler.OPERATION_NAME, getPathAddress("Test1"));
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        checkNoDeployments(kernelServices);
    }

    @Test
    public void testUnmanagedDeploymentRelativePath() throws Exception {
        KernelServices kernelServices = createKernelServices();

        File file = writeToFile("test-file1", 1, 2, 3, 4, 5);
        File dir = file.getParentFile();
        ModelNode content = new ModelNode();
        content.get(PATH).set(file.getName());
        content.get(RELATIVE_TO).set(dir.getAbsolutePath());
        ModelNode op = createOperation(kernelServices, "Test1", content);
        kernelServices.executeForFailure(op);

        content.get(ARCHIVE).set(false);
        op = createOperation(kernelServices, "Test1", content);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        ModelNode deployedContent = checkSingleUnmanagedDeployment(kernelServices, "Test1");
        checkUnmanagedContents(file, deployedContent, false, false);

        op = Util.createOperation(DeploymentDeployHandler.OPERATION_NAME, getPathAddress("Test1"));
        kernelServices.executeForFailure(op);

        op = Util.createOperation(DeploymentRemoveHandler.OPERATION_NAME, getPathAddress("Test1"));
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        checkNoDeployments(kernelServices);
    }

    @Test
    public void testRedeploy() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode content = getInputStreamIndexContent();
        ModelNode op = createOperation(kernelServices, "Test1", content);
        op.get(ENABLED).set(true);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op, new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5})));
        checkSingleDeployment(kernelServices, "Test1");

        op = Util.createOperation(DeploymentRedeployHandler.OPERATION_NAME, getPathAddress("Test1"));
        kernelServices.executeForFailure(op);

        op = Util.createOperation(DeploymentRemoveHandler.OPERATION_NAME, getPathAddress("Test1"));
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        checkNoDeployments(kernelServices);
    }

    @Test
    public void testBadContentType() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode badContent = new ModelNode();
        badContent.get(BYTES).set(1);
        ModelNode op = createAddOperation(kernelServices, "Test1", null, badContent);
        kernelServices.executeForFailure(op);

        badContent = new ModelNode();
        badContent.get(URL).set(convertToByteArray(1, 2, 3, 4, 5));
        op = createAddOperation(kernelServices, "Test1", null, badContent);
        kernelServices.executeForFailure(op);

        badContent = new ModelNode();
        badContent.get(BYTES).set(getByteContent(1, 2, 3, 4, 5).get(BYTES));
        badContent.get(URL).set(getFileUrl("Test1", 1, 2, 3, 4, 5).get(URL));
        op = createAddOperation(kernelServices, "Test1", null, badContent);
        kernelServices.executeForFailure(op);

        badContent = new ModelNode();
        badContent.get(PATH).set(writeToFile(1, 2, 3, 4, 5).getAbsolutePath());
        op = createAddOperation(kernelServices, "Test1", null, badContent);
        kernelServices.executeForFailure(op);

        badContent = new ModelNode();
        badContent.get(URL).set(getFileUrl("Test1", 1, 2, 3, 4, 5).get(URL));
        badContent.get(PATH).set(writeToFile(1, 2, 3, 4, 5).getAbsolutePath());
        op = createAddOperation(kernelServices, "Test1", null, badContent);
        kernelServices.executeForFailure(op);
    }

    @Test
    public void testCantWriteToAttributes() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode content = getByteContent(1, 2, 3, 4, 5);
        ModelNode op = createOperation(kernelServices, "Test1", content);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op, new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5})));
        checkSingleDeployment(kernelServices, "Test1");

        op = createWriteAttributeOperation(kernelServices, "Test1", NAME, new ModelNode("Whatever"));
        kernelServices.executeForFailure(op);

        op = createWriteAttributeOperation(kernelServices, "Test1", RUNTIME_NAME, new ModelNode("Whatever"));
        kernelServices.executeForFailure(op);

        op = createWriteAttributeOperation(kernelServices, "Test1", ENABLED, new ModelNode(true));
        kernelServices.executeForFailure(op);

        op = createWriteAttributeOperation(kernelServices, "Test1", PERSISTENT, new ModelNode(false));
        kernelServices.executeForFailure(op);

        op = createWriteAttributeOperation(kernelServices, "Test1", CONTENT, createList(getByteContent(1, 2, 3)));
        kernelServices.executeForFailure(op);
    }

    @Test
    public void testRootContentHandlers() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode operation = Util.createOperation(DeploymentUploadBytesHandler.OPERATION_NAME, PathAddress.EMPTY_ADDRESS);
        operation.get(BYTES).set(convertToByteArray(new int[] {1, 2, 3, 4, 5}));
        ModelNode hashBytes = kernelServices.executeForResult(operation);
        checkNoDeployments(kernelServices);

        operation = Util.createOperation(DeploymentUploadURLHandler.OPERATION_NAME, PathAddress.EMPTY_ADDRESS);
        File file = writeToFile("test-file1", new int[] {1, 2, 3, 4, 5});
        operation.get(URL).set(file.toURI().toURL().toString());
        ModelNode hashUrl = kernelServices.executeForResult(operation);
        checkNoDeployments(kernelServices);
        Assert.assertEquals(hashBytes, hashUrl);

        operation = Util.createOperation(DeploymentUploadStreamAttachmentHandler.OPERATION_NAME, PathAddress.EMPTY_ADDRESS);
        operation.get(INPUT_STREAM_INDEX).set(0);
        ModelNode hashStream = kernelServices.executeForResult(operation, new ByteArrayInputStream(convertToByteArray(1, 2, 3, 4, 5)));
        checkNoDeployments(kernelServices);
        Assert.assertEquals(hashBytes, hashStream);
    }


    @Test
    public void testDeploymentFullReplaceHandlerNoDeployment() throws Exception {
        KernelServices kernelServices = createKernelServices();

        //Now start replacing it
        ModelNode op = Util.createOperation(DeploymentFullReplaceHandler.OPERATION_NAME, PathAddress.EMPTY_ADDRESS);
        op.get(NAME).set("Test1");
        op.get(CONTENT).add(getByteContent(6, 7, 8, 9, 10));
        kernelServices.executeForFailure(op);
    }

    @Test
    public void testDeploymentFullReplaceHandlerManaged() throws Exception {
        KernelServices kernelServices = createKernelServices();

        //Create the original deployment
        ModelNode content = getByteContent(1, 2, 3, 4, 5);
        ModelNode op = createOperation(kernelServices, "Test1", content);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        ModelNode originalHash = checkSingleDeployment(kernelServices, "Test1");

        //Now start replacing it
        op = Util.createOperation(DeploymentFullReplaceHandler.OPERATION_NAME, PathAddress.EMPTY_ADDRESS);
        op.get(NAME).set("Test1");
        op.get(CONTENT).add(getByteContent(6, 7, 8, 9, 10));
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        ModelNode newHash = checkSingleDeployment(kernelServices, "Test1");
        Assert.assertFalse(originalHash.equals(newHash));

        op = op.clone();
        op.get(CONTENT).clear();
        ModelNode hashContent = new ModelNode();
        hashContent.get(HASH).set(newHash);
        op.get(CONTENT).add(hashContent);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        Assert.assertEquals(newHash, checkSingleDeployment(kernelServices, "Test1"));

        op = op.clone();
        op.get(CONTENT).clear();
        op.get(CONTENT).add(getFileUrl("Test1", 1, 2, 3, 4, 5));
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        Assert.assertEquals(originalHash, checkSingleDeployment(kernelServices, "Test1"));

        //Replace again with a runtime name
        op = op.clone();
        op.get(CONTENT).clear();
        op.get(CONTENT).add(getInputStreamIndexContent());
        op.get(RUNTIME_NAME).set("number1");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op, new ByteArrayInputStream(new byte[] {6, 7, 8, 9, 10})));
        Assert.assertEquals(newHash, checkSingleDeployment(kernelServices, "Test1", "number1"));
    }

    @Test
    public void testDeploymentFullReplaceHandlerUnmanaged() throws Exception {
        KernelServices kernelServices = createKernelServices();

        File file1 = writeToFile("Testfile1", 1, 2, 3, 4, 5);
        File file2 = writeToFile("Testfile2", 6, 7, 8, 9, 10);

        //Create the original deployment
        ModelNode contentNode = new ModelNode();
        contentNode.get(PATH).set(file1.getAbsolutePath());
        ModelNode op = createOperation(kernelServices, "Test1", contentNode);
        kernelServices.executeForFailure(op);

        contentNode.get(ARCHIVE).set(true);
        op.get(CONTENT).clear();
        op.get(CONTENT).add(contentNode);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        ModelNode originalContent = checkSingleUnmanagedDeployment(kernelServices, "Test1");
        checkUnmanagedContents(file1, originalContent, true, true);

        op = Util.createOperation(DeploymentFullReplaceHandler.OPERATION_NAME, PathAddress.EMPTY_ADDRESS);
        op.get(NAME).set("Test1");
        contentNode = new ModelNode();
        contentNode.get(PATH).set(file2.getAbsolutePath());
        contentNode.get(ARCHIVE).set(false);
        kernelServices.executeForFailure(op);
        op.get(CONTENT).add(contentNode);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        ModelNode currentContent = checkSingleUnmanagedDeployment(kernelServices, "Test1");
        checkUnmanagedContents(file2, currentContent, false, true);

        op = op.clone();
        op.get(CONTENT).clear();
        contentNode = new ModelNode();
        contentNode.get(RELATIVE_TO).set(file1.getParentFile().getAbsolutePath());
        contentNode.get(ARCHIVE).set(true);
        op.get(CONTENT).add(contentNode);
        kernelServices.executeForFailure(op);

        contentNode.get(PATH).set(file1.getName());
        op.get(CONTENT).clear();
        op.get(CONTENT).add(contentNode);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        currentContent = checkSingleUnmanagedDeployment(kernelServices, "Test1");
        checkUnmanagedContents(file1, currentContent, true, false);
    }

    private ModelNode createList(ModelNode element) {
        ModelNode list = new ModelNode();
        list.add(element);
        return list;
    }



    private void checkUnmanagedContents(File file, ModelNode deployedContent, boolean archive, boolean absolute) {
        Assert.assertEquals(absolute ? 2 : 3, deployedContent.keys().size());
        if (absolute) {
            Assert.assertEquals(file.getAbsolutePath(), deployedContent.get(PATH).asString());
        } else {
            Assert.assertEquals(file.getName(), deployedContent.get(PATH).asString());
            Assert.assertEquals(file.getParentFile().getAbsolutePath(), deployedContent.get(RELATIVE_TO).asString());
        }
        Assert.assertEquals(archive, deployedContent.get(ARCHIVE).asBoolean());
    }


    private ModelNode createOperation(KernelServices kernelServices, String name, ModelNode content) throws Exception {
        return createAddOperation(kernelServices, name, null, content);
    }

    private ModelNode createAddOperation(KernelServices kernelServices, String name, String runtimeName, ModelNode content) throws Exception {
        ModelNode operation = Util.createOperation(DeploymentAddHandler.OPERATION_NAME, getPathAddress(name));
        if (runtimeName != null) {
            operation.get(RUNTIME_NAME).set(runtimeName);
        }
        operation.get(CONTENT).add(content);

        kernelServices.validateOperation(operation);

        return operation;
    }

    private ModelNode createWriteAttributeOperation(KernelServices kernelServices, String name, String attrName, ModelNode attrValue) {
        ModelNode operation = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, getPathAddress(name));
        operation.get(NAME).set(attrName);
        operation.get(VALUE).set(attrValue);

        kernelServices.validateOperation(operation);

        return operation;
    }

    private ModelNode checkSingleDeployment(KernelServices kernelServices, String name) throws Exception {
        return checkSingleDeployment(kernelServices, name, name);
    }

    private ModelNode checkSingleDeployment(KernelServices kernelServices, String name, String runtimeName) throws Exception {
        ModelNode deployments = getDeploymentParentResource(kernelServices);
        Assert.assertEquals(1, deployments.keys().size());
        Assert.assertFalse(deployments.get(name, ENABLED).isDefined());
        Assert.assertFalse(deployments.get(name, PERSISTENT).isDefined());
        Assert.assertEquals(name, deployments.get(name, NAME).asString());
        Assert.assertEquals(runtimeName, deployments.get(name, RUNTIME_NAME).asString());
        Assert.assertFalse(deployments.get(name, SUBDEPLOYMENT).isDefined());
        return getContentHashOnly(deployments.get(name));
    }

    private ModelNode checkSingleUnmanagedDeployment(KernelServices kernelServices, String name) throws Exception {
        ModelNode deployments = getDeploymentParentResource(kernelServices);
        Assert.assertEquals(1, deployments.keys().size());
        Assert.assertFalse(deployments.get(name, ENABLED).isDefined());
        Assert.assertFalse(deployments.get(name, PERSISTENT).isDefined());
        Assert.assertEquals(name, deployments.get(name, NAME).asString());
        Assert.assertEquals(name, deployments.get(name, RUNTIME_NAME).asString());
        Assert.assertFalse(deployments.get(name, SUBDEPLOYMENT).isDefined());
        return getContentOnly(deployments.get(name));
    }



    private ModelNode getInputStreamIndexContent() {
        ModelNode model = new ModelNode();
        model.get(INPUT_STREAM_INDEX).set(0);
        return model;
    }

    private ModelNode getByteContent(int...bytes) {
        ModelNode model = new ModelNode();
        model.get(BYTES).set(convertToByteArray(bytes));
        return model;
    }

    private ModelNode getFileUrl(String name, int...bytes) throws Exception {
        File f = writeToFile(name, bytes);
        ModelNode model = new ModelNode();
        model.get(URL).set(f.toURI().toURL().toString());
        return model;
    }

    private File writeToFile(String name, int...bytes) throws IOException {
        File file = new File("target/test-file");
        file.delete();
        FileOutputStream fout = new FileOutputStream(file);
        try {
            for (byte b : convertToByteArray(bytes)) {
                fout.write(b);
            }
        } finally {
            IoUtils.safeClose(fout);
        }

        return file;
    }

    private byte[] convertToByteArray(int...bytes) {
        byte[] bytez = new byte[bytes.length];
        for (int i =0 ; i < bytes.length ; i++) {
            bytez[i] = (byte)bytes[i];
        }
        return bytez;
    }

    private ModelNode getContentHashOnly(ModelNode deployment) {
        ModelNode contentEntry = getContentOnly(deployment);
        Assert.assertEquals(1, contentEntry.keys().size());
        ModelNode hash = contentEntry.get(HASH);
        Assert.assertTrue(hash.isDefined());
        Assert.assertEquals(ModelType.BYTES, hash.getType());
        return hash;
    }

    private ModelNode getContentOnly(ModelNode deployment) {
        ModelNode content = deployment.get(CONTENT);
        Assert.assertTrue(content.isDefined());
        Assert.assertEquals(ModelType.LIST, content.getType());
        Assert.assertEquals(1, content.asList().size());

        ModelNode contentEntry = content.asList().get(0);
        Assert.assertTrue(contentEntry.isDefined());
        return contentEntry;
    }

    private KernelServices createKernelServices() throws Exception {
        KernelServices kernelServices =  createKernelServicesBuilder().build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());
        return kernelServices;
    }

    private void removeDeployment(KernelServices kernelServices, String name) {
        ModelNode remove = Util.createOperation(DeploymentRemoveHandler.OPERATION_NAME, getPathAddress(name));
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(remove));
    }

    private void checkNoDeployments(KernelServices kernelServices) throws Exception {
        Assert.assertFalse(getDeploymentParentResource(kernelServices).isDefined());
    }

    protected KernelServicesBuilder createKernelServicesBuilder() {
        return createKernelServicesBuilder(TestModelType.DOMAIN);
    }

    protected PathAddress getPathAddress(String name) {
        return PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, name));
    }

    protected ModelNode getDeploymentParentResource(KernelServices kernelServices) throws Exception {
        return kernelServices.readWholeModel().get(DEPLOYMENT);
    }
}
