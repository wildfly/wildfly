package org.jboss.as.subsystem.test.otherservices.subsystem;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SubsystemAddWithOtherService extends AbstractBoottimeAddStepHandler {

    public static final SubsystemAddWithOtherService INSTANCE = new SubsystemAddWithOtherService();

    private final Logger log = Logger.getLogger(SubsystemAddWithOtherService.class);

    private SubsystemAddWithOtherService() {
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
    }

    /** {@inheritDoc} */
    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        MyService mine = new MyService();
        context.getServiceTarget().addService(MyService.NAME, mine)
            .addDependency(OtherService.NAME, OtherService.class, mine.otherValue)
            .install();

    }
}
