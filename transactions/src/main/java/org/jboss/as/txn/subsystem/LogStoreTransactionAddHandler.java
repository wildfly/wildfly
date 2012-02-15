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
class LogStoreTransactionAddHandler extends AbstractAddStepHandler {

    public static final LogStoreTransactionAddHandler INSTANCE = new LogStoreTransactionAddHandler();

    private LogStoreTransactionAddHandler() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final SimpleAttributeDefinition attribute : LogStoreProviders.TRANSACTION_ATTRIBUTE) {
            attribute.validateAndSet(operation, model);
        }

    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

    }
}
