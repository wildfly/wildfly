/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.persistence.jipijapa.hibernate7.service;

import static org.hibernate.cfg.AvailableSettings.CACHE_REGION_FACTORY;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;
import static org.wildfly.persistence.jipijapa.hibernate7.JpaLogger.JPA_LOGGER;

import java.util.Map;

import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cache.internal.RegionFactoryInitiator;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 * @author Scott Marlow
 */
public class WildFlyCustomRegionFactoryInitiator extends RegionFactoryInitiator {

    private static final String INFINISPAN_REGION_FACTORY = "org.infinispan.hibernate.cache.v62.InfinispanRegionFactory";
    private static final String UNSPECIFIED = "UNSPECIFIED";
    private static final String NONE = "NONE";

    @Override
    protected RegionFactory resolveRegionFactory(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
        final Object useSecondLevelCache = configurationValues.get(USE_SECOND_LEVEL_CACHE);
        final String jpaSharedCodeModeValue = configurationValues.get(JAKARTA_SHARED_CACHE_MODE) != null ? configurationValues.get(JAKARTA_SHARED_CACHE_MODE).toString() : UNSPECIFIED;
        final Object regionFactory = configurationValues.get(CACHE_REGION_FACTORY);

        // treat Hibernate 2lc as off, if not specified.
        // Note that Hibernate 2lc in 5.1.x, defaults to disabled, so this code is only needed in 5.3.x+.
        if(Boolean.parseBoolean((String)useSecondLevelCache)) {
            JPA_LOGGER.tracef("WildFlyCustomRegionFactoryInitiator#resolveRegionFactory using %s for 2lc, useSecondLevelCache=%s, jpaSharedCodeModeValue=%s, regionFactory=%s",
                    INFINISPAN_REGION_FACTORY, useSecondLevelCache,jpaSharedCodeModeValue, regionFactory);
            configurationValues.put(CACHE_REGION_FACTORY, INFINISPAN_REGION_FACTORY);
            return super.resolveRegionFactory(configurationValues, registry);
        } else if(UNSPECIFIED.equals(jpaSharedCodeModeValue)
             || NONE.equals(jpaSharedCodeModeValue)) {
            // explicitly disable 2lc cache
            JPA_LOGGER.tracef("WildFlyCustomRegionFactoryInitiator#resolveRegionFactory not using a 2lc, useSecondLevelCache=%s, jpaSharedCodeModeValue=%s, regionFactory=%s",
                    useSecondLevelCache,jpaSharedCodeModeValue, regionFactory);
            return NoCachingRegionFactory.INSTANCE;
        }
        else {
            JPA_LOGGER.tracef("WildFlyCustomRegionFactoryInitiator#resolveRegionFactory using %s for 2lc, useSecondLevelCache=%s, jpaSharedCodeModeValue=%s, regionFactory=%s",
                    INFINISPAN_REGION_FACTORY, useSecondLevelCache,jpaSharedCodeModeValue, regionFactory);
            configurationValues.put(CACHE_REGION_FACTORY, INFINISPAN_REGION_FACTORY);
            return super.resolveRegionFactory(configurationValues, registry);
        }
    }
}
