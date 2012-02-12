package org.jboss.as.connector.services;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import static org.jboss.as.connector.ConnectorLogger.ROOT_LOGGER;

public class AdminObjectService implements Service<Object> {

    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("connector", "admin-object");

    private final Object value;

    /** create an instance **/
    public AdminObjectService(Object value) {
        super();
        this.value = value;

    }

    @Override
    public Object getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("started AdminObjectService %s", context.getController().getName());

    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("stopped AdminObjectService %s", context.getController().getName());

    }

}
