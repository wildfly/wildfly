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

import org.hibernate.stat.Statistics;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jpa.spi.ManagementAdaptor;
import org.jboss.dmr.ModelNode;

import java.util.Locale;

/**
 * Contains management support for Hibernate
 *
 * @author Scott Marlow
 */
public class HibernateManagementAdaptor implements ManagementAdaptor {

    private static final String PROVIDER_LABEL = "hibernate-persistence-unit";
    public static final String OPERATION_CLEAR = "clear";
    public static final String OPERATION_SUMMARY = "summary";
    public static final String OPERATION_STATISTICS_ENABLED = "enabled";
    public static final String OPERATION_ENTITY_DELETE_COUNT = "entity-delete-count";
    public static final String OPERATION_ENTITY_INSERT_COUNT = "entity-insert-count";
    public static final String OPERATION_ENTITY_LOAD_COUNT = "entity-load-count";
    public static final String OPERATION_ENTITY_FETCH_COUNT = "entity-fetch-count";
    public static final String OPERATION_ENTITY_UPDATE_COUNT = "entity-update-count";
    public static final String OPERATION_QUERY_EXECUTION_COUNT = "query-execution-count";
    public static final String OPERATION_QUERY_EXECUTION_MAX_TIME = "query-execution-max-time";
    public static final String OPERATION_QUERY_EXECUTION_MAX_TIME_QUERY_STRING = "query-execution-max-time-query-string";
    public static final String OPERATION_QUERY_CACHE_HIT_COUNT = "query-cache-hit-count";
    public static final String OPERATION_QUERY_CACHE_MISS_COUNT = "query-cache-miss-count";
    public static final String OPERATION_QUERY_CACHE_PUT_COUNT = "query-cache-put-count";
    public static final String OPERATION_FLUSH_COUNT = "flush-count";
    public static final String OPERATION_CONNECT_COUNT = "connect-count";
    public static final String OPERATION_SECOND_LEVEL_CACHE_HIT_COUNT = "second-level-cache-hit-count";
    public static final String OPERATION_SECOND_LEVEL_CACHE_MISS_COUNT = "second-level-cache-miss-count";
    public static final String OPERATION_SECOND_LEVEL_CACHE_PUT_COUNT = "second-level-cache-put-count";
    public static final String OPERATION_SESSION_CLOSE_COUNT = "session-close-count";
    public static final String OPERATION_SESSION_OPEN_COUNT = "session-open-count";
    public static final String OPERATION_COLLECTION_LOAD_COUNT = "collection-load-count";
    public static final String OPERATION_COLLECTION_FETCH_COUNT = "collection-fetch-count";
    public static final String OPERATION_COLLECTION_UPDATE_COUNT = "collection-update-count";
    public static final String OPERATION_COLLECTION_REMOVE_COUNT = "collection-remove-count";
    public static final String OPERATION_COLLECTION_RECREATED_COUNT = "collection-recreated-count";
    public static final String OPERATION_SUCCESSFUL_TRANSACTION_COUNT = "successful-transaction-count";
    public static final String OPERATION_COMPLETED_TRANSACTION_COUNT = "completed-transaction-count";
    public static final String OPERATION_PREPARED_STATEMENT_COUNT = "prepared-statement-count";
    public static final String OPERATION_CLOSE_STATEMENT_COUNT = "close-statement-count";
    public static final String OPERATION_OPTIMISTIC_FAILURE_COUNT = "optimistic-failure-count";

    @Override
    public void register(final ManagementResourceRegistration jpaSubsystemDeployments) {

        // setup top level statistics
        DescriptionProvider topLevelDescriptions = new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                // get description/type for each top level Hibernate statistic
                return HibernateDescriptions.describeTopLevelAttributes(locale);
            }
        };

        final ManagementResourceRegistration jpaHibernateRegistration =
            jpaSubsystemDeployments.registerSubModel(PathElement.pathElement(getIdentificationLabel()), topLevelDescriptions);

        registerStatisticAttributes(jpaHibernateRegistration);

        registerStatisticOperations(jpaHibernateRegistration);

        // setup 2lc statistics
//        DescriptionProvider secondLevelCacheDescriptions = new DescriptionProvider() {
//
//            @Override
//            public ModelNode getModelDescription(Locale locale) {
//                // get description/type
//                return HibernateDescriptions.describeSecondLevelCacheAttributes(locale);
//            }
//        };


