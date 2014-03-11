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

package org.wildfly.test.integration.security.picketlink.idm.util;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.integration.security.common.Utils.applyUpdate;
import static org.wildfly.extension.picketlink.common.model.ModelElement.COMMON_NAME;
import static org.wildfly.extension.picketlink.common.model.ModelElement.COMMON_SUPPORTS_ALL;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_CONFIGURATION;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_MANAGEMENT_JNDI_NAME;
import static org.wildfly.extension.picketlink.common.model.ModelElement.PARTITION_MANAGER;
import static org.wildfly.extension.picketlink.common.model.ModelElement.SUPPORTED_TYPES;
import static org.wildfly.extension.picketlink.idm.IDMExtension.SUBSYSTEM_NAME;

/**
 * @author Pedro Igor
 */
public abstract class AbstractIdentityManagementServerSetupTask implements ServerSetupTask {

    public static final String EXTENSION_MODULE_NAME = "org.wildfly.extension.picketlink";

    private final String alias;
    private final String jndiName;

    public AbstractIdentityManagementServerSetupTask(String alias, String jndiName) {
        this.alias = alias;
        this.jndiName = jndiName;
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        addExtensionAndSubsystem(managementClient);
        createIdentityManagementConfiguration(managementClient);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        ModelControllerClient controllerClient = managementClient.getControllerClient();

        removeIdentityManagementConfiguration(controllerClient);
        applyUpdate(Util.createRemoveOperation(PathAddress.pathAddress().append(SUBSYSTEM, SUBSYSTEM_NAME)), controllerClient);
        applyUpdate(Util.createRemoveOperation(PathAddress.pathAddress().append(EXTENSION, EXTENSION_MODULE_NAME)), controllerClient);
    }

    public void createIdentityManagementConfiguration(ManagementClient managementClient) throws Exception {
        final ModelNode compositeOp = new ModelNode();

        compositeOp.get(OP).set(COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();

        ModelNode steps = compositeOp.get(STEPS);

        ModelNode identityManagementAddOperation = createIdentityManagementAddOperation();

        steps.add(identityManagementAddOperation);

        doCreateIdentityManagement(identityManagementAddOperation, steps);

        applyUpdate(compositeOp, managementClient.getControllerClient());
    }

    public void removeIdentityManagementConfiguration(ModelControllerClient controllerClient) throws Exception {
        applyUpdate(Util.createRemoveOperation(getIdentityManagementPathAddress()), controllerClient);
    }

    /**
     * <p>Subclasses should override this method to add all the necessary operations to add a identity management configuration.</p>
     *
     * @param identityManagementAddOperation
     * @param operationSteps
     */
    protected abstract void doCreateIdentityManagement(ModelNode identityManagementAddOperation, ModelNode operationSteps);

    protected PathAddress createIdentityConfigurationPathAddress(String name) {
        return getIdentityManagementPathAddress().append(IDENTITY_CONFIGURATION.getName(), name);
    }

    protected ModelNode createSupportedAllTypesAddOperation(ModelNode identityStoreModelNode) {
        ModelNode operationAddSupportedTypes = Util.createAddOperation(PathAddress.pathAddress(identityStoreModelNode.get(OP_ADDR)).append(SUPPORTED_TYPES
            .getName(), SUPPORTED_TYPES.getName()));

        operationAddSupportedTypes.get(COMMON_SUPPORTS_ALL.getName()).set(true);

        return operationAddSupportedTypes;
    }

    private ModelNode createIdentityManagementAddOperation() {
        ModelNode operationAddIdentityManagement = Util.createAddOperation(getIdentityManagementPathAddress());

        operationAddIdentityManagement.get(COMMON_NAME.getName()).set(this.alias);
        operationAddIdentityManagement.get(IDENTITY_MANAGEMENT_JNDI_NAME.getName()).set(this.jndiName);

        return operationAddIdentityManagement;
    }

    private PathAddress getIdentityManagementPathAddress() {
        return PathAddress.pathAddress().append(SUBSYSTEM, SUBSYSTEM_NAME).append(PARTITION_MANAGER.getName(), this.alias);
    }

    private void addExtensionAndSubsystem(ManagementClient managementClient) throws Exception {
        ModelNode operationAddExtension = Util.createAddOperation(PathAddress.pathAddress()
            .append(EXTENSION, EXTENSION_MODULE_NAME));

        applyUpdate(operationAddExtension, managementClient.getControllerClient());

        ModelNode operationAddSubsystem = Util.createAddOperation(PathAddress.pathAddress().append(SUBSYSTEM, SUBSYSTEM_NAME));

        applyUpdate(operationAddSubsystem, managementClient.getControllerClient());
    }

}
