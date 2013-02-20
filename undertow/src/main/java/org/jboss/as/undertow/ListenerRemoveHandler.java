package org.jboss.as.undertow;

import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class ListenerRemoveHandler extends ServiceRemoveStepHandler {
    private AbstractListenerAdd listenerAddHandler;

    ListenerRemoveHandler(AbstractListenerAdd addOperation) {
        super(addOperation);
        this.listenerAddHandler = addOperation;
    }

    @Override
    protected ServiceName serviceName(String name) {
        return listenerAddHandler.constructServiceName(name);
    }
}
