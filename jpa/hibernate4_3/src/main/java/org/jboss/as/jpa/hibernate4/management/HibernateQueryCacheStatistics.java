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

package org.jboss.as.jpa.hibernate4.management;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.jipijapa.management.spi.EntityManagerFactoryAccess;
import org.jipijapa.management.spi.Operation;
import org.jipijapa.management.spi.PathAddress;

/**
 * Hibernate query cache statistics
 *
 * @author Scott Marlow
 */
public class HibernateQueryCacheStatistics extends HibernateAbstractStatistics {

    public static final String ATTRIBUTE_QUERY_NAME = "query-name";
    public static final String OPERATION_QUERY_EXECUTION_COUNT = "query-execution-count";
    public static final String OPERATION_QUERY_EXECUTION_ROW_COUNT = "query-execution-row-count";
    public static final String OPERATION_QUERY_EXECUTION_AVG_TIME = "query-execution-average-time";
    public static final String OPERATION_QUERY_EXECUTION_MAX_TIME = "query-execution-max-time";
    public static final String OPERATION_QUERY_EXECUTION_MIN_TIME = "query-execution-min-time";
    public static final String OPERATION_QUERY_CACHE_HIT_COUNT = "query-cache-hit-count";
    public static final String OPERATION_QUERY_CACHE_MISS_COUNT = "query-cache-miss-count";
    public static final String OPERATION_QUERY_CACHE_PUT_COUNT = "query-cache-put-count";

    public HibernateQueryCacheStatistics() {
        /**
         * specify the different operations
         */
        operations.put(ATTRIBUTE_QUERY_NAME, showQueryName);
        types.put(ATTRIBUTE_QUERY_NAME,String.class);

        operations.put(OPERATION_QUERY_EXECUTION_COUNT, queryExecutionCount);
        types.put(OPERATION_QUERY_EXECUTION_COUNT, Long.class);

        operations.put(OPERATION_QUERY_EXECUTION_ROW_COUNT, queryExecutionRowCount);
        types.put(OPERATION_QUERY_EXECUTION_ROW_COUNT, Long.class);

        operations.put(OPERATION_QUERY_EXECUTION_AVG_TIME, queryExecutionAverageTime);
        types.put(OPERATION_QUERY_EXECUTION_AVG_TIME, Long.class);

        operations.put(OPERATION_QUERY_EXECUTION_MAX_TIME, queryExecutionMaximumTime);
        types.put(OPERATION_QUERY_EXECUTION_MAX_TIME, Long.class);

        operations.put(OPERATION_QUERY_EXECUTION_MIN_TIME, queryExecutionMinimumTime);
        types.put(OPERATION_QUERY_EXECUTION_MIN_TIME, Long.class);

        operations.put(OPERATION_QUERY_CACHE_HIT_COUNT, queryCacheHitCount);
        types.put(OPERATION_QUERY_CACHE_HIT_COUNT, Long.class);

        operations.put(OPERATION_QUERY_CACHE_MISS_COUNT, queryCacheMissCount);
        types.put(OPERATION_QUERY_CACHE_MISS_COUNT, Long.class);

        operations.put(OPERATION_QUERY_CACHE_PUT_COUNT, queryCachePutCount);
        types.put(OPERATION_QUERY_CACHE_PUT_COUNT, Long.class);
   }

    @Override
    public Collection<String> getDynamicChildrenNames(EntityManagerFactoryAccess entityManagerFactoryLookup, PathAddress pathAddress) {
        Set<String> result = new HashSet<>();
        org.hibernate.stat.Statistics stats = getBaseStatistics(entityManagerFactoryLookup.entityManagerFactory(pathAddress.getValue(HibernateStatistics.PROVIDER_LABEL)));
        if (stats != null) {
            String[] queries = stats.getQueries();
            if (queries != null) {
                for (String query : queries) {
                    result.add(QueryName.queryName(query).getDisplayName());
                }
            }
        }
        return result;
    }

    private org.hibernate.stat.Statistics getBaseStatistics(EntityManagerFactory entityManagerFactory) {
        if (entityManagerFactory == null) {
            return null;
        }
        HibernateEntityManagerFactory entityManagerFactoryImpl = (HibernateEntityManagerFactory) entityManagerFactory;
        SessionFactory sessionFactory = entityManagerFactoryImpl.getSessionFactory();
        if (sessionFactory != null) {
            return sessionFactory.getStatistics();
        }
        return null;
    }

