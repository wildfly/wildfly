package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_ALIASES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_TRANSFORMED_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.subsystem.test.AbstractSubsystemTest.StringConfigurationPersister;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;


/**
 * Allows access to the service container and the model controller
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class KernelServices {

    private volatile ServiceContainer container;
    private final ModelController controller;
    private final StringConfigurationPersister persister;
    private final OperationValidator operationValidator;
    private final String mainSubsystemName;

    KernelServices(ServiceContainer container, ModelController controller, StringConfigurationPersister persister, OperationValidator operationValidator, String mainSubsystemName) {
        this.container = container;
        this.controller = controller;
        this.persister = persister;
        this.operationValidator = operationValidator;
        this.mainSubsystemName = mainSubsystemName;
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

    /**
     * Execute an operation in the model controller, expecting succes and return the "result" node
     *
     * @param operation the operation to execute
     * @return the result of the operation
     * @throws OperationFailedException if the operation failed
     */
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
     * Create an operation
     *
     * @param operationName the name of the operation
     * @param address the address
     * @throws IllegalArgumentException if the address is bad
     */
    public ModelNode createOperation(String operationName, String...address) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        if (address.length > 0) {
            if (address.length % 2 != 0) {
                throw new IllegalArgumentException("Address must be in pairs");
            }
            for (String addr : address) {
                operation.get(OP_ADDR).add(addr);
            }
        } else {
            operation.get(OP_ADDR).setEmptyList();
        }

        return operation;
    }

    /**
     * Reads the persisted subsystem xml
     *
     * @return the xml
     */
    public String getPersistedSubsystemXml() {
        return persister.marshalled;
    }

    /**
     * Reads the whole model from the model controller
     *
     * @return the whole model
     */
    public ModelNode readWholeModel() {
        return readWholeModel(false);
    }

    /**
     * Reads the whole model from the model controller
     *
     * @return the whole model
     */
    public ModelNode readWholeModel(boolean includeAliases) {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        op.get(RECURSIVE).set(true);
        if (includeAliases) {
            op.get(INCLUDE_ALIASES).set(true);
        }
        ModelNode result = executeOperation(op);
        return AbstractSubsystemTest.checkResultAndGetContents(result);
    }

    public ModelNode readTransformedModel(int major,int minor) {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_TRANSFORMED_RESOURCE_OPERATION);
        op.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        op.get(RECURSIVE).set(true);
        op.get(SUBSYSTEM).set(mainSubsystemName);
        op.get(ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION).set(major);
        op.get(ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION).set(minor);
        ModelNode result = executeOperation(op);
        return AbstractSubsystemTest.checkResultAndGetContents(result);
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
        }
    }
}