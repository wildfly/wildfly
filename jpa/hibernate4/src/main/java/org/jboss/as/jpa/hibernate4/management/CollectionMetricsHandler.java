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

package org.jboss.as.jpa.hibernate4.management;

import org.hibernate.stat.CollectionStatistics;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.jpa.spi.PersistenceUnitServiceRegistry;
import org.jboss.dmr.ModelNode;

/**
 * Handles reads of collection metrics.
 *
 * @author Scott Marlow
 */
public abstract class CollectionMetricsHandler extends AbstractRuntimeOnlyHandler {

    private final PersistenceUnitServiceRegistry persistenceUnitRegistry;

    private CollectionMetricsHandler(PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        this.persistenceUnitRegistry = persistenceUnitRegistry;
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        CollectionStatistics statistics = collectionStatistics(operation);
        if (statistics != null) {
            handle(statistics, context, operation.require(ModelDescriptionConstants.NAME).asString());
        }
        context.stepCompleted();
    }

    protected abstract void handle(CollectionStatistics statistics, OperationContext context, String attributeName);

    private CollectionStatistics collectionStatistics(ModelNode operation) {
        final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        final String puResourceName = address.getElement(address.size() - 2).getValue();
        final String roleName = address.getLastElement().getValue();
        ManagementLookup stats = ManagementLookup.create(persistenceUnitRegistry, puResourceName);
        return stats == null ? null : stats.getStatistics().getCollectionStatistics(roleName);
    }


    static final CollectionMetricsHandler getLoadCount(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        return new CollectionMetricsHandler(persistenceUnitRegistry) {
            @Override
            protected void handle(CollectionStatistics statistics, OperationContext context, String attributeName) {
                long count = statistics.getLoadCount();
                context.getResult().set(count);
            }
        };
    }

    static final CollectionMetricsHandler getFetchCount(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        return new CollectionMetricsHandler(persistenceUnitRegistry) {
            @Override
            protected void handle(CollectionStatistics statistics, OperationContext context, String attributeName) {
                long count = statistics.getFetchCount();
                context.getResult().set(count);
            }
        };
    }

    static final CollectionMetricsHandler getRecreateCount(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        return new CollectionMetricsHandler(persistenceUnitRegistry) {
            @Override
            protected void handle(CollectionStatistics statistics, OperationContext context, String attributeName) {
                long count = statistics.getRecreateCount();
                context.getResult().set(count);
            }
        };
    }

    static final CollectionMetricsHandler getRemoveCount(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        return new CollectionMetricsHandler(persistenceUnitRegistry) {
            @Override
            protected void handle(CollectionStatistics statistics, OperationContext context, String attributeName) {
                long count = statistics.getRemoveCount();
                context.getResult().set(count);
            }
        };
    }

    static final CollectionMetricsHandler getUpdateCount(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        return new CollectionMetricsHandler(persistenceUnitRegistry) {
            @Override
            protected void handle(CollectionStatistics statistics, OperationContext context, String attributeName) {
                long count = statistics.getUpdateCount();
                context.getResult().set(count);
            }
        };
    }

}
