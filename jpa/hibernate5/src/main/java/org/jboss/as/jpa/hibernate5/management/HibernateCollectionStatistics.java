/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jpa.hibernate5.management;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernateEntityManagerFactory;
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
        return Collections.unmodifiableCollection(Arrays.asList(
                getBaseStatistics(entityManagerFactoryLookup.entityManagerFactory(pathAddress.getValue(HibernateStatistics.PROVIDER_LABEL))).getCollectionRoleNames()));
    }

    private org.hibernate.stat.Statistics getBaseStatistics(EntityManagerFactory entityManagerFactory) {
        HibernateEntityManagerFactory entityManagerFactoryImpl = (HibernateEntityManagerFactory) entityManagerFactory;
        SessionFactory sessionFactory = entityManagerFactoryImpl.getSessionFactory();
        if (sessionFactory != null) {
            return sessionFactory.getStatistics();
        }
        return null;
    }

    private CollectionStatistics getStatistics(final EntityManagerFactory entityManagerFactory, String collectionName) {
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
