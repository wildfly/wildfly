/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jpa.hibernate5.service;

import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;

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
    @Override
    protected RegionFactory resolveRegionFactory(Map configurationValues, ServiceRegistryImplementor registry) {
        Object use_second_level_cache = configurationValues.get(USE_SECOND_LEVEL_CACHE);
        Object jpa_shared_code_mode = configurationValues.get(JPA_SHARED_CACHE_MODE);

        // treat Hibernate 2lc as off, if not specified.
        // Note that Hibernate 2lc in 5.1.x, defaults to disabled, so this code is only needed in 5.3.x+.
        if(Boolean.parseBoolean((String)use_second_level_cache) ||
                (jpa_shared_code_mode != null || !"NONE".equals(jpa_shared_code_mode.toString()))) {
            configurationValues.put("hibernate.cache.region.factory_class", "org.infinispan.hibernate.cache.v53.InfinispanRegionFactory");
            return super.resolveRegionFactory(configurationValues, registry);
        }
        else {
            // explicitly disable 2lc cache
            return NoCachingRegionFactory.INSTANCE;
        }
    }
}
