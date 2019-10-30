/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn.ee.concurrency;

import org.glassfish.enterprise.concurrent.spi.TransactionSetupProvider;
import org.jboss.as.ee.concurrent.ServiceTransactionSetupProvider;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.transaction.TransactionManager;

/**
 * Service responsible for managing a transaction setup provider's lifecycle.
 *
 * @author Eduardo Martins
 */
public class TransactionSetupProviderService implements Service<TransactionSetupProvider> {

    public static final Class<?> SERVICE_VALUE_TYPE = TransactionSetupProvider.class;

    private final InjectedValue<TransactionManager> transactionManagerInjectedValue;

    private volatile TransactionSetupProvider transactionSetupProvider;

    /**
     */
    public TransactionSetupProviderService() {
        this.transactionManagerInjectedValue = new InjectedValue<>();
    }

    public InjectedValue<TransactionManager> getTransactionManagerInjectedValue() {
        return transactionManagerInjectedValue;
    }

    public void start(final StartContext context) throws StartException {
        transactionSetupProvider = new ServiceTransactionSetupProvider(new TransactionSetupProviderImpl(transactionManagerInjectedValue.getValue()), context.getController().getName());
    }

    public void stop(final StopContext context) {
        transactionSetupProvider = null;
    }

    public TransactionSetupProvider getValue() throws IllegalStateException {
        final TransactionSetupProvider value = this.transactionSetupProvider;
        if (value == null) {
            throw TransactionLogger.ROOT_LOGGER.transactionSetupProviderServiceNotStarted();
        }
        return value;
    }

}
