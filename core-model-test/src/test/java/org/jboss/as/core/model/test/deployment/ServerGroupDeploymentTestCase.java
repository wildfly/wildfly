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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.ModelWriteSanitizer;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.domain.controller.operations.deployment.DeploymentAddHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentRemoveHandler;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.server.deployment.DeploymentDeployHandler;
import org.jboss.as.server.deployment.DeploymentRedeployHandler;
import org.jboss.as.server.deployment.DeploymentUndeployHandler;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServerGroupDeploymentTestCase extends AbstractCoreModelTest {
    @Test
    public void testCantHaveTwoSameDeploymentsWithSameName() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode op = createOperation(kernelServices, "Test1");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        checkSingleDeployment(kernelServices, "Test1", false);

        op = createOperation(kernelServices, "Test1");
        kernelServices.executeForFailure(op);
    }

    @Test
    public void testCanHaveTwoDeploymentsWithDifferentNames() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode op = createOperation(kernelServices, "Test1");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));

        op = createOperation(kernelServices, "Test2");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));

        ModelNode deployments = getDeploymentParentResource(kernelServices);
        Assert.assertEquals(2, deployments.keys().size());

        Assert.assertEquals(false, deployments.get("Test1", ENABLED).asBoolean());
        Assert.assertEquals("Test1", deployments.get("Test1", NAME).asString());
        Assert.assertEquals("Test1", deployments.get("Test1", RUNTIME_NAME).asString());
        Assert.assertFalse(deployments.get("Test1", PERSISTENT).isDefined());
        Assert.assertFalse(deployments.get("Test1", SUBDEPLOYMENT).isDefined());

        Assert.assertEquals(false, deployments.get("Test2", ENABLED).asBoolean());
        Assert.assertEquals("Test2", deployments.get("Test2", NAME).asString());
        Assert.assertEquals("Test2", deployments.get("Test2", RUNTIME_NAME).asString());
        Assert.assertFalse(deployments.get("Test1", PERSISTENT).isDefined());
        Assert.assertFalse(deployments.get("Test2", SUBDEPLOYMENT).isDefined());
    }

    @Test
    public void testDeploymentWithDifferentEnabledRuntimeNameAndPersistentSettings() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode op = createOperation(kernelServices, "Test1", "ONE", false);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));

        op = createOperation(kernelServices, "Test2", "TWO", true);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));

        ModelNode deployments = getDeploymentParentResource(kernelServices);
        Assert.assertEquals(2, deployments.keys().size());

        Assert.assertEquals(false, deployments.get("Test1", ENABLED).asBoolean());
        Assert.assertEquals("Test1", deployments.get("Test1", NAME).asString());
        Assert.assertEquals("ONE", deployments.get("Test1", RUNTIME_NAME).asString());
        Assert.assertFalse(deployments.get("Test1", PERSISTENT).isDefined());
        Assert.assertFalse(deployments.get("Test1", SUBDEPLOYMENT).isDefined());

        Assert.assertEquals(true, deployments.get("Test2", ENABLED).asBoolean());
        Assert.assertEquals("Test2", deployments.get("Test2", NAME).asString());
        Assert.assertEquals("TWO", deployments.get("Test2", RUNTIME_NAME).asString());
        Assert.assertFalse(deployments.get("Test1", PERSISTENT).isDefined());
        Assert.assertFalse(deployments.get("Test2", SUBDEPLOYMENT).isDefined());
    }

    @Test
    public void testAddRemoveManagedDeployments() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode op = createOperation(kernelServices, "Test1");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op, new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5})));
        checkSingleDeployment(kernelServices, "Test1", false);
        removeDeployment(kernelServices, "Test1");
        checkNoDeployments(kernelServices);

        op = createOperation(kernelServices, "Test1");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        checkSingleDeployment(kernelServices, "Test1", false);
        removeDeployment(kernelServices, "Test1");
        checkNoDeployments(kernelServices);
    }

    @Test
    public void testDeployManagedDeployment() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode op = createOperation(kernelServices, "Test1");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        checkSingleDeployment(kernelServices, "Test1", false);

        op = Util.createOperation(DeploymentDeployHandler.OPERATION_NAME, getPathAddress("Test1"));
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));

        op = Util.createOperation(DeploymentUndeployHandler.OPERATION_NAME, getPathAddress("Test1"));
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));

        op = Util.createOperation(DeploymentDeployHandler.OPERATION_NAME, getPathAddress("Test1"));
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));

        op = Util.createOperation(DeploymentRemoveHandler.OPERATION_NAME, getPathAddress("Test1"));
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        checkNoDeployments(kernelServices);
    }

    @Test
    public void testRedeploy() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode op = createOperation(kernelServices, "Test1");
        op.get(ENABLED).set(true);
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op, new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5})));
        checkSingleDeployment(kernelServices, "Test1", true);

        op = Util.createOperation(DeploymentRedeployHandler.OPERATION_NAME, getPathAddress("Test1"));
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        checkSingleDeployment(kernelServices, "Test1", true);

        op = Util.createOperation(DeploymentRemoveHandler.OPERATION_NAME, getPathAddress("Test1"));
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op));
        checkNoDeployments(kernelServices);
    }

    @Test
    public void testCantWriteToAttributes() throws Exception {
        KernelServices kernelServices = createKernelServices();

        ModelNode op = createOperation(kernelServices, "Test1");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(op, new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5})));
        checkSingleDeployment(kernelServices, "Test1", false);

        op = createWriteAttributeOperation(kernelServices, "Test1", NAME, new ModelNode("Whatever"));
        kernelServices.executeForFailure(op);

        op = createWriteAttributeOperation(kernelServices, "Test1", RUNTIME_NAME, new ModelNode("Whatever"));
        kernelServices.executeForFailure(op);

        op = createWriteAttributeOperation(kernelServices, "Test1", ENABLED, new ModelNode(true));
        kernelServices.executeForFailure(op);
    }

    private ModelNode createOperation(KernelServices kernelServices, String name) throws Exception {
        return createOperation(kernelServices, name, null, null);
    }

    private ModelNode createOperation(KernelServices kernelServices, String name, String runtimeName, Boolean enabled) throws Exception {
        ModelNode operation = Util.createOperation(DeploymentAddHandler.OPERATION_NAME, getPathAddress(name));
        if (runtimeName != null) {
            operation.get(RUNTIME_NAME).set(runtimeName);
        }
        if (enabled != null) {
            operation.get(ENABLED).set(enabled);
        }
        //TODO Why is this needed?
        operation.get(CONTENT);

        //PERSISTENT is not exposed to users, it is deployment scanner only - so don't try to validate the operation with PERSISTENT set
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

    private void checkSingleDeployment(KernelServices kernelServices, String name, boolean deployed) throws Exception {
        ModelNode deployments = getDeploymentParentResource(kernelServices);
        Assert.assertEquals(1, deployments.keys().size());
        Assert.assertEquals(deployed, deployments.get(name, ENABLED).asBoolean());
        Assert.assertEquals(name, deployments.get(name, NAME).asString());
        Assert.assertFalse(deployments.get(name, PERSISTENT).isDefined());
        Assert.assertEquals(name, deployments.get(name, RUNTIME_NAME).asString());
        Assert.assertFalse(deployments.get(name, SUBDEPLOYMENT).isDefined());
        Assert.assertFalse(deployments.get(name, CONTENT).isDefined());
    }

    private ModelNode getByteContent(int...bytes) {
        ModelNode model = new ModelNode();
        model.get(BYTES).set(convertToByteArray(bytes));
        return model;
    }

    private byte[] convertToByteArray(int...bytes) {
        byte[] bytez = new byte[bytes.length];
        for (int i =0 ; i < bytes.length ; i++) {
            bytez[i] = (byte)bytes[i];
        }
        return bytez;
    }

    private KernelServices createKernelServices() throws Exception {
        ModelNode addDeployment1 = Util.createOperation(DeploymentAddHandler.OPERATION_NAME, PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, "Test1")));
        addDeployment1.get(CONTENT).add(getByteContent(1, 2, 3, 4, 5));

        ModelNode addDeployment2 = Util.createOperation(DeploymentAddHandler.OPERATION_NAME, PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, "Test2")));
        addDeployment2.get(CONTENT).add(getByteContent(1, 2, 3, 4, 5));

        List<ModelNode> deployments = new ArrayList<ModelNode>();
        deployments.add(addDeployment1);
        deployments.add(addDeployment2);

        KernelServices kernelServices =  createKernelServicesBuilder()
                .setBootOperations(deployments)
                .setModelInitializer(new ModelInitializer() {
                    @Override
                    public void populateModel(Resource rootResource) {
                        rootResource.registerChild(PathElement.pathElement(SERVER_GROUP, "Test"), Resource.Factory.create());
                    }
                }, new ModelWriteSanitizer() {

                    @Override
                    public ModelNode sanitize(ModelNode model) {
                        ModelNode node = model.clone();
                        node.remove(SERVER_GROUP);
                        return node;
                    }
                })
                .build();
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
        return PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "Test"), PathElement.pathElement(DEPLOYMENT, name));
    }

    protected ModelNode getDeploymentParentResource(KernelServices kernelServices) throws Exception {
        return kernelServices.readWholeModel().get(SERVER_GROUP, "Test", DEPLOYMENT);
    }
}