//        final ManagementResourceRegistration secondLevelCacheRegistration =
//            jpaHibernateRegistration.registerSubModel(PathElement.pathElement("cache"), secondLevelCacheDescriptions);

//        registerSecondLevelCacheAttributes(secondLevelCacheRegistration);
//
// TODO:  handle 2lc and other stats

    }

    private void registerStatisticOperations(ManagementResourceRegistration jpaHibernateRegistration) {
        /**
         * reset all statistics
         */
        DescriptionProvider clear = new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return HibernateDescriptions.clear(locale);
            }
        };

        OperationStepHandler clearHandler = new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                stats.clear();
            }
        };
        jpaHibernateRegistration.registerOperationHandler(OPERATION_CLEAR, clearHandler, clear);

        /**
         * log statistics at INFO level
         */
        DescriptionProvider summary = new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return HibernateDescriptions.summary(locale);
            }
        };

        OperationStepHandler summaryHandler = new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                stats.logSummary();
            }
        };
        jpaHibernateRegistration.registerOperationHandler(OPERATION_SUMMARY, summaryHandler, summary);
    }

    private void registerStatisticAttributes(ManagementResourceRegistration jpaHibernateRegistration) {

        /**
         * Get global number of entity deletes
         * @return entity deletion count
         */
        jpaHibernateRegistration.registerMetric(OPERATION_ENTITY_DELETE_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getEntityDeleteCount());
            }
        });

        /**
         * Get global number of entity inserts
         * @return entity insertion count
         */
        jpaHibernateRegistration.registerMetric(OPERATION_ENTITY_INSERT_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getEntityInsertCount());
            }
        });


        /**
         * Get global number of entity loads
         * @return entity load (from DB)
         */
        jpaHibernateRegistration.registerMetric(OPERATION_ENTITY_LOAD_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getEntityLoadCount());
            }
        });

        /**
         * Get global number of entity fetchs
         * @return entity fetch (from DB)
         */
        jpaHibernateRegistration.registerMetric(OPERATION_ENTITY_FETCH_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getEntityFetchCount());
            }
        });


        /**
         * Get global number of entity updates
         * @return entity update
         */
        jpaHibernateRegistration.registerMetric(OPERATION_ENTITY_UPDATE_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getEntityUpdateCount());
            }
        });

        /**
         * Get global number of executed queries
         * @return query execution count
         */
        jpaHibernateRegistration.registerMetric(OPERATION_QUERY_EXECUTION_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getQueryExecutionCount());
            }
        });

        /**
         * Get the time in milliseconds of the slowest query.
         */
        jpaHibernateRegistration.registerMetric(OPERATION_QUERY_EXECUTION_MAX_TIME, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getQueryExecutionMaxTime());
            }
        });

        /**
         * Get the query string for the slowest query.
         */
        jpaHibernateRegistration.registerMetric(OPERATION_QUERY_EXECUTION_MAX_TIME_QUERY_STRING, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                String sql = stats.getQueryExecutionMaxTimeQueryString();
                if (sql != null) {
                    response.set(sql);
                } else {
                    context.getResult();        // result will be undefined
                }
            }
        });

        /**
         * Get the global number of cached queries successfully retrieved from cache
         */
        jpaHibernateRegistration.registerMetric(OPERATION_QUERY_CACHE_HIT_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getQueryCacheHitCount());
            }
        });

        /**
         * Get the global number of cached queries *not* found in cache
         */
        jpaHibernateRegistration.registerMetric(OPERATION_QUERY_CACHE_MISS_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getQueryCacheMissCount());
            }
        });

        /**
         * Get the global number of cacheable queries put in cache
         */
        jpaHibernateRegistration.registerMetric(OPERATION_QUERY_CACHE_PUT_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getQueryCachePutCount());
            }
        });

        /**
         * Get the global number of flush executed by sessions (either implicit or explicit)
         */
        jpaHibernateRegistration.registerMetric(OPERATION_FLUSH_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getFlushCount());
            }
        });

        /**
         * Get the global number of connections asked by the sessions
         * (the actual number of connections used may be much smaller depending
         * whether you use a connection pool or not)
         */
        jpaHibernateRegistration.registerMetric(OPERATION_CONNECT_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getConnectCount());
            }
        });

        /**
         * Global number of cacheable entities/collections successfully retrieved from the cache
         */
        jpaHibernateRegistration.registerMetric(OPERATION_SECOND_LEVEL_CACHE_HIT_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getSecondLevelCacheHitCount());
            }
        });

        /**
         * Global number of cacheable entities/collections not found in the cache and loaded from the database.
         */
        jpaHibernateRegistration.registerMetric(OPERATION_SECOND_LEVEL_CACHE_MISS_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getSecondLevelCacheMissCount());
            }
        });

        /**
         * Global number of cacheable entities/collections put in the cache
         */
        jpaHibernateRegistration.registerMetric(OPERATION_SECOND_LEVEL_CACHE_PUT_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getSecondLevelCachePutCount());
            }
        });

        /**
         * Global number of sessions closed
         */
        jpaHibernateRegistration.registerMetric(OPERATION_SESSION_CLOSE_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getSessionCloseCount());
            }
        });

        /**
         * Global number of sessions opened
         */
        jpaHibernateRegistration.registerMetric(OPERATION_SESSION_OPEN_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getSessionOpenCount());
            }
        });

        /**
         * Global number of collections loaded
         */
        jpaHibernateRegistration.registerMetric(OPERATION_COLLECTION_LOAD_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getCollectionLoadCount());
            }
        });

        /**
         * Global number of collections fetched
         */
        jpaHibernateRegistration.registerMetric(OPERATION_COLLECTION_FETCH_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getCollectionFetchCount());
            }
        });

        /**
         * Global number of collections updated
         */
        jpaHibernateRegistration.registerMetric(OPERATION_COLLECTION_UPDATE_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getCollectionUpdateCount());
            }
        });


        /**
         * Global number of collections removed
         */
        //even on inverse="true"
        jpaHibernateRegistration.registerMetric(OPERATION_COLLECTION_REMOVE_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getCollectionRemoveCount());
            }
        });

        /**
         * Global number of collections recreated
         */
        jpaHibernateRegistration.registerMetric(OPERATION_COLLECTION_RECREATED_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getCollectionRecreateCount());
            }
        });

        /**
         * The number of transactions we know to have been successful
         */
        jpaHibernateRegistration.registerMetric(OPERATION_SUCCESSFUL_TRANSACTION_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getSuccessfulTransactionCount());
            }
        });

        /**
         * The number of transactions we know to have completed
         */
        jpaHibernateRegistration.registerMetric(OPERATION_COMPLETED_TRANSACTION_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getTransactionCount());
            }
        });

        /**
         * The number of prepared statements that were acquired
         */
        jpaHibernateRegistration.registerMetric(OPERATION_PREPARED_STATEMENT_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getPrepareStatementCount());
            }
        });

        /**
         * The number of prepared statements that were released
         */
        jpaHibernateRegistration.registerMetric(OPERATION_CLOSE_STATEMENT_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getCloseStatementCount());
            }
        });

        /**
         * The number of <tt>StaleObjectStateException</tt>s
         * that occurred
         */
        jpaHibernateRegistration.registerMetric(OPERATION_OPTIMISTIC_FAILURE_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                response.set(stats.getOptimisticFailureCount());
            }
        });

        /**
         * enable/disable statistics attribute
         */
        // void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler, OperationStepHandler writeHandler, AttributeAccess.Storage storage);
        jpaHibernateRegistration.registerReadWriteAttribute(OPERATION_STATISTICS_ENABLED,
            new AbstractMetricsHandler() {  // readHandler
                @Override
                void handle(final ModelNode response, final String name, Statistics stats, OperationContext context) {
                    response.set(stats.isStatisticsEnabled());
                }
            },
            StatisticsEnabledWriteHandler.INSTANCE,
            AttributeAccess.Storage.RUNTIME
        );

    }

    private void registerSecondLevelCacheAttributes(ManagementResourceRegistration extendedEntityRegistration) {

    }



    @Override
    public String getIdentificationLabel() {
        return PROVIDER_LABEL;
    }

    abstract static class AbstractMetricsHandler implements OperationStepHandler {

        abstract void handle(ModelNode response, String name, Statistics stats, OperationContext context);

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));

            final Resource jpa = context.getRootResource().navigate(address.subAddress(0, address.size() - 1));
            final ModelNode subModel = jpa.getModel();

            final ModelNode node = jpa.requireChild(address.getLastElement()).getModel();
            final String puname = node.require("scoped-unit-name").asString();
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws
                    OperationFailedException {
                    Statistics stats = ManagementUtility.getStatistics(context, puname);
                    if (stats != null) {
                        handle(context.getResult(), address.getLastElement().getValue(), stats, context);
                    }
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
            context.completeStep();
        }
    }


}
