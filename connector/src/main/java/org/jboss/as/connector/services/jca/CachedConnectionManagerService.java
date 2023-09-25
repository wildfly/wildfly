/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.jca;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Cached connection manager service
 */
public class CachedConnectionManagerService implements Service<CachedConnectionManager> {

    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("connector", "ccm");

    private final InjectedValue<TransactionIntegration> transactionIntegration = new InjectedValue<TransactionIntegration>();

    private volatile CachedConnectionManager value;

    private final boolean debug;
    private final boolean error;
    private final boolean ignoreUnknownConnections;

    /** create an instance **/
    public CachedConnectionManagerService(final boolean debug, final boolean error, boolean ignoreUnknownConnections) {
        super();
        this.debug = debug;
        this.error = error;
        this.ignoreUnknownConnections = ignoreUnknownConnections;
    }

    @Override
    public CachedConnectionManager getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        value = new CachedConnectionManagerImpl(transactionIntegration.getValue());
        value.setDebug(debug);
        value.setError(error);
        value.setIgnoreUnknownConnections(ignoreUnknownConnections);

        value.start();
        ROOT_LOGGER.debugf("Started CcmService %s", context.getController().getName());
    }

    @Override
    public void stop(StopContext context) {
        value.stop();
        ROOT_LOGGER.debugf("Stopped CcmService %s", context.getController().getName());
    }

    public Injector<TransactionIntegration> getTransactionIntegrationInjector() {
        return transactionIntegration;
    }
}
