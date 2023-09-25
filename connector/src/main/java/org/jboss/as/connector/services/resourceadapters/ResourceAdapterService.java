/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.resourceadapters;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import jakarta.resource.spi.ResourceAdapter;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class ResourceAdapterService implements Service<ResourceAdapter> {

    private ServiceName serviceName;
    private final ResourceAdapter value;

    /** create an instance **/
    public ResourceAdapterService(ServiceName serviceName, ResourceAdapter value) {
        super();
        this.serviceName = serviceName;
        this.value = value;
    }

    @Override
    public ResourceAdapter getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("Started ResourceAdapterService %s", serviceName);

    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("Stopped ResourceAdapterService %s", serviceName);
    }
}
