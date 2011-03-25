package org.jboss.as.connector.services;

import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class ManagementRepositoryService implements Service<ManagementRepository> {

    private static final Logger log = Logger.getLogger("org.jboss.as.connector");
    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("connector", "admin-object");

    private final ManagementRepository value;

    /** create an instance **/
    public ManagementRepositoryService() {
        super();
        this.value = new ManagementRepository();

    }

    @Override
    public ManagementRepository getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        log.infof("started ManagementRepositoryService %s", context.getController().getName());

    }

    @Override
    public void stop(StopContext context) {
        log.infof("stopped ManagementRepositoryService %s", context.getController().getName());

    }

}
