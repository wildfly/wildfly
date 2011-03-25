package org.jboss.as.connector.services;

import javax.resource.cci.ConnectionFactory;

import org.jboss.jca.core.api.management.AdminObject;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class ConnectionFactoryService implements Service<Object> {

    private static final Logger log = Logger.getLogger("org.jboss.as.connector");
    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("connector", "connection-factory");

    private final Object value;

    /** create an instance **/
    public ConnectionFactoryService(Object value) {
        super();
        this.value = value;

    }

    @Override
    public Object getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        log.debugf("started ConnectionFactoryService %s", context.getController().getName());

    }

    @Override
    public void stop(StopContext context) {
        log.debugf("stopped ConnectionFactoryService %s", context.getController().getName());

    }

}
