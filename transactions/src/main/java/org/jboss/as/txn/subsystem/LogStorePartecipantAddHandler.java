package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.List;

/**
 *
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 */
class LogStorePartecipantAddHandler extends AbstractAddStepHandler {

    public static final LogStorePartecipantAddHandler INSTANCE = new LogStorePartecipantAddHandler();

    private LogStorePartecipantAddHandler() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final SimpleAttributeDefinition attribute : LogStoreProviders.PARTECIPANT_ATTRIBUTE) {
            attribute.validateAndSet(operation, model);
        }
        for (final SimpleAttributeDefinition attribute : LogStoreProviders.PARTECIPANT_RW_ATTRIBUTE) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

    }
}
