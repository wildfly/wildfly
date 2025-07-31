/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.persistence.jipijapa.hibernate7.service;

import static org.wildfly.persistence.jipijapa.hibernate7.JpaLogger.JPA_LOGGER;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.internal.util.cache.InternalCacheFactory;
import org.hibernate.service.spi.ServiceContributor;
import org.kohsuke.MetaInfServices;

/**
 * Contribute specialized Hibernate Service impls
 *
 * @author Steve Ebersole
 * @author Scott Marlow
 */
@MetaInfServices
public class ServiceContributorImpl implements ServiceContributor {
    private static final String CONTROLJTAINTEGRATION = "wildfly.jpa.jtaplatform"; // these properties are documented in org.jboss.as.jpa.config.Configuration
    private static final String CONTROL2LCINTEGRATION = "wildfly.jpa.regionfactory";
    private static final String TRANSACTION_PLATFORM = "hibernate.transaction.jta.platform";
    private static final String EHCACHE = "ehcache";
    private static final String HIBERNATE_REGION_FACTORY_CLASS = "hibernate.cache.region.factory_class";
    private static final String DEFAULT_REGION_FACTORY = "org.infinispan.hibernate.cache.v62.InfinispanRegionFactory";

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
        if ((regionFactory instanceof String)) {
            String cacheRegionFactory = (String) regionFactory;
            if (cacheRegionFactory.contains(EHCACHE)) {
                JPA_LOGGER.tracef("ServiceContributorImpl#contribute application is using Ehcache via %s=%s",
                        HIBERNATE_REGION_FACTORY_CLASS, cacheRegionFactory);
                return;
            } else if (!DEFAULT_REGION_FACTORY.equals(cacheRegionFactory)) {
                // warn and ignore application cache region setting
                JPA_LOGGER.ignoredCacheRegionSetting(HIBERNATE_REGION_FACTORY_CLASS, cacheRegionFactory);
            }
        }
        if (regionFactoryInitiatorEnabled == null ||
                (regionFactoryInitiatorEnabled instanceof Boolean && ((Boolean) regionFactoryInitiatorEnabled).booleanValue()) ||
                Boolean.parseBoolean(regionFactoryInitiatorEnabled.toString())) {
            JPA_LOGGER.tracef("ServiceContributorImpl#contribute adding ORM initiator for 2lc region factory");
            serviceRegistryBuilder.addInitiator(new WildFlyCustomRegionFactoryInitiator());
        }

        //Now customize the Hibernate "internal cache": this is a component used for internal needs
        //(it's NOT the second level cache). ORM integrators such as WildFly are encouraged to inject
        //a better implementation than the one included by default.
        //WildFly happens to already pack Caffeine, which is an excellent choice so that's what it will use.
        //This setting is not user configurable.
        serviceRegistryBuilder.addService(InternalCacheFactory.class, new WildFlyCustomInternalCacheFactory());
    }

}
