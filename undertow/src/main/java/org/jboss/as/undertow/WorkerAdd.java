package org.jboss.as.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.XnioWorker;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class WorkerAdd extends AbstractAddStepHandler {
    public static final WorkerAdd INSTANCE = new WorkerAdd();

    private WorkerAdd(){

    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : WorkerResourceDefinition.ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final OptionMap.Builder builder = OptionMap.builder();

        for (OptionAttributeDefinition attr : WorkerResourceDefinition.ATTRIBUTES) {
            Option option = attr.getOption();
            ModelNode value = attr.resolveModelAttribute(context, model);
            if (attr.getType() == ModelType.INT) {
                builder.set((Option<Integer>) option, value.asInt());
            } else if (attr.getType() == ModelType.LONG) {
                builder.set(option, value.asLong());
            } else if (attr.getType() == ModelType.BOOLEAN) {
                builder.set(option, value.asBoolean());
            }
        }
        builder.set(Options.WORKER_NAME, name);

        final WorkerService workerService = new WorkerService(builder.getMap());
        final ServiceBuilder<XnioWorker> serviceBuilder = context.getServiceTarget().
                addService(UndertowService.WORKER.append(name), workerService);

        serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        final ServiceController<XnioWorker> serviceController = serviceBuilder.install();
        if (newControllers != null) {
            newControllers.add(serviceController);
        }


    }
}
