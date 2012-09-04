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
package org.jboss.as.model.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_ALIASES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ModelTestKernelServices {

    private volatile ServiceContainer container;
    private final ModelController controller;
    private final StringConfigurationPersister persister;
    private final OperationValidator operationValidator;
    private final ManagementResourceRegistration rootRegistration;
    private final Map<ModelVersion, ModelTestKernelServices> legacyServices;
    private final boolean successfulBoot;
    private final Throwable bootError;

    protected ModelTestKernelServices(ServiceContainer container, ModelController controller, StringConfigurationPersister persister, ManagementResourceRegistration rootRegistration,
            OperationValidator operationValidator, ModelVersion legacyModelVersion, boolean successfulBoot, Throwable bootError) {
        this.container = container;
        this.controller = controller;
        this.persister = persister;
        this.operationValidator = operationValidator;
        this.rootRegistration = rootRegistration;
        this.legacyServices = legacyModelVersion != null ? null : new HashMap<ModelVersion, ModelTestKernelServices>();
        this.successfulBoot = successfulBoot;
        this.bootError = bootError;
    }


    /**
     * Get whether the controller booted successfully
     * @return true if the controller booted successfully
     */
    public boolean isSuccessfulBoot() {
        return successfulBoot;
    }


    /**
     * Get any errors thrown on boot
     * @return the boot error
     */
    public Throwable getBootError() {
        return bootError;
    }


    /**
     * Gets the legacy controller services for the controller containing the passed in model version
     *
     * @param modelVersion the model version of the legacy model controller
     * @throws IllegalStateException if this is not the test's main model controller
     * @throws IllegalStateException if there is no legacy controller containing the version
     */
    public ModelTestKernelServices getLegacyServices(ModelVersion modelVersion) {
        checkIsMainController();
        ModelTestKernelServices legacy = legacyServices.get(modelVersion);
        if (legacy == null) {
            throw new IllegalStateException("No legacy subsystem controller was found for model version " + modelVersion);
        }
        return legacy;
    }

    protected void checkIsMainController() {
        if (legacyServices == null) {
            throw new IllegalStateException("Can only be called for the main controller");
        }
    }

    /**
     * Reads the whole model from the model controller without aliases or runtime attributes/resources
     *
     * @return the whole model
     */
    public ModelNode readWholeModel() {
        return readWholeModel(false);
    }

    /**
     * Reads the whole model from the model controller without runtime attributes/resources
     *
     * @param includeAliases whether to include aliases
     * @return the whole model
     */
    public ModelNode readWholeModel(boolean includeAliases) {
        return readWholeModel(includeAliases, false);
    }

    /**
     * Reads the whole model from the model controller
     *
     * @param includeAliases whether to include aliases
     * @param includeRuntime whether to include runtime attributes/resources
     * @return the whole model
     */
    public ModelNode readWholeModel(boolean includeAliases, boolean includeRuntime) {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        op.get(RECURSIVE).set(true);
        if (includeRuntime) {
            op.get(INCLUDE_RUNTIME).set(true);
        }
        if (includeAliases) {
            op.get(INCLUDE_ALIASES).set(true);
        }
        ModelNode result = executeOperation(op);
        return ModelTestUtils.checkResultAndGetContents(result);
    }

    /**
     * Gets the service container
     *
     * @return the service container
     */
    public ServiceContainer getContainer() {
        return container;
    }


    /**
     * Execute an operation in the model controller
     *
     * @param operation the operation to execute
     * @return the whole result of the operation
     */
    public ModelNode executeOperation(ModelNode operation) {
        return controller.execute(operation, null, OperationTransactionControl.COMMIT, null);
    }



    public ModelNode executeForResult(ModelNode operation) throws OperationFailedException {
        ModelNode rsp = executeOperation(operation);
        if (FAILED.equals(rsp.get(OUTCOME).asString())) {
            throw new OperationFailedException(rsp.get(FAILURE_DESCRIPTION));
        }
        return rsp.get(RESULT);
    }


    /**
     * Execute an operation in the model controller, expecting failure.
     * Gives a junit {@link AssertionFailedError} if the operation did not fail.
     *
     * @param operation the operation to execute
     * @return the result of the operation
     */
    public void executeForFailure(ModelNode operation) {
        try {
            executeForResult(operation);
            Assert.fail("Should have given error");
        } catch (OperationFailedException expected) {
        }
    }


    /**
     * Reads the persisted subsystem xml
     *
     * @return the xml
     */
    public String getPersistedSubsystemXml() {
        return persister.getMarshalled();
    }

    /**
     * Validates the operations against the description providers in the model controller
     *
     * @param operations the operations to validate
     * @throws AssertionFailedError if the operations are not valid
     */
    public void validateOperations(List<ModelNode> operations) {
        operationValidator.validateOperations(operations);
    }

    /**
     * Validates the operation against the description providers in the model controller
     *
     * @param operation the operation to validate
     * @throws AssertionFailedError if the operation is not valid
     */
    public void validateOperation(ModelNode operation) {
        operationValidator.validateOperation(operation);
    }

    public void shutdown() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            container = null;

            if (legacyServices != null) {
                for (ModelTestKernelServices legacyService: legacyServices.values()) {
                    legacyService.shutdown();
                }
            }
        }
    }

    public ImmutableManagementResourceRegistration getRootRegistration() {
        return rootRegistration;
    }

    protected void addLegacyKernelService(ModelVersion modelVersion, ModelTestKernelServices legacyServices) {
        this.legacyServices.put(modelVersion, legacyServices);
    }

}
