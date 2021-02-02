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

import static org.jboss.as.jpa.hibernate5.JpaLogger.JPA_LOGGER;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Contribute specialized Hibernate Service impls
 *
 * @author Steve Ebersole
 * @author Scott Marlow
 */
public class ServiceContributorImpl implements ServiceContributor {
    private static final String CONTROLJTAINTEGRATION = "wildfly.jpa.jtaplatform"; // these properties are documented in org.jboss.as.jpa.config.Configuration
    private static final String CONTROL2LCINTEGRATION = "wildfly.jpa.regionfactory";
    private static final String TRANSACTION_PLATFORM = "hibernate.transaction.jta.platform";
    private static final String EHCACHE = "ehcache";
    private static final String HIBERNATE_REGION_FACTORY_CLASS = "hibernate.cache.region.factory_class";

    @Override
    public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
        // note that the following deprecated getSettings() is agreed to be replaced with method that returns immutable copy of configuration settings.
        final Object jtaPlatformInitiatorEnabled = serviceRegistryBuilder.getSettings().getOrDefault(CONTROLJTAINTEGRATION, true);

        if (serviceRegistryBuilder.getSettings().get(TRANSACTION_PLATFORM) != null) {
            // applications that already specify the transaction platform property which will override the WildFlyCustomJtaPlatform.
            JPA_LOGGER.tracef("ServiceContributorImpl#contribute application configured the Jakarta Transactions Platform to be used instead of WildFlyCustomJtaPlatform (%s=%s)",
                    TRANSACTION_PLATFORM, serviceRegistryBuilder.getSettings().get(TRANSACTION_PLATFORM));
        } else if (jtaPlatformInitiatorEnabled == null ||
                (jtaPlatformInitiatorEnabled instanceof Boolean && ((Boolean) jtaPlatformInitiatorEnabled).booleanValue()) ||
                Boolean.parseBoolean(jtaPlatformInitiatorEnabled.toString())) {
            // use WildFlyCustomJtaPlatform unless they explicitly set wildfly.jpa.jtaplatform to false.
            JPA_LOGGER.tracef("ServiceContributorImpl#contribute application will use WildFlyCustomJtaPlatform");
            serviceRegistryBuilder.addInitiator(new WildFlyCustomJtaPlatformInitiator());
        }

        final Object regionFactoryInitiatorEnabled = serviceRegistryBuilder.getSettings().getOrDefault(CONTROL2LCINTEGRATION, true);
        final Object regionFactory = serviceRegistryBuilder.getSettings().get(HIBERNATE_REGION_FACTORY_CLASS);
        if ((regionFactory instanceof String) && (((String) regionFactory)).contains(EHCACHE)) {
            JPA_LOGGER.tracef("ServiceContributorImpl#contribute application is using Ehcache via regionFactory=%s",
                    regionFactory);
        } else if (regionFactoryInitiatorEnabled == null ||
                (regionFactoryInitiatorEnabled instanceof Boolean && ((Boolean) regionFactoryInitiatorEnabled).booleanValue()) ||
                Boolean.parseBoolean(regionFactoryInitiatorEnabled.toString())) {
            JPA_LOGGER.tracef("ServiceContributorImpl#contribute adding ORM initiator for 2lc region factory");
            serviceRegistryBuilder.addInitiator(new WildFlyCustomRegionFactoryInitiator());
        }
    }
}
