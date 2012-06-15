package org.jboss.as.subsystem.test;

import junit.framework.AssertionFailedError;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.subsystem.test.AbstractSubsystemTest.StringConfigurationPersister;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_TRANSFORMED_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;


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
     * @return the result of the operation
     */
    public ModelNode executeOperation(ModelNode operation) {
        return controller.execute(operation, null, OperationTransactionControl.COMMIT, null);
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
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        op.get(RECURSIVE).set(true);
        ModelNode result = executeOperation(op);
        return AbstractSubsystemTest.checkResultAndGetContents(result);
    }

    /**
     *
     * @param major  the major management version
     * @param minor  the major management version
     * @return the transformed model
     *
     * @deprecated use {@link #readTransformedModel(int, int, int)}
     */
    @Deprecated
    public ModelNode readTransformedModel(int major,int minor) {
        return readTransformedModel(major, minor, 0);
    }

    public ModelNode readTransformedModel(int major,int minor, int micro) {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_TRANSFORMED_RESOURCE_OPERATION);
        op.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        op.get(RECURSIVE).set(true);
        op.get(SUBSYSTEM).set(mainSubsystemName);
        op.get(ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION).set(major);
        op.get(ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION).set(minor);
        op.get(ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION).set(micro);
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
