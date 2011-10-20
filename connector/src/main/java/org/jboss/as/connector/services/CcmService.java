/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.connector.services;

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

import static org.jboss.as.connector.ConnectorLogger.ROOT_LOGGER;

/**
 * Cached connection manager service
 */
public class CcmService implements Service<CachedConnectionManager> {

    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("connector", "ccm");

    private final InjectedValue<TransactionIntegration> transactionIntegration = new InjectedValue<TransactionIntegration>();

    private volatile CachedConnectionManager value;

    private final boolean debug;
    private final boolean error;

    /** create an instance **/
    public CcmService(final boolean debug, final boolean error) {
        super();
        this.debug = debug;
        this.error = error;
    }

    @Override
    public CachedConnectionManager getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        value = new CachedConnectionManagerImpl(transactionIntegration.getValue().getTransactionManager(),
                                                transactionIntegration.getValue().getTransactionSynchronizationRegistry(),
                                                transactionIntegration.getValue().getUserTransactionRegistry());
        value.setDebug(debug);
        value.setError(error);

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