    private org.hibernate.stat.QueryStatistics getStatistics(EntityManagerFactory entityManagerFactory, String displayQueryName) {
        if (entityManagerFactory == null) {
            return null;
        }
        HibernateEntityManagerFactory entityManagerFactoryImpl = (HibernateEntityManagerFactory) entityManagerFactory;
        SessionFactory sessionFactory = entityManagerFactoryImpl.getSessionFactory();
        // convert displayed (transformed by QueryNames) query name to original query name to look up query statistics
        if (sessionFactory != null) {
            String[] originalQueryNames = sessionFactory.getStatistics().getQueries();
            if (originalQueryNames != null) {
                for (String originalQueryName : originalQueryNames) {
                    if (QueryName.queryName(originalQueryName).getDisplayName().equals(displayQueryName)) {
                        return sessionFactory.getStatistics().getQueryStatistics(originalQueryName);
                    }
                }
            }
        }
        return null;
    }

    private Operation queryExecutionCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.QueryStatistics statistics = getStatistics(getEntityManagerFactory(args), getQueryName(args));
            return Long.valueOf(statistics != null ? statistics.getExecutionCount() : 0);
        }
    };

    private Operation queryExecutionMaximumTime = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.QueryStatistics statistics = getStatistics(getEntityManagerFactory(args), getQueryName(args));
            return Long.valueOf(statistics != null ? statistics.getExecutionMaxTime() : 0);
        }
    };

    private Operation queryExecutionRowCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.QueryStatistics statistics = getStatistics(getEntityManagerFactory(args), getQueryName(args));
            return Long.valueOf(statistics != null ? statistics.getExecutionRowCount() : 0);
        }
    };

    private Operation queryExecutionAverageTime = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.QueryStatistics statistics = getStatistics(getEntityManagerFactory(args), getQueryName(args));
            return Long.valueOf(statistics != null ? statistics.getExecutionAvgTime() : 0);
        }
    };

    private Operation queryExecutionMinimumTime = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.QueryStatistics statistics = getStatistics(getEntityManagerFactory(args), getQueryName(args));
            return Long.valueOf(statistics != null ? statistics.getExecutionMinTime() : 0);
        }
    };

    private Operation queryCacheHitCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.QueryStatistics statistics = getStatistics(getEntityManagerFactory(args), getQueryName(args));
            return Long.valueOf(statistics != null ? statistics.getCacheHitCount() : 0);
        }
    };

    private Operation queryCacheMissCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.QueryStatistics statistics = getStatistics(getEntityManagerFactory(args), getQueryName(args));
            return Long.valueOf(statistics != null ? statistics.getCacheMissCount() : 0);
        }
    };

    private Operation queryCachePutCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.QueryStatistics statistics = getStatistics(getEntityManagerFactory(args), getQueryName(args));
            return Long.valueOf(statistics != null ? statistics.getCachePutCount() : 0);
        }
    };

    private Operation showQueryName = new Operation() {
        @Override
        public Object invoke(Object... args) {
            String displayQueryName = getQueryName(args);
            EntityManagerFactory entityManagerFactory = getEntityManagerFactory(args);
            if (displayQueryName != null && entityManagerFactory != null) {
                HibernateEntityManagerFactory entityManagerFactoryImpl = (HibernateEntityManagerFactory) entityManagerFactory;
                SessionFactory sessionFactory = entityManagerFactoryImpl.getSessionFactory();
                // convert displayed (transformed by QueryNames) query name to original query name
                if (sessionFactory != null) {
                    String[] originalQueryNames = sessionFactory.getStatistics().getQueries();
                    if (originalQueryNames != null) {
                        for (String originalQueryName : originalQueryNames) {
                            if (QueryName.queryName(originalQueryName).getDisplayName().equals(displayQueryName)) {
                                return originalQueryName;
                            }
                        }
                    }
                }

            }
            return null;
        }
    };

    private String getQueryName(Object... args) {
        PathAddress pathAddress = getPathAddress(args);
        if (pathAddress != null) {
            return pathAddress.getValue(HibernateStatistics.QUERYCACHE);
        }
        return null;
    }
}
