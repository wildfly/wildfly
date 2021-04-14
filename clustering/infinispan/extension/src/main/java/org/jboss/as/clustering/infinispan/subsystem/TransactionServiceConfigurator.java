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

import static org.jboss.as.clustering.controller.CommonRequirement.LOCAL_TRANSACTION_PROVIDER;
import static org.jboss.as.clustering.infinispan.subsystem.TransactionResourceDefinition.Attribute.LOCKING;
import static org.jboss.as.clustering.infinispan.subsystem.TransactionResourceDefinition.Attribute.MODE;
import static org.jboss.as.clustering.infinispan.subsystem.TransactionResourceDefinition.Attribute.STOP_TIMEOUT;
import static org.jboss.as.clustering.infinispan.subsystem.TransactionResourceDefinition.Attribute.COMPLETE_TIMEOUT;
import static org.jboss.as.clustering.infinispan.subsystem.TransactionResourceDefinition.TransactionRequirement.TRANSACTION_SYNCHRONIZATION_REGISTRY;

import java.util.EnumSet;

import javax.transaction.TransactionSynchronizationRegistry;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.configuration.cache.TransactionConfigurationBuilder;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.tm.EmbeddedTransactionManager;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.infinispan.TransactionManagerProvider;
import org.jboss.as.clustering.infinispan.TransactionSynchronizationRegistryProvider;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceDependency;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 * @author Paul Ferraro
 */
public class TransactionServiceConfigurator extends ComponentServiceConfigurator<TransactionConfiguration> {

    private volatile LockingMode locking;
    private volatile long stopTimeout;
    private volatile long transactionTimeout;
    private volatile TransactionMode mode;
    private volatile Dependency transactionDependency;
    private volatile SupplierDependency<TransactionSynchronizationRegistry> tsrDependency;

    public TransactionServiceConfigurator(PathAddress address) {
        super(CacheComponent.TRANSACTION, address);
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        new CompositeDependency(this.transactionDependency, this.tsrDependency).register(builder);
        return super.register(builder);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.mode = ModelNodes.asEnum(MODE.resolveModelAttribute(context, model), TransactionMode.class);
        this.locking = ModelNodes.asEnum(LOCKING.resolveModelAttribute(context, model), LockingMode.class);
        this.stopTimeout = STOP_TIMEOUT.resolveModelAttribute(context, model).asLong();
        this.transactionTimeout = COMPLETE_TIMEOUT.resolveModelAttribute(context, model).asLong();
        this.transactionDependency = !EnumSet.of(TransactionMode.NONE, TransactionMode.BATCH).contains(this.mode) ? new ServiceDependency(context.getCapabilityServiceName(LOCAL_TRANSACTION_PROVIDER.getName(), null)) : null;
        this.tsrDependency = this.mode == TransactionMode.NON_XA ? new ServiceSupplierDependency<>(context.getCapabilityServiceName(TRANSACTION_SYNCHRONIZATION_REGISTRY.getName(), null)) : null;
        return this;
    }

    @Override
    public TransactionConfiguration get() {
        TransactionConfigurationBuilder builder = new ConfigurationBuilder().transaction()
                .lockingMode(this.locking)
                .cacheStopTimeout(this.stopTimeout)
                .completedTxTimeout(transactionTimeout)
                .transactionMode((this.mode == TransactionMode.NONE) ? org.infinispan.transaction.TransactionMode.NON_TRANSACTIONAL : org.infinispan.transaction.TransactionMode.TRANSACTIONAL)
                .useSynchronization(this.mode == TransactionMode.NON_XA)
                .recovery().enabled(this.mode == TransactionMode.FULL_XA).transaction()
                ;

        switch (this.mode) {
            case NONE: {
                break;
            }
            case BATCH: {
                builder.transactionManagerLookup(new TransactionManagerProvider(EmbeddedTransactionManager.getInstance()));
                break;
            }
            case NON_XA: {
                builder.transactionSynchronizationRegistryLookup(new TransactionSynchronizationRegistryProvider(this.tsrDependency.get()));
                // fall through
            }
            default: {
                builder.transactionManagerLookup(new TransactionManagerProvider(ContextTransactionManager.getInstance()));
            }
        }
        return builder.create();
    }
}
