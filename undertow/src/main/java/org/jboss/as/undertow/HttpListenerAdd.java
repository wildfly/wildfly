package org.jboss.as.undertow;

import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class HttpListenerAdd extends AbstractListenerAdd {

    HttpListenerAdd(AbstractListenerResourceDefinition definition) {
        super(definition);
    }

    protected ServiceName constructServiceName(final String name) {
        return UndertowService.HTTP_LISTENER.append(name);
    }

    @Override
    void installService(OperationContext context, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final HttpListenerService service = createService(name);
        final ServiceBuilder<HttpListenerService> serviceBuilder = context.getServiceTarget().addService(constructServiceName(name), service);
        addDefaultDependencies(serviceBuilder, service);

        configureAdditionalDependencies(context, serviceBuilder, model, service);
        serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        final ServiceController<HttpListenerService> serviceController = serviceBuilder.install();
        if (newControllers != null) {
            newControllers.add(serviceController);
        }
    }

    protected HttpListenerService createService(final String name) {
        return new HttpListenerService(name);
    }

    protected void configureAdditionalDependencies(OperationContext context, ServiceBuilder<HttpListenerService> serviceBuilder, ModelNode model, HttpListenerService service) throws OperationFailedException {
    }
}
