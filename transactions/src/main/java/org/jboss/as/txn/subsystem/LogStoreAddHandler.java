package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.List;
import java.util.Locale;

/**
 *
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 */
class LogStoreAddHandler extends AbstractBoottimeAddStepHandler {

    public static final LogStoreAddHandler INSTANCE = new LogStoreAddHandler();

    private LogStoreAddHandler() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final SimpleAttributeDefinition attribute : LogStoreProviders.LOG_STORE_ATTRIBUTE) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws UnsupportedOperationException {

        //init JMX

    }

}
