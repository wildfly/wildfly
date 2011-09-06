package org.jboss.as.connector.services;

import javax.resource.spi.ResourceAdapter;

import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.Service;

import static org.jboss.as.connector.ConnectorLogger.ROOT_LOGGER;

public class ResourceAdapterService implements Service<ResourceAdapter> {

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
        ROOT_LOGGER.debugf("started ResourceAdapterService %s", context.getController().getName());

    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("stopped ResourceAdapterService %s", context.getController().getName());

    }

}
