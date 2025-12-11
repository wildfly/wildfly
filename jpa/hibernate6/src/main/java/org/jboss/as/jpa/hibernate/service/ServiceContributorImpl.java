/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jpa.hibernate.service;

import static org.jboss.as.jpa.hibernate.HibernateSecondLevelCache.DEFAULT_REGION_FACTORY;
import static org.jboss.as.jpa.hibernate.JpaLogger.JPA_LOGGER;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.cfg.TransactionSettings;
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
    private static final String EHCACHE = "ehcache";

    @Override
    public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
        // note that the following deprecated getSettings() is agreed to be replaced with method that returns immutable copy of configuration settings.
        final Object jtaPlatformInitiatorEnabled = serviceRegistryBuilder.getSettings().getOrDefault(CONTROLJTAINTEGRATION, true);

        if (serviceRegistryBuilder.getSettings().get(TransactionSettings.JTA_PLATFORM) != null) {
            // applications that already specify the transaction platform property which will override the WildFlyCustomJtaPlatform.
            JPA_LOGGER.tracef("ServiceContributorImpl#contribute application configured the Jakarta Transactions Platform to be used instead of WildFlyCustomJtaPlatform (%s=%s)",
                    TransactionSettings.JTA_PLATFORM, serviceRegistryBuilder.getSettings().get(TransactionSettings.JTA_PLATFORM));
        } else if (jtaPlatformInitiatorEnabled == null ||
                (jtaPlatformInitiatorEnabled instanceof Boolean && ((Boolean) jtaPlatformInitiatorEnabled).booleanValue()) ||
                Boolean.parseBoolean(jtaPlatformInitiatorEnabled.toString())) {
            // use WildFlyCustomJtaPlatform unless they explicitly set wildfly.jpa.jtaplatform to false.
            JPA_LOGGER.tracef("ServiceContributorImpl#contribute application will use WildFlyCustomJtaPlatform");
            serviceRegistryBuilder.addInitiator(new WildFlyCustomJtaPlatformInitiator());
        }

        final Object regionFactoryInitiatorEnabled = serviceRegistryBuilder.getSettings().getOrDefault(CONTROL2LCINTEGRATION, true);
        final Object regionFactory = serviceRegistryBuilder.getSettings().get(CacheSettings.CACHE_REGION_FACTORY);
        if ((regionFactory instanceof String)) {
            String cacheRegionFactory = (String) regionFactory;
            if (cacheRegionFactory.contains(EHCACHE)) {
                JPA_LOGGER.tracef("ServiceContributorImpl#contribute application is using Ehcache via %s=%s",
                        CacheSettings.CACHE_REGION_FACTORY, cacheRegionFactory);
                return;
            } else if (!DEFAULT_REGION_FACTORY.equals(cacheRegionFactory)) {
                // warn and ignore application cache region setting
                JPA_LOGGER.ignoredCacheRegionSetting(CacheSettings.CACHE_REGION_FACTORY, cacheRegionFactory);
            }
        }
        if (regionFactoryInitiatorEnabled == null ||
                (regionFactoryInitiatorEnabled instanceof Boolean && ((Boolean) regionFactoryInitiatorEnabled).booleanValue()) ||
                Boolean.parseBoolean(regionFactoryInitiatorEnabled.toString())) {
            JPA_LOGGER.tracef("ServiceContributorImpl#contribute adding ORM initiator for 2lc region factory");
            serviceRegistryBuilder.addInitiator(new WildFlyCustomRegionFactoryInitiator());
        }
    }
}
