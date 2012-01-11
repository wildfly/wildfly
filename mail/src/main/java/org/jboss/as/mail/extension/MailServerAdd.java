package org.jboss.as.mail.extension;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.List;

import static org.jboss.as.mail.extension.MailSubsystemModel.FROM;
import static org.jboss.as.mail.extension.MailSubsystemModel.OUTBOUND_SOCKET_BINDING_REF;
import static org.jboss.as.mail.extension.MailSubsystemModel.PASSWORD;
import static org.jboss.as.mail.extension.MailSubsystemModel.SSL;
import static org.jboss.as.mail.extension.MailSubsystemModel.USER_NAME;

/**
 * @author Tomaz Cerar
 * @created 8.12.11 0:19
 */
public class MailServerAdd extends AbstractAddStepHandler {

    static final MailServerAdd INSTANCE = new MailServerAdd();

    private MailServerAdd() {
    }

    /**
     * Populate the given node in the persistent configuration model based on the values in the given operation.
     *
     * @param operation the operation
     * @param model     persistent configuration model node that corresponds to the address of {@code operation}
     * @throws org.jboss.as.controller.OperationFailedException
     *          if {@code operation} is invalid or populating the model otherwise fails
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF.validateAndSet(operation,model);
        MailServerDefinition.SSL.validateAndSet(operation,model);
        MailServerDefinition.USERNAME.validateAndSet(operation,model);
        MailServerDefinition.PASSWORD.validateAndSet(operation,model);
    }

}
