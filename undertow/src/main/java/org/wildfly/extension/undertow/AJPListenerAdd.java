package org.wildfly.extension.undertow;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
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
    AbstractListenerService<? extends AbstractListenerService> createService(String name, OperationContext context, ModelNode model) throws OperationFailedException {
        return new AJPListenerService(name);
    }

    @Override
    void configureAdditionalDependencies(OperationContext context, ServiceBuilder<? extends AbstractListenerService> serviceBuilder, ModelNode model, AbstractListenerService service) throws OperationFailedException {

    }
}
