package org.jboss.as.connector.services;

import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.Service;

public class JndiService implements Service<Object> {

    private static final Logger log = Logger.getLogger("org.jboss.as.connector");
    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("connector", "jndiname");

    private final Object value;

    private final String jndiName;

    /** create an instance **/
    public JndiService(Object value, String jndiName) {
        super();
        this.value = value;
        this.jndiName = jndiName;

    }

    @Override
    public Object getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        log.infof("started JndiService for %s jndi name", jndiName);

    }

    @Override
    public void stop(StopContext context) {
        log.infof("stopped JndiService for %s jndi name", jndiName);

    }

}
