
package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;

import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.PathAddress;
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

    KernelServices(ServiceContainer container, ModelController controller, StringConfigurationPersister persister) {
        this.container = container;
        this.controller = controller;
        this.persister = persister;
    }

    /**
     * Gets the service container
     * @return the service container
     */
    public ServiceContainer getContainer() {
        return container;
    }

    /**
     * Execute an operation in the model controller
     * @param operation the operation to execute
     * @return the result of the operation
     */
    public ModelNode executeOperation(ModelNode operation) {
        return controller.execute(operation, null, OperationTransactionControl.COMMIT, null);
    }

    /**
     * Reads the persisted subsystem xml
     * @return the xml
     */
    public String getPersistedSubsystemXml() {
        return persister.marshalled;
    }

    /**
     * Reads the whole model from the model controller
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