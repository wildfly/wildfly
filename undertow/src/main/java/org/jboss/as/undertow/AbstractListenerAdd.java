package org.jboss.as.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.xnio.Pool;
import org.xnio.XnioWorker;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
abstract class AbstractListenerAdd extends AbstractAddStepHandler {
    protected String name;
    protected String bindingRef;
    protected String workerName;
    protected String bufferPoolName;
    protected String serverName;
    private final AbstractListenerResourceDefinition listenerDefinition;
    protected boolean enabled;


    AbstractListenerAdd(AbstractListenerResourceDefinition definition) {
        this.listenerDefinition = definition;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : listenerDefinition.getAttributes()) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathAddress parent = address.subAddress(0, address.size() - 1);
        name = address.getLastElement().getValue();
        bindingRef = AbstractListenerResourceDefinition.SOCKET_BINDING.resolveModelAttribute(context, model).asString();
        workerName = AbstractListenerResourceDefinition.WORKER.resolveModelAttribute(context, model).asString();
        bufferPoolName = AbstractListenerResourceDefinition.BUFFER_POOL.resolveModelAttribute(context, model).asString();
        enabled = AbstractListenerResourceDefinition.ENABLED.resolveModelAttribute(context, model).asBoolean();
        serverName = parent.getLastElement().getValue();
        if (enabled){
            installService(context, model, verificationHandler, newControllers);
        }
    }

    protected void addDefaultDependencies(ServiceBuilder<? extends AbstractListenerService<?>> serviceBuilder, AbstractListenerService<?> service) {
        serviceBuilder.addDependency(UndertowService.WORKER.append(workerName), XnioWorker.class, service.getWorker())
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingRef), SocketBinding.class, service.getBinding())
                .addDependency(UndertowService.BUFFER_POOL.append(bufferPoolName), Pool.class, service.getBufferPool())
                .addDependency(UndertowService.SERVER.append(serverName), Server.class, service.getServerService());
    }

    abstract ServiceName constructServiceName(final String name);

    abstract void installService(OperationContext context, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException;

}
