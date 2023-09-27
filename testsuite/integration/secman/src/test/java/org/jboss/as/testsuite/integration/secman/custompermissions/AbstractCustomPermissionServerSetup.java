/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.testsuite.integration.secman.custompermissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

public abstract class AbstractCustomPermissionServerSetup implements ServerSetupTask {

    private TestModule module;
    private List<ModelNode> backupList = new ArrayList<>();

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        this.module = Utils.createTestModule("moduleperm.jar", "org.jboss.test", GrantCustomPermissionModuleMinimumPermissionTestCase.class
                .getResource("module.xml").getFile(), CustomPermission.class);

        if (writeMinimumPermissions()) {
            backupMinimumPermissions(managementClient);

            final ModelNode address = new ModelNode();
            address.add("subsystem", "security-manager");
            address.add("deployment-permissions", "default");
            address.protect();

            final ModelNode operation = new ModelNode();
            operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            operation.get(ModelDescriptionConstants.OP_ADDR).set(address);

            operation.get("name").set("minimum-permissions");
            ModelNode customPermission = new ModelNode();
            customPermission.get("class").set(new ModelNode(CustomPermission.class.getName()));
            customPermission.get("name").set(new ModelNode("org.jboss.test"));
            customPermission.get("module").set(new ModelNode("org.jboss.test"));

            operation.get("value").set(Arrays.asList(customPermission));

            ManagementOperations.executeOperation(managementClient.getControllerClient(), operation);
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }

    public void backupMinimumPermissions(ManagementClient managementClient) throws Exception {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "security-manager");
        address.add("deployment-permissions", "default");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        operation.get("name").set("minimum-permissions");

        ModelNode result = ManagementOperations.executeOperation(managementClient.getControllerClient(), operation);
        if(result.isDefined()){
            List<ModelNode> list = result.asList();
            for (ModelNode modelNode : list) {
                ModelNode customPermission = new ModelNode();
                customPermission.get("class").set(modelNode.get("class"));
                customPermission.get("name").set(modelNode.get("name"));
                customPermission.get("module").set(modelNode.get("module"));
                backupList.add(customPermission);
            }
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        this.module.remove();

        //restore minimum permissions
        if (writeMinimumPermissions()) {
            final ModelNode address = new ModelNode();
            address.add("subsystem", "security-manager");
            address.add("deployment-permissions", "default");
            address.protect();

            final ModelNode operation = new ModelNode();
            operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
            operation.get("name").set("minimum-permissions");
            operation.get("value").set(backupList);

            ManagementOperations.executeOperation(managementClient.getControllerClient(), operation);
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }

    protected boolean writeMinimumPermissions() {
        return false;
    };
}
