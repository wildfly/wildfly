/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.jpa.hibernate.service;

import static org.hibernate.cfg.AvailableSettings.JAKARTA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;
import static org.jboss.as.jpa.hibernate.JpaLogger.JPA_LOGGER;

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

    private static final String HIBERNATE_REGION_FACTORY_CLASS = "hibernate.cache.region.factory_class";
    private static final String V53_INFINISPAN_REGION_FACTORY = "org.infinispan.hibernate.cache.v53.InfinispanRegionFactory";
    private static final String UNSPECIFIED = "UNSPECIFIED";
    private static final String NONE = "NONE";

    @Override
    protected RegionFactory resolveRegionFactory(Map configurationValues, ServiceRegistryImplementor registry) {
        final Object useSecondLevelCache = configurationValues.get(USE_SECOND_LEVEL_CACHE);
        final String jpaSharedCodeModeValue = configurationValues.get(JAKARTA_SHARED_CACHE_MODE) != null ? configurationValues.get(JAKARTA_SHARED_CACHE_MODE).toString() : UNSPECIFIED;
        final Object regionFactory = configurationValues.get(HIBERNATE_REGION_FACTORY_CLASS);

        // treat Hibernate 2lc as off, if not specified.
        // Note that Hibernate 2lc in 5.1.x, defaults to disabled, so this code is only needed in 5.3.x+.
        if(Boolean.parseBoolean((String)useSecondLevelCache)) {
            JPA_LOGGER.tracef("WildFlyCustomRegionFactoryInitiator#resolveRegionFactory using %s for 2lc, useSecondLevelCache=%s, jpaSharedCodeModeValue=%s, regionFactory=%s",
                    V53_INFINISPAN_REGION_FACTORY, useSecondLevelCache,jpaSharedCodeModeValue, regionFactory);
            configurationValues.put(HIBERNATE_REGION_FACTORY_CLASS, V53_INFINISPAN_REGION_FACTORY);
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
                    V53_INFINISPAN_REGION_FACTORY, useSecondLevelCache,jpaSharedCodeModeValue, regionFactory);
            configurationValues.put(HIBERNATE_REGION_FACTORY_CLASS, V53_INFINISPAN_REGION_FACTORY);
            return super.resolveRegionFactory(configurationValues, registry);
        }
    }
}
