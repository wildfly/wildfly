package org.jboss.as.connector.services;

import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class ManagementRepositoryService implements Service<ManagementRepository> {

    private static final Logger log = Logger.getLogger("org.jboss.as.connector");

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
        log.debugf("started ManagementRepositoryService %s", context.getController().getName());

    }

    @Override
    public void stop(StopContext context) {
        log.debugf("stopped ManagementRepositoryService %s", context.getController().getName());

    }

}
