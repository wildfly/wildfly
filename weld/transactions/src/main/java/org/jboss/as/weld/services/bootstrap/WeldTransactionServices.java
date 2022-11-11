/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.services.bootstrap;

import java.util.function.Consumer;

import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.jboss.as.weld.ServiceNames;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.LocalUserTransaction;

/**
 * Service that implements welds {@link TransactionServices}
 * <p>
 * This class is thread safe, and does not require a happens-before action between construction and usage
 *
 * @author Stuart Douglas
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WeldTransactionServices implements TransactionServices, Service {

    public static final ServiceName SERVICE_NAME = ServiceNames.WELD_TRANSACTION_SERVICES_SERVICE_NAME;
    private final Consumer<WeldTransactionServices> weldTransactionServicesConsumer;
    private final boolean jtsEnabled;

    public WeldTransactionServices(final boolean jtsEnabled, final Consumer<WeldTransactionServices> weldTransactionServicesConsumer) {
        this.jtsEnabled = jtsEnabled;
        this.weldTransactionServicesConsumer = weldTransactionServicesConsumer;
    }

    @Override
    public UserTransaction getUserTransaction() {
        return LocalUserTransaction.getInstance();
    }

    @Override
    public boolean isTransactionActive() {
        try {
            final int status = ContextTransactionManager.getInstance().getStatus();
            return status == Status.STATUS_ACTIVE ||
                    status == Status.STATUS_COMMITTING ||
                    status == Status.STATUS_MARKED_ROLLBACK ||
                    status == Status.STATUS_PREPARED ||
                    status == Status.STATUS_PREPARING ||
                    status == Status.STATUS_ROLLING_BACK;
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void registerSynchronization(Synchronization synchronizedObserver) {
        try {
            final Synchronization synchronization;
            if (!jtsEnabled) {
                synchronization = synchronizedObserver;
            } else {
                synchronization = new JTSSynchronizationWrapper(synchronizedObserver);
            }
            ContextTransactionManager.getInstance().getTransaction().registerSynchronization(synchronization);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } catch (RollbackException e) {
            throw new RuntimeException(e);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(final StartContext context) {
        weldTransactionServicesConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        weldTransactionServicesConsumer.accept(null);
    }

}
