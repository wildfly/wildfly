package org.jboss.as.undertow;

import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
class AJPListenerAdd extends AbstractListenerAdd {

    AJPListenerAdd(AJPListenerResourceDefinition def) {
        super(def);
    }

    protected ServiceName constructServiceName(final String name) {
        return UndertowService.AJP_LISTENER.append(name);
    }

    @Override
    void installService(OperationContext context, ModelNode operation, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final AJPListenerService service = new AJPListenerService(name);
        final ServiceBuilder<AJPListenerService> serviceBuilder = context.getServiceTarget().addService(constructServiceName(name), service);
        addDefaultDependencies(serviceBuilder, service);

        serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        final ServiceController<AJPListenerService> serviceController = serviceBuilder.install();
        if (newControllers != null) {
            newControllers.add(serviceController);
        }
    }
}
