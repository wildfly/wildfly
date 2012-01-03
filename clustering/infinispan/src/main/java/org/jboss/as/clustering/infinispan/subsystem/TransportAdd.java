package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransportAdd extends AbstractAddStepHandler {

    public static final TransportAdd INSTANCE = new TransportAdd();

    static ModelNode createOperation(ModelNode address, ModelNode existing) {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        populate(existing, operation);
        return operation;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        // this is not necessary as the CLI checks for duplicate addresses
        if (model.isDefined()) {
            ModelNode exception = new ModelNode() ;
            exception.get(FAILURE_DESCRIPTION).set("Add operation failed: singleton transport already exists.") ;
            throw new OperationFailedException(exception);
        }

        // copy operation data to the model
        populate(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        //
        context.reloadRequired();
    }

    private static void populate(ModelNode operation, ModelNode model) {
        // simply transfer the attributes from operation to model
        for (AttributeDefinition attr : CommonAttributes.TRANSPORT_ATTRIBUTES) {

            if (operation.hasDefined(attr.getName())) {
                model.get(attr.getName()).set(operation.get(attr.getName()));
            }
        }
    }
}
