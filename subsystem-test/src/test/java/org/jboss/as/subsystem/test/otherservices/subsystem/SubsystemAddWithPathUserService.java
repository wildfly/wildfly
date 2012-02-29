package org.jboss.as.subsystem.test.otherservices.subsystem;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.services.path.AbstractPathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SubsystemAddWithPathUserService extends AbstractBoottimeAddStepHandler {

    public static final SubsystemAddWithPathUserService INSTANCE = new SubsystemAddWithPathUserService();

    private SubsystemAddWithPathUserService() {
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

        PathUserService mine = new PathUserService();
        context.getServiceTarget().addService(SocketBindingUserService.NAME, mine)
            .addDependency(AbstractPathService.pathNameOf("p2"), String.class, mine.pathValue)
            .install();

    }
}
