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

import org.hibernate.stat.QueryStatistics;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.jpa.spi.PersistenceUnitServiceRegistry;
import org.jboss.dmr.ModelNode;

/**
 * Handles reads of query metrics.
 *
 * @author Scott Marlow
 */
public abstract class QueryMetricsHandler extends AbstractRuntimeOnlyHandler {

    private final PersistenceUnitServiceRegistry persistenceUnitRegistry;

    private QueryMetricsHandler(PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        this.persistenceUnitRegistry = persistenceUnitRegistry;
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        handleQueryStatistics(context, operation);
        context.stepCompleted();
    }

    protected abstract void handle(QueryStatistics statistics, OperationContext context, String attributeName, String originalQueryName);

    private void handleQueryStatistics(OperationContext context, ModelNode operation) {
        final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        final String puResourceName = address.getElement(address.size() - 2).getValue();
        final String displayQueryName = address.getLastElement().getValue();
        ManagementLookup stats = ManagementLookup.create(persistenceUnitRegistry, puResourceName);
        if (stats != null) {
            String[] originalQueryNames = stats.getStatistics().getQueries();
            if (originalQueryNames != null) {
                for (String originalQueryName : originalQueryNames) {
                    if (QueryName.queryName(originalQueryName).getDisplayName().equals(displayQueryName)) {
                        QueryStatistics statistics = stats.getStatistics().getQueryStatistics(originalQueryName);
                        handle(statistics, context, operation.require(ModelDescriptionConstants.NAME).asString(), originalQueryName);
                        break;
                    }
                }
            }
        }
    }

    static final QueryMetricsHandler getExecutionCount(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        return new QueryMetricsHandler(persistenceUnitRegistry) {
            @Override
            protected void handle(QueryStatistics statistics, OperationContext context, String attributeName, String originalQueryName) {
                long count = statistics.getExecutionCount();
                context.getResult().set(count);
            }
        };
    }

    static final QueryMetricsHandler getCacheHitCount(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        return new QueryMetricsHandler(persistenceUnitRegistry) {
            @Override
            protected void handle(QueryStatistics statistics, OperationContext context, String attributeName, String originalQueryName) {
                long count = statistics.getCacheHitCount();
                context.getResult().set(count);
            }
        };
    }

    static final QueryMetricsHandler getCachePutCount(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        return new QueryMetricsHandler(persistenceUnitRegistry) {
            @Override
            protected void handle(QueryStatistics statistics, OperationContext context, String attributeName, String originalQueryName) {
                long count = statistics.getCachePutCount();
                context.getResult().set(count);
            }
        };
    }

    static final QueryMetricsHandler getCacheMissCount(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        return new QueryMetricsHandler(persistenceUnitRegistry) {
            @Override
            protected void handle(QueryStatistics statistics, OperationContext context, String attributeName, String originalQueryName) {
                long count = statistics.getCacheMissCount();
                context.getResult().set(count);
            }
        };
    }

    static final QueryMetricsHandler getExecutionRowCount(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        return new QueryMetricsHandler(persistenceUnitRegistry) {
            @Override
            protected void handle(QueryStatistics statistics, OperationContext context, String attributeName, String originalQueryName) {
                long count = statistics.getExecutionRowCount();
                context.getResult().set(count);
            }
        };
    }

    static final QueryMetricsHandler getExecutionAvgTime(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        return new QueryMetricsHandler(persistenceUnitRegistry) {
            @Override
            protected void handle(QueryStatistics statistics, OperationContext context, String attributeName, String originalQueryName) {
                long count = statistics.getExecutionAvgTime();
                context.getResult().set(count);
            }
        };
    }

    static final QueryMetricsHandler getExecutionMaxTime(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        return new QueryMetricsHandler(persistenceUnitRegistry) {
            @Override
            protected void handle(QueryStatistics statistics, OperationContext context, String attributeName, String originalQueryName) {
                long count = statistics.getExecutionMaxTime();
                context.getResult().set(count);
            }
        };
    }

    static final QueryMetricsHandler getExecutionMinTime(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        return new QueryMetricsHandler(persistenceUnitRegistry) {
            @Override
            protected void handle(QueryStatistics statistics, OperationContext context, String attributeName, String originalQueryName) {
                long count = statistics.getExecutionMinTime();
                context.getResult().set(count);
            }
        };
    }

    public static OperationStepHandler getOriginalName(PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        return new QueryMetricsHandler(persistenceUnitRegistry) {
            @Override
            protected void handle(QueryStatistics statistics, OperationContext context, String attributeName, String originalQueryName) {
                context.getResult().set(originalQueryName);
            }
        };
    }
}
