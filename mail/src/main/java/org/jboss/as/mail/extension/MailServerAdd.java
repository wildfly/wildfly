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
        //Util.copyModel(operation, model, OUTBOUND_SOCKET_BINDING_REF, SSL, FROM, USER_NAME, PASSWORD);
    }


    /**
     * Make any runtime changes necessary to effect the changes indicated by the given {@code operation}. Executes
     * after {@link #populateModel(org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode)}, so the given {@code model}
     * parameter will reflect any changes made in that method.
     * <p>
     * This default implementation does nothing.
     * </p>
     *
     * @param context             the operation context
     * @param operation           the operation being executed
     * @param model               persistent configuration model node that corresponds to the address of {@code operation}
     * @param verificationHandler step handler that can be added as a listener to any new services installed in order to
     *                            validate the services installed correctly during the
     *                            {@link org.jboss.as.controller.OperationContext.Stage#VERIFY VERIFY stage}
     * @param newControllers      holder for the {@link org.jboss.msc.service.ServiceController} for any new services installed by the method. The
     *                            method should add the {@code ServiceController} for any new services to this list. If the
     *                            overall operation needs to be rolled back, the list will be used in
     *                            {@link #rollbackRuntime(org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode, java.util.List)}  to automatically removed
     *                            the newly added services
     * @throws org.jboss.as.controller.OperationFailedException
     *          if {@code operation} is invalid or updating the runtime otherwise fails
     */
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        super.performRuntime(context, operation, model, verificationHandler, newControllers);
    }
}
