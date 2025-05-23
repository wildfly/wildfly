/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.persistence.jipijapa.hibernate7.management;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.CollectionStatistics;
import org.jipijapa.management.spi.EntityManagerFactoryAccess;
import org.jipijapa.management.spi.Operation;
import org.jipijapa.management.spi.PathAddress;

/**
 * Hibernate collection statistics
 *
 * @author Scott Marlow
 */
public class HibernateCollectionStatistics extends HibernateAbstractStatistics {

    private static final String ATTRIBUTE_COLLECTION_NAME = "collection-name";
    public static final String OPERATION_COLLECTION_LOAD_COUNT = "collection-load-count";
    public static final String OPERATION_COLLECTION_FETCH_COUNT = "collection-fetch-count";
    public static final String OPERATION_COLLECTION_UPDATE_COUNT = "collection-update-count";
    public static final String OPERATION_COLLECTION_REMOVE_COUNT = "collection-remove-count";
    public static final String OPERATION_COLLECTION_RECREATED_COUNT = "collection-recreated-count";

    public HibernateCollectionStatistics() {
        /**
         * specify the different operations
         */
        operations.put(ATTRIBUTE_COLLECTION_NAME, showCollectionName);
        types.put(ATTRIBUTE_COLLECTION_NAME,String.class);

        operations.put(OPERATION_COLLECTION_LOAD_COUNT, collectionLoadCount);
        types.put(OPERATION_COLLECTION_LOAD_COUNT, Long.class);

        operations.put(OPERATION_COLLECTION_FETCH_COUNT, collectionFetchCount);
        types.put(OPERATION_COLLECTION_FETCH_COUNT, Long.class);

        operations.put(OPERATION_COLLECTION_UPDATE_COUNT, collectionUpdateCount);
        types.put(OPERATION_COLLECTION_UPDATE_COUNT, Long.class);

        operations.put(OPERATION_COLLECTION_REMOVE_COUNT, collectionRemoveCount);
        types.put(OPERATION_COLLECTION_REMOVE_COUNT, Long.class);

        operations.put(OPERATION_COLLECTION_RECREATED_COUNT, collectionRecreatedCount);
        types.put(OPERATION_COLLECTION_RECREATED_COUNT, Long.class);
    }

    @Override
    public Collection<String> getDynamicChildrenNames(EntityManagerFactoryAccess entityManagerFactoryLookup, PathAddress pathAddress) {
        org.hibernate.stat.Statistics stats = getBaseStatistics(entityManagerFactoryLookup.entityManagerFactory(pathAddress.getValue(HibernateStatistics.PROVIDER_LABEL)));
        if (stats == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(Arrays.asList(stats.getCollectionRoleNames()));
    }

    private org.hibernate.stat.Statistics getBaseStatistics(EntityManagerFactory entityManagerFactory) {
        if (entityManagerFactory == null) {
            return null;
        }
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        if (sessionFactory != null) {
            return sessionFactory.getStatistics();
        }
        return null;
    }

    private CollectionStatistics getStatistics(final EntityManagerFactory entityManagerFactory, PathAddress pathAddress) {
        if (entityManagerFactory == null) {
            return null;
        }
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        if (sessionFactory != null) {
            return sessionFactory.getStatistics().getCollectionStatistics(pathAddress.getValue(HibernateStatistics.COLLECTION));
        }
        return null;
    }

    private Operation showCollectionName = new Operation() {
        @Override
        public Object invoke(Object... args) {
            return getStatisticName(args);
        }
    };

    private Operation collectionUpdateCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            CollectionStatistics statistics = getStatistics(getEntityManagerFactory(args), getPathAddress(args));
            return Long.valueOf(statistics != null ? statistics.getUpdateCount() : 0);
        }
    };

    private Operation collectionRemoveCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            CollectionStatistics statistics = getStatistics(getEntityManagerFactory(args), getPathAddress(args));
            return Long.valueOf(statistics != null ? statistics.getRemoveCount() : 0);
        }
    };

    private Operation collectionRecreatedCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            CollectionStatistics statistics = getStatistics(getEntityManagerFactory(args), getPathAddress(args));
            return Long.valueOf(statistics != null ? statistics.getRemoveCount() : 0);
        }
    };

    private Operation collectionLoadCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            CollectionStatistics statistics = getStatistics(getEntityManagerFactory(args), getPathAddress(args));
            return Long.valueOf(statistics != null ? statistics.getLoadCount() : 0);
        }
    };

    private Operation collectionFetchCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            CollectionStatistics statistics = getStatistics(getEntityManagerFactory(args), getPathAddress(args));
            return Long.valueOf(statistics != null ? statistics.getFetchCount() : 0);
        }
    };
}
