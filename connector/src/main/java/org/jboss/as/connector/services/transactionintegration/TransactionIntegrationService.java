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

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.txn.integration.JBossContextXATerminator;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.tx.jbossts.TransactionIntegrationImpl;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.jboss.tm.usertx.UserTransactionRegistry;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 * A WorkManager Service.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public final class TransactionIntegrationService implements Service {

    private final Consumer<TransactionIntegration> tiConsumer;

    private final Supplier<TransactionSynchronizationRegistry> tsrSupplier;

    private final Supplier<UserTransactionRegistry> utrSupplier;

    private final Supplier<JBossContextXATerminator> terminatorSupplier;

    private final Supplier<XAResourceRecoveryRegistry> rrSupplier;

    /** create an instance **/
    public TransactionIntegrationService(final Consumer<TransactionIntegration> tiConsumer,
                                         final Supplier<TransactionSynchronizationRegistry> tsrSupplier,
                                         final Supplier<UserTransactionRegistry> utrSupplier,
                                         final Supplier<JBossContextXATerminator> terminatorSupplier,
                                         final Supplier<XAResourceRecoveryRegistry> rrSupplier) {
        this.tiConsumer = tiConsumer;
        this.tsrSupplier = tsrSupplier;
        this.utrSupplier = utrSupplier;
        this.terminatorSupplier = terminatorSupplier;
        this.rrSupplier = rrSupplier;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        tiConsumer.accept(new TransactionIntegrationImpl(ContextTransactionManager.getInstance(),
                tsrSupplier.get(), utrSupplier.get(), terminatorSupplier.get(), rrSupplier.get()));
        ROOT_LOGGER.debugf("Starting Jakarta Connectors TransactionIntegrationService");
    }

    @Override
    public void stop(final StopContext context) {
        tiConsumer.accept(null);
    }
}
