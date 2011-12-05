package org.jboss.as.mail.extension;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Handler responsible for adding the mail subsystem resource to the model
 *
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 */
class MailSubsystemAdd extends AbstractAddStepHandler {

    static final MailSubsystemAdd INSTANCE = new MailSubsystemAdd();

    private MailSubsystemAdd() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
        model.get(MailSubsystemModel.MAIL_SESSION);
    }
}
