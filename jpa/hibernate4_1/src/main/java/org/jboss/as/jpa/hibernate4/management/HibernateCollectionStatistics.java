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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.ejb.HibernateEntityManagerFactory;
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
        HibernateEntityManagerFactory entityManagerFactoryImpl = (HibernateEntityManagerFactory) entityManagerFactory;
        SessionFactory sessionFactory = entityManagerFactoryImpl.getSessionFactory();
        if (sessionFactory != null) {
            return sessionFactory.getStatistics();
        }
        return null;
    }

    private CollectionStatistics getStatistics(final EntityManagerFactory entityManagerFactory, String collectionName) {
        if (entityManagerFactory == null) {
            return null;
        }
        HibernateEntityManagerFactory entityManagerFactoryImpl = (HibernateEntityManagerFactory) entityManagerFactory;
        SessionFactory sessionFactory = entityManagerFactoryImpl.getSessionFactory();
        if (sessionFactory != null) {
            return sessionFactory.getStatistics().getCollectionStatistics(collectionName);
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
            CollectionStatistics statistics = getStatistics(getEntityManagerFactory(args), getStatisticName(args));
            return Long.valueOf(statistics != null ? statistics.getUpdateCount() : 0);
        }
    };

    private Operation collectionRemoveCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            CollectionStatistics statistics = getStatistics(getEntityManagerFactory(args), getStatisticName(args));
            return Long.valueOf(statistics != null ? statistics.getRemoveCount() : 0);
        }
    };

    private Operation collectionRecreatedCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            CollectionStatistics statistics = getStatistics(getEntityManagerFactory(args), getStatisticName(args));
            return Long.valueOf(statistics != null ? statistics.getRemoveCount() : 0);
        }
    };

    private Operation collectionLoadCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            CollectionStatistics statistics = getStatistics(getEntityManagerFactory(args), getStatisticName(args));
            return Long.valueOf(statistics != null ? statistics.getLoadCount() : 0);
        }
    };

    private Operation collectionFetchCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            CollectionStatistics statistics = getStatistics(getEntityManagerFactory(args), getStatisticName(args));
            return Long.valueOf(statistics != null ? statistics.getFetchCount() : 0);
        }
    };
}
