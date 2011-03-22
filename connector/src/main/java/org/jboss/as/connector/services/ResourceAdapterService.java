package org.jboss.as.connector.services;

import javax.resource.spi.ResourceAdapter;

import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.Service;

public class ResourceAdapterService implements Service<ResourceAdapter> {

    private static final Logger log = Logger.getLogger("org.jboss.as.connector");

    private final ResourceAdapter value;

    /** create an instance **/
    public ResourceAdapterService(ResourceAdapter value) {
        super();
        this.value = value;

    }

    @Override
    public ResourceAdapter getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        log.debugf("started ResourceAdapterService %s", context.getController().getName());

    }

    @Override
    public void stop(StopContext context) {
        log.debugf("stopped ResourceAdapterService %s", context.getController().getName());

    }

}
