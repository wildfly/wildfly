/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.services.transactionintegration;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.txn.integration.JBossContextXATerminator;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.tx.jbossts.TransactionIntegrationImpl;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.jboss.tm.usertx.UserTransactionRegistry;

/**
 * A WorkManager Service.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public final class TransactionIntegrationService implements Service<TransactionIntegration> {

    private volatile TransactionIntegration value;

    private final InjectedValue<TransactionManager> tm = new InjectedValue<TransactionManager>();

    private final InjectedValue<TransactionSynchronizationRegistry> tsr = new InjectedValue<TransactionSynchronizationRegistry>();

    private final InjectedValue<UserTransactionRegistry> utr = new InjectedValue<UserTransactionRegistry>();

    private final InjectedValue<JBossContextXATerminator> terminator = new InjectedValue<JBossContextXATerminator>();

    private final InjectedValue<XAResourceRecoveryRegistry> rr = new InjectedValue<XAResourceRecoveryRegistry>();

    /** create an instance **/
    public TransactionIntegrationService() {
        super();
    }

    @Override
    public TransactionIntegration getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.value = new TransactionIntegrationImpl(tm.getValue(), tsr.getValue(), utr.getValue(), terminator.getValue(),
                rr.getValue());
        ROOT_LOGGER.debugf("Starting JCA TransactionIntegrationService");
    }

    @Override
    public void stop(StopContext context) {

    }

    public Injector<TransactionManager> getTmInjector() {
        return tm;
    }

    public Injector<TransactionSynchronizationRegistry> getTsrInjector() {
        return tsr;
    }

    public Injector<UserTransactionRegistry> getUtrInjector() {
        return utr;
    }

    public Injector<JBossContextXATerminator> getTerminatorInjector() {
        return terminator;
    }

    public Injector<XAResourceRecoveryRegistry> getRrInjector() {
        return rr;
    }

}
