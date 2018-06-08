/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.TransactionResourceDefinition.Attribute.LOCKING;
import static org.jboss.as.clustering.infinispan.subsystem.TransactionResourceDefinition.Attribute.MODE;
import static org.jboss.as.clustering.infinispan.subsystem.TransactionResourceDefinition.Attribute.STOP_TIMEOUT;

import java.util.Optional;
import java.util.function.Supplier;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.tm.EmbeddedTransactionManager;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.infinispan.TransactionManagerProvider;
import org.jboss.as.clustering.infinispan.TransactionSynchronizationRegistryProvider;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class TransactionServiceConfigurator extends ComponentServiceConfigurator<TransactionConfiguration> {

    private volatile Supplier<TransactionManager> tm;
    private volatile Supplier<TransactionSynchronizationRegistry> tsr;

    private volatile LockingMode locking;
    private volatile long timeout;
    private volatile TransactionMode mode;

    public TransactionServiceConfigurator(PathAddress address) {
        super(CacheComponent.TRANSACTION, address);
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        switch (this.mode) {
            case NONE: {
                break;
            }
            case BATCH: {
                this.tm = new Supplier<TransactionManager>() {
                    @Override
                    public TransactionManager get() {
                        return EmbeddedTransactionManager.getInstance();
                    }
                };
                break;
            }
            case NON_XA: {
                this.tsr = builder.requires(TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY);
            }
            default: {
                this.tm = builder.requires(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER);
            }
        }
        return super.register(builder);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.mode = ModelNodes.asEnum(MODE.resolveModelAttribute(context, model), TransactionMode.class);
        this.locking = ModelNodes.asEnum(LOCKING.resolveModelAttribute(context, model), LockingMode.class);
        this.timeout = STOP_TIMEOUT.resolveModelAttribute(context, model).asLong();
        return this;
    }

    @Override
    public TransactionConfiguration get() {
        return new ConfigurationBuilder().transaction()
                .lockingMode(this.locking)
                .cacheStopTimeout(this.timeout)
                .transactionManagerLookup(Optional.ofNullable(this.tm).map(Supplier::get).map(TransactionManagerProvider::new).orElse(null))
                .transactionSynchronizationRegistryLookup(Optional.ofNullable(this.tsr).map(Supplier::get).map(TransactionSynchronizationRegistryProvider::new).orElse(null))
                .transactionMode((this.mode == TransactionMode.NONE) ? org.infinispan.transaction.TransactionMode.NON_TRANSACTIONAL : org.infinispan.transaction.TransactionMode.TRANSACTIONAL)
                .useSynchronization(this.mode == TransactionMode.NON_XA)
                .recovery().enabled(this.mode == TransactionMode.FULL_XA).transaction()
                .create();
    }
}
