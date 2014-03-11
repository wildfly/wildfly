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

package org.wildfly.test.integration.security.picketlink.federation;

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
import static org.wildfly.extension.picketlink.common.model.ModelElement.FEDERATION;
import static org.wildfly.extension.picketlink.federation.FederationExtension.SUBSYSTEM_NAME;

/**
 * @author Pedro Igor
 */
public abstract class AbstractFederationServerSetupTask implements ServerSetupTask {

    public static final String EXTENSION_MODULE_NAME = "org.wildfly.extension.picketlink";

    private final String alias;

    public AbstractFederationServerSetupTask(String alias) {
        this.alias = alias;
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        addExtensionAndSubsystem(managementClient);
        createFederation(managementClient);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        ModelControllerClient controllerClient = managementClient.getControllerClient();

        removeFederation(controllerClient);
        applyUpdate(Util.createRemoveOperation(PathAddress.pathAddress().append(SUBSYSTEM, SUBSYSTEM_NAME)), controllerClient);
        applyUpdate(Util.createRemoveOperation(PathAddress.pathAddress().append(EXTENSION, EXTENSION_MODULE_NAME)), controllerClient);
    }

    public void createFederation(ManagementClient managementClient) throws Exception {
        final ModelNode compositeOp = new ModelNode();

        compositeOp.get(OP).set(COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();

        ModelNode steps = compositeOp.get(STEPS);

        ModelNode federationOperation = createFederationAddOperation();

        steps.add(federationOperation);

        doCreateIdentityProvider(federationOperation, steps);
        doCreateServiceProviders(federationOperation, steps);

        applyUpdate(compositeOp, managementClient.getControllerClient());
    }

    protected abstract void doCreateServiceProviders(ModelNode federationOperation, ModelNode steps);

    public void removeFederation(ModelControllerClient controllerClient) throws Exception {
        applyUpdate(Util.createRemoveOperation(getFederationPathAddress()), controllerClient);
    }

    /**
     * <p>Subclasses should override this method to add all the necessary operations to add a identity provider configuration.</p>
     *
     * @param federationAddOperation
     * @param operationSteps
     */
    protected abstract void doCreateIdentityProvider(ModelNode federationAddOperation, ModelNode operationSteps);

    private ModelNode createFederationAddOperation() {
        ModelNode federationAddOperation = Util.createAddOperation(getFederationPathAddress());

        federationAddOperation.get(COMMON_NAME.getName()).set(this.alias);

        return federationAddOperation;
    }

    private PathAddress getFederationPathAddress() {
        return PathAddress.pathAddress().append(SUBSYSTEM, SUBSYSTEM_NAME).append(FEDERATION.getName(), this.alias);
    }

    private void addExtensionAndSubsystem(ManagementClient managementClient) throws Exception {
        ModelNode operationAddExtension = Util.createAddOperation(PathAddress.pathAddress()
            .append(EXTENSION, EXTENSION_MODULE_NAME));

        applyUpdate(operationAddExtension, managementClient.getControllerClient());

        ModelNode operationAddSubsystem = Util.createAddOperation(PathAddress.pathAddress().append(SUBSYSTEM, SUBSYSTEM_NAME));

        applyUpdate(operationAddSubsystem, managementClient.getControllerClient());
    }

}
