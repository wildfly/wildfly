package org.jboss.as.jaxr.extension;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.jaxr.service.JAXRBootstrapService;
import org.jboss.as.jaxr.service.JAXRConfiguration;
import org.jboss.as.jaxr.service.JUDDIContextService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
class JAXRSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final JAXRSubsystemAdd INSTANCE = new JAXRSubsystemAdd();

    private JAXRSubsystemAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
    }

    @Override
    public void performBoottime(final OperationContext context, final ModelNode operation, ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                // [TODO] AS7-2278 JAXR configuration through the domain model
                JAXRConfiguration config = new JAXRConfiguration();
                ServiceTarget serviceTarget = context.getServiceTarget();
                newControllers.add(JAXRBootstrapService.addService(serviceTarget, config));
                newControllers.add(JUDDIContextService.addService(serviceTarget, config));
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
