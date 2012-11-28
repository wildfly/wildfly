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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class ModelTestKernelServicesImpl<T extends ModelTestKernelServices<T>> implements ModelTestKernelServices<T> {

    private volatile ServiceContainer container;
    private final ModelTestModelControllerService controllerService;
    private final ModelController controller;
    private final StringConfigurationPersister persister;
    private final OperationValidator operationValidator;
    private final ManagementResourceRegistration rootRegistration;
    private final Map<ModelVersion, T> legacyServices;
    private final boolean successfulBoot;
    private final Throwable bootError;

    protected ModelTestKernelServicesImpl(ServiceContainer container, ModelTestModelControllerService controllerService, StringConfigurationPersister persister, ManagementResourceRegistration rootRegistration,
            OperationValidator operationValidator, ModelVersion legacyModelVersion, boolean successfulBoot, Throwable bootError) {
        this.container = container;
        this.controllerService = controllerService;
        this.controller = controllerService.getValue();
        this.persister = persister;
        this.operationValidator = operationValidator;
        this.rootRegistration = rootRegistration;
        this.legacyServices = legacyModelVersion != null ? null : new HashMap<ModelVersion, T>();
        this.successfulBoot = successfulBoot;
        this.bootError = bootError;
    }


    /**
     * Get whether the controller booted successfully
     * @return true if the controller booted successfully
     */
    @Override
    public boolean isSuccessfulBoot() {
        return successfulBoot;
    }


    /**
     * Get any errors thrown on boot
     * @return the boot error
     */
    @Override
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
    @Override
    public T getLegacyServices(ModelVersion modelVersion) {
        checkIsMainController();
        T legacy = legacyServices.get(modelVersion);
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
    @Override
    public ModelNode readWholeModel() {
        return readWholeModel(false);
    }

    /**
     * Reads the whole model from the model controller without runtime attributes/resources
     *
     * @param includeAliases whether to include aliases
     * @return the whole model
     */
    @Override
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
    @Override
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
    @Override
    public ServiceContainer getContainer() {
        return container;
    }


    /**
     * Execute an operation in the model controller
     *
     * @param operation the operation to execute
     * @param inputStream Input Streams for the operation
     * @return the whole result of the operation
     */
    @Override
    public ModelNode executeOperation(ModelNode operation, InputStream...inputStreams) {
        if (inputStreams.length == 0) {
            return executeOperation(operation, OperationTransactionControl.COMMIT);
        } else {
            ExecutorService executor = Executors.newCachedThreadPool();
            try {
                ModelControllerClient client = controller.createClient(executor);
                OperationBuilder builder = OperationBuilder.create(operation);
                for (InputStream in : inputStreams) {
                    builder.addInputStream(in);
                }
                Operation op = builder.build();

                try {
                    return client.execute(op);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Override
    public ModelNode executeOperation(ModelNode operation, OperationTransactionControl txControl) {
        return controller.execute(operation, null, txControl, null);
    }

    @Override
    public ModelNode executeForResult(ModelNode operation, InputStream...inputStreams) throws OperationFailedException {
        ModelNode rsp = executeOperation(operation, inputStreams);
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
    @Override
    public void executeForFailure(ModelNode operation, InputStream...inputStreams) {
        try {
            executeForResult(operation, inputStreams);
            Assert.fail("Should have given error");
        } catch (OperationFailedException expected) {
        }
    }


    /**
     * Reads the persisted subsystem xml
     *
     * @return the xml
     */
    @Override
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
    @Override
    public void validateOperation(ModelNode operation) {
        operationValidator.validateOperation(operation);
    }

    @Override
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
                for (T legacyService: legacyServices.values()) {
                    legacyService.shutdown();
                }
            }
        }
    }

    @Override
    public ImmutableManagementResourceRegistration getRootRegistration() {
        return rootRegistration;
    }

    protected void addLegacyKernelService(ModelVersion modelVersion, T legacyServices) {
        this.legacyServices.put(modelVersion, legacyServices);
    }

    protected  ModelNode internalExecute(ModelNode operation, OperationStepHandler handler) {
        return controllerService.internalExecute(operation, OperationMessageHandler.DISCARD, OperationTransactionControl.COMMIT, null, handler);
    }

    protected TransformationContext createTransformationContext(TransformationTarget target) {
        return new TestTransformationContext(target, ModelTestModelControllerService.grabRootResource(this));
    }

    private class TestTransformationContext implements TransformationContext {

        private final TransformationTarget target;
        private final Resource rootResource;

        TestTransformationContext(TransformationTarget target, Resource rootResource){
            this.target = target;
            this.rootResource = rootResource;
        }

        @Override
        public TransformationTarget getTarget() {
            return target;
        }

        @Override
        public ProcessType getProcessType() {
            return controllerService.getProcessType();
        }

        @Override
        public RunningMode getRunningMode() {
            return controllerService.getRunningMode();
        }

        @Override
        public ImmutableManagementResourceRegistration getResourceRegistration(final PathAddress address) {
            return controllerService.getRootRegistration().getSubModel(address);
        }

        @Override
        public ImmutableManagementResourceRegistration getResourceRegistrationFromRoot(PathAddress address) {
            return controllerService.getRootRegistration().getSubModel(address);
        }

        @Override
        public Resource readResource(final PathAddress address) {
            return Resource.Tools.navigate(rootResource, address);
        }

        @Override
        public Resource readResourceFromRoot(final PathAddress address) {
            return Resource.Tools.navigate(rootResource, address);
        }

        @Override
        public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
            return node.resolve();
        }
    };
}
