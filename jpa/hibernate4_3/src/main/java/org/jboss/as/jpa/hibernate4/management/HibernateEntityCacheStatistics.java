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
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.jipijapa.management.spi.EntityManagerFactoryAccess;
import org.jipijapa.management.spi.Operation;
import org.jipijapa.management.spi.PathAddress;

/**
 * Hibernate entity cache (SecondLevelCacheStatistics) statistics
 *
 * @author Scott Marlow
 */
public class HibernateEntityCacheStatistics extends HibernateAbstractStatistics {

    public static final String ATTRIBUTE_ENTITY_CACHE_REGION_NAME = "entity-cache-region-name";
    public static final String OPERATION_SECOND_LEVEL_CACHE_HIT_COUNT = "second-level-cache-hit-count";
    public static final String OPERATION_SECOND_LEVEL_CACHE_MISS_COUNT = "second-level-cache-miss-count";
    public static final String OPERATION_SECOND_LEVEL_CACHE_PUT_COUNT = "second-level-cache-put-count";
    public static final String OPERATION_SECOND_LEVEL_CACHE_COUNT_IN_MEMORY = "second-level-cache-count-in-memory";
    public static final String OPERATION_SECOND_LEVEL_CACHE_SIZE_IN_MEMORY = "second-level-cache-size-in-memory";

    public HibernateEntityCacheStatistics() {
        /**
         * specify the different operations
         */
        operations.put(ATTRIBUTE_ENTITY_CACHE_REGION_NAME, getEntityCacheRegionName);
        types.put(ATTRIBUTE_ENTITY_CACHE_REGION_NAME,String.class);

        operations.put(OPERATION_SECOND_LEVEL_CACHE_HIT_COUNT, entityCacheHitCount);
        types.put(OPERATION_SECOND_LEVEL_CACHE_HIT_COUNT, Long.class);

        operations.put(OPERATION_SECOND_LEVEL_CACHE_MISS_COUNT, entityCacheMissCount);
        types.put(OPERATION_SECOND_LEVEL_CACHE_MISS_COUNT, Long.class);

        operations.put(OPERATION_SECOND_LEVEL_CACHE_PUT_COUNT, entityCachePutCount);
        types.put(OPERATION_SECOND_LEVEL_CACHE_PUT_COUNT, Long.class);

        operations.put(OPERATION_SECOND_LEVEL_CACHE_COUNT_IN_MEMORY, entityCacheCountInMemory);
        types.put(OPERATION_SECOND_LEVEL_CACHE_COUNT_IN_MEMORY, Long.class);

        operations.put(OPERATION_SECOND_LEVEL_CACHE_SIZE_IN_MEMORY, entityCacheSizeInMemory);
        types.put(OPERATION_SECOND_LEVEL_CACHE_SIZE_IN_MEMORY, Long.class);

    }

    @Override
    public Collection<String> getDynamicChildrenNames(EntityManagerFactoryAccess entityManagerFactoryLookup, PathAddress pathAddress) {
        org.hibernate.stat.Statistics stats = getBaseStatistics(entityManagerFactoryLookup.entityManagerFactory(pathAddress.getValue(HibernateStatistics.PROVIDER_LABEL)));
        if (stats == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(Arrays.asList(stats.getEntityNames()));
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

    org.hibernate.stat.SecondLevelCacheStatistics getStatistics(EntityManagerFactoryAccess entityManagerFactoryaccess, PathAddress pathAddress) {
        String scopedPersistenceUnitName = pathAddress.getValue(HibernateStatistics.PROVIDER_LABEL);
        HibernateEntityManagerFactory entityManagerFactoryImpl = (HibernateEntityManagerFactory) entityManagerFactoryaccess.entityManagerFactory(scopedPersistenceUnitName);
        if (entityManagerFactoryImpl == null) {
            return null;
        }
        SessionFactory sessionFactory = entityManagerFactoryImpl.getSessionFactory();
        if (sessionFactory != null) {
            // The entity class name is prefixed by the application scoped persistence unit name

            return sessionFactory.getStatistics().getSecondLevelCacheStatistics(scopedPersistenceUnitName + "." +
                    pathAddress.getValue(HibernateStatistics.ENTITYCACHE));
        }
        return null;
    }
    private Operation getEntityCacheRegionName = new Operation() {
        @Override
        public Object invoke(Object... args) {
            return getStatisticName(args);
        }
    };


    private Operation entityCacheHitCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.SecondLevelCacheStatistics statistics = getStatistics(getEntityManagerFactoryAccess(args),  getPathAddress(args));
            return Long.valueOf(statistics != null ? statistics.getHitCount() : 0);
        }
    };

    private Operation entityCacheMissCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.SecondLevelCacheStatistics statistics = getStatistics(getEntityManagerFactoryAccess(args),  getPathAddress(args));
            return Long.valueOf(statistics != null ? statistics.getMissCount() : 0);
        }
    };

    private Operation entityCachePutCount = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.SecondLevelCacheStatistics statistics = getStatistics(getEntityManagerFactoryAccess(args),  getPathAddress(args));
            return Long.valueOf(statistics != null ? statistics.getPutCount() : 0);
        }
    };

    private Operation entityCacheSizeInMemory = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.SecondLevelCacheStatistics statistics = getStatistics(getEntityManagerFactoryAccess(args),  getPathAddress(args));
            return Long.valueOf(statistics != null ? statistics.getSizeInMemory() : 0);
        }
    };

    private Operation entityCacheCountInMemory = new Operation() {
        @Override
        public Object invoke(Object... args) {
            org.hibernate.stat.SecondLevelCacheStatistics statistics = getStatistics(getEntityManagerFactoryAccess(args),  getPathAddress(args));
            return Long.valueOf(statistics != null ? statistics.getElementCountInMemory() : 0);
        }
    };

}
