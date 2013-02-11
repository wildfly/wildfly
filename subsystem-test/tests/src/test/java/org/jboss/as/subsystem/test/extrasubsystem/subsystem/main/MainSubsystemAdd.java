package org.jboss.as.subsystem.test.extrasubsystem.subsystem.main;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.subsystem.test.extrasubsystem.subsystem.dependency.Dependency;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class MainSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final MainSubsystemAdd INSTANCE = new MainSubsystemAdd();

    private final Logger log = Logger.getLogger(MainSubsystemAdd.class);

    private MainSubsystemAdd() {
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

        MainService main = new MainService();
        context.getServiceTarget().addService(MainService.NAME, main)
            .addDependency(Dependency.NAME, Dependency.class, main.dependencyValue)
            .install();
    }
}
