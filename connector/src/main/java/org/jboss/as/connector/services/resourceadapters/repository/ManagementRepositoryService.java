package org.jboss.as.connector.services.resourceadapters.repository;

import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

public class ManagementRepositoryService implements Service<ManagementRepository> {

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
        ROOT_LOGGER.debugf("started ManagementRepositoryService %s", context.getController().getName());

    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("stopped ManagementRepositoryService %s", context.getController().getName());

    }

}
