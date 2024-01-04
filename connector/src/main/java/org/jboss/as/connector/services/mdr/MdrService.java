/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.mdr;

import static org.jboss.as.connector.logging.ConnectorLogger.MDR_LOGGER;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A MdrService. it provide access to IronJacamar's metadata repository
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public final class MdrService implements Service<AS7MetadataRepository> {

    private final AS7MetadataRepository value;

    /**
     * Create instance
     */
    public MdrService() {
        this.value = new AS7MetadataRepositoryImpl();
    }

    @Override
    public AS7MetadataRepository getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        MDR_LOGGER.debugf("Starting service MDR");
    }

    @Override
    public void stop(StopContext context) {

    }

}
