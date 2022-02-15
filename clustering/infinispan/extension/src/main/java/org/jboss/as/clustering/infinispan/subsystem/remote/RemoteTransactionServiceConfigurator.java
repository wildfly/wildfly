/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem.remote;

import static org.jboss.as.clustering.infinispan.subsystem.remote.RemoteTransactionResourceDefinition.Attribute.*;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.TransactionConfiguration;
import org.infinispan.client.hotrod.configuration.TransactionConfigurationBuilder;
import org.infinispan.client.hotrod.transaction.manager.RemoteTransactionManager;
import org.jboss.as.clustering.infinispan.TransactionManagerProvider;
import org.jboss.as.clustering.infinispan.jakarta.TransactionManagerAdapter;
import org.jboss.as.clustering.infinispan.subsystem.ComponentServiceConfigurator;
import org.jboss.as.clustering.infinispan.subsystem.TransactionMode;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 * Configures the transaction component of a remote cache container.
 * @author Paul Ferraro
 */
@Deprecated
public class RemoteTransactionServiceConfigurator extends ComponentServiceConfigurator<TransactionConfiguration> {

    private volatile TransactionMode mode;

    RemoteTransactionServiceConfigurator(PathAddress address) {
        super(RemoteCacheContainerComponent.TRANSACTION, address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.mode = TransactionMode.valueOf(MODE.resolveModelAttribute(context, model).asString());
        return this;
    }

    @Override
    public TransactionConfiguration get() {
        TransactionConfigurationBuilder builder = new ConfigurationBuilder().transaction();
        switch (this.mode) {
            case NONE: {
                builder.transactionMode(org.infinispan.client.hotrod.configuration.TransactionMode.NONE);
                break;
            }
            case BATCH: {
                builder.transactionMode(org.infinispan.client.hotrod.configuration.TransactionMode.NON_DURABLE_XA);
                builder.transactionManagerLookup(new TransactionManagerProvider(RemoteTransactionManager.getInstance()));
                break;
            }
            case NON_XA: {
                Object tm = ContextTransactionManager.getInstance();
                builder.transactionMode(org.infinispan.client.hotrod.configuration.TransactionMode.NON_XA);
                builder.transactionManagerLookup(new TransactionManagerProvider(tm instanceof javax.transaction.TransactionManager ? (javax.transaction.TransactionManager) tm : new TransactionManagerAdapter((jakarta.transaction.TransactionManager) tm)));
                break;
            }
            case NON_DURABLE_XA: {
                Object tm = ContextTransactionManager.getInstance();
                builder.transactionMode(org.infinispan.client.hotrod.configuration.TransactionMode.NON_DURABLE_XA);
                builder.transactionManagerLookup(new TransactionManagerProvider(tm instanceof javax.transaction.TransactionManager ? (javax.transaction.TransactionManager) tm : new TransactionManagerAdapter((jakarta.transaction.TransactionManager) tm)));
                break;
            }
            default: {
                throw new IllegalArgumentException(this.mode.toString());
            }
        }
        return builder.create();
    }
}
