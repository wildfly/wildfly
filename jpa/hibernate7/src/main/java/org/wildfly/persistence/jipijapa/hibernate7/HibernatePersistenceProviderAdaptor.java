/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.persistence.jipijapa.hibernate7;

import java.util.Map;
import java.util.Properties;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.hibernate.cfg.AvailableSettings;
import org.jipijapa.cache.spi.Classification;
import org.jipijapa.event.impl.internal.Notification;
import org.jipijapa.plugin.spi.EntityManagerFactoryBuilder;
import org.jipijapa.plugin.spi.JtaManager;
import org.jipijapa.plugin.spi.ManagementAdaptor;
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.jipijapa.plugin.spi.Platform;
import org.jipijapa.plugin.spi.TwoPhaseBootstrapCapable;
import org.kohsuke.MetaInfServices;
import org.wildfly.persistence.jipijapa.hibernate7.management.HibernateManagementAdaptor;
import org.wildfly.persistence.jipijapa.hibernate7.service.WildFlyCustomJtaPlatform;

/**
 * Implements the PersistenceProviderAdaptor for Hibernate
 *
 * @author Scott Marlow
 */
@MetaInfServices(PersistenceProviderAdaptor.class)
public class HibernatePersistenceProviderAdaptor implements PersistenceProviderAdaptor, TwoPhaseBootstrapCapable {

    public static final String NAMING_STRATEGY_JPA_COMPLIANT_IMPL = "org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl";
    private volatile Platform platform;
    private static final String SHARED_CACHE_MODE = "jakarta.persistence.sharedCache.mode";
    private static final String NONE = SharedCacheMode.NONE.name();
    private static final String UNSPECIFIED = SharedCacheMode.UNSPECIFIED.name();

    // Hibernate ORM 5.3 setting which if false, the old IdentifierGenerator were used for AUTO, TABLE and SEQUENCE id generation.
    // Hibernate ORM 6.0 does not support AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS
    private static final String USE_NEW_ID_GENERATOR_MAPPINGS = "hibernate.id.new_generator_mappings";

    @Override
    public void injectJtaManager(JtaManager jtaManager) {
        WildFlyCustomJtaPlatform.setTransactionSynchronizationRegistry(jtaManager.getSynchronizationRegistry());
    }

    @Override
    public void injectPlatform(Platform platform) {
        if (this.platform != platform) {
            this.platform = platform;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void addProviderProperties(Map properties, PersistenceUnitMetadata pu) {
        putPropertyIfAbsent(pu, properties, AvailableSettings.JPAQL_STRICT_COMPLIANCE, "true"); // JIPI-24 ignore jpql aliases case

        failOnIncompatibleSetting(pu, properties);  // fail application deployment if application sets hibernate.id.new_generator_mappings to false
        // applications that set new_generator_mappings to false need to be migrated to Hibernate ORM 6+,
        // the database may need changes as well to deal with ensuring the "next id" counter is represented
        // correctly in the database to match what the application is changed to instead use when inserting
        // new database table rows.

        putPropertyIfAbsent(pu, properties, AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, "false");
        putPropertyIfAbsent(pu, properties, AvailableSettings.IMPLICIT_NAMING_STRATEGY, NAMING_STRATEGY_JPA_COMPLIANT_IMPL);
        putPropertyIfAbsent(pu, properties, AvailableSettings.SCANNER, HibernateArchiveScanner.class);
        properties.put(AvailableSettings.CLASSLOADERS, pu.getClassLoader());
        // Only set SESSION_FACTORY_NAME_IS_JNDI to false if application didn't override Hibernate ORM session factory name.
        if (!pu.getProperties().containsKey(AvailableSettings.SESSION_FACTORY_NAME)) {
            putPropertyIfAbsent(pu, properties, AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, Boolean.FALSE);
        }
        putPropertyIfAbsent(pu, properties, AvailableSettings.SESSION_FACTORY_NAME, pu.getScopedPersistenceUnitName());

        putPropertyIfAbsent(pu, properties, AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, false);

        // Enable JPA Compliance mode
        putPropertyIfAbsent(pu, properties, AvailableSettings.JPA_COMPLIANCE, true);
    }

    private void failOnIncompatibleSetting(PersistenceUnitMetadata pu, Map properties) {
        if ("false".equals(pu.getProperties().getProperty(USE_NEW_ID_GENERATOR_MAPPINGS))) {
            throw JpaLogger.JPA_LOGGER.failOnIncompatibleSetting();
        }
    }

    @Override
    public void addProviderDependencies(PersistenceUnitMetadata pu) {
        final Properties properties = pu.getProperties();
        final String sharedCacheMode = properties.getProperty(SHARED_CACHE_MODE);

        if (Classification.NONE.equals(platform.defaultCacheClassification())) {
            JpaLogger.JPA_LOGGER.tracef("second level cache is not supported in platform, ignoring shared cache mode");
            pu.setSharedCacheMode(SharedCacheMode.NONE);
        }
        // precedence order of cache settings (1 overrides other settings and 3 is lowest precedence level):
        // 1 - SharedCacheMode.NONE
        // 2 - AvailableSettings.USE_SECOND_LEVEL_CACHE
        // 2 - AvailableSettings.USE_QUERY_CACHE
        // 3 - SharedCacheMode.UNSPECIFIED
        // 3 - SharedCacheMode.ENABLE_SELECTIVE
        // 3 - SharedCacheMode.DISABLE_SELECTIVE

        // if SharedCacheMode.NONE, set cacheDisabled to true.
        boolean cacheDisabled = noneCacheMode(pu)
                // Or if Hibernate cache settings are specified and Hibernate settings indicate cache is disabled, set cacheDisabled to true.
                || (haveHibernateCachePropertyDefined(pu) && !hibernateCacheEnabled(pu));

        if (!cacheDisabled) {
            HibernateSecondLevelCache.addSecondLevelCacheDependencies(pu.getProperties(), pu.getScopedPersistenceUnitName());
            JpaLogger.JPA_LOGGER.secondLevelCacheIsEnabled(pu.getScopedPersistenceUnitName());
            // for SharedCacheMode.UNSPECIFIED, enable the cache and enable caching for entities marked with Cacheable
            if (unspecifiedCacheMode(pu)) {
                pu.setSharedCacheMode(SharedCacheMode.ENABLE_SELECTIVE);
            }

        } else {
            JpaLogger.JPA_LOGGER.tracef("second level cache disabled for %s, pu %s property = %s, pu.getSharedCacheMode = %s",
                    pu.getScopedPersistenceUnitName(),
                    SHARED_CACHE_MODE,
                    sharedCacheMode,
                    pu.getSharedCacheMode().toString());
            pu.setSharedCacheMode(SharedCacheMode.NONE);  // ensure that Hibernate doesn't try to use the 2lc
        }
    }

    /**
     * Determine if Hibernate cache properties are specified.
     *
     * @param pu
     *
     * @return true if Hibernate cache setting are specified.
     */
    private boolean haveHibernateCachePropertyDefined(PersistenceUnitMetadata pu) {
        return (pu.getProperties().getProperty(AvailableSettings.USE_SECOND_LEVEL_CACHE) != null ||
                pu.getProperties().getProperty(AvailableSettings.USE_QUERY_CACHE) != null);
    }

    /**
     * Determine if Hibernate cache properties are enabling or disabling cache.
     *
     * @param pu
     *
     * @return true if cache enabled, false if cache disabled.
     */
    private boolean hibernateCacheEnabled(PersistenceUnitMetadata pu) {
        return (Boolean.parseBoolean(pu.getProperties().getProperty(AvailableSettings.USE_SECOND_LEVEL_CACHE))
                || Boolean.parseBoolean(pu.getProperties().getProperty(AvailableSettings.USE_QUERY_CACHE))
        );

    }

    private boolean unspecifiedCacheMode(PersistenceUnitMetadata pu) {
        return SharedCacheMode.UNSPECIFIED.equals(pu.getSharedCacheMode()) ||
                UNSPECIFIED.equals(pu.getProperties().getProperty(SHARED_CACHE_MODE));
    }

    private boolean noneCacheMode(PersistenceUnitMetadata pu) {
        return SharedCacheMode.NONE.equals(pu.getSharedCacheMode()) ||
                NONE.equals(pu.getProperties().getProperty(SHARED_CACHE_MODE));
    }

    private void putPropertyIfAbsent(PersistenceUnitMetadata pu, Map properties, String property, Object value) {
        if (!pu.getProperties().containsKey(property)) {
            properties.put(property, value);
        }
    }

    @Override
    public void beforeCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        Notification.beforeEntityManagerFactoryCreate(Classification.INFINISPAN, pu);
    }

    @Override
    public void afterCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        Notification.afterEntityManagerFactoryCreate(Classification.INFINISPAN, pu);
    }

    @Override
    public ManagementAdaptor getManagementAdaptor() {
        return HibernateManagementAdaptor.getInstance();
    }

    /**
     * determine if management console can display the second level cache entries
     *
     * @param pu
     *
     * @return false if a custom AvailableSettings.CACHE_REGION_PREFIX property is specified.
     * true if the scoped persistence unit name is used to prefix cache entries.
     */
    @Override
    public boolean doesScopedPersistenceUnitNameIdentifyCacheRegionName(PersistenceUnitMetadata pu) {
        String cacheRegionPrefix = pu.getProperties().getProperty(AvailableSettings.CACHE_REGION_PREFIX);

        return cacheRegionPrefix == null || cacheRegionPrefix.equals(pu.getScopedPersistenceUnitName());
    }

    @Override
    public void cleanup(PersistenceUnitMetadata pu) {

    }

    @Override
    public Object beanManagerLifeCycle(BeanManager beanManager) {
        return new HibernateExtendedBeanManager(beanManager);
    }

    @Override
    public void markPersistenceUnitAvailable(Object wrapperBeanManagerLifeCycle) {

        HibernateExtendedBeanManager hibernateExtendedBeanManager = (HibernateExtendedBeanManager) wrapperBeanManagerLifeCycle;
        // notify Hibernate ORM ExtendedBeanManager extension that the entity listener(s) can now be registered.
        hibernateExtendedBeanManager.beanManagerIsAvailableForUse();
    }

    /* start of TwoPhaseBootstrapCapable methods */

    public EntityManagerFactoryBuilder getBootstrap(final PersistenceUnitInfo info, final Map map) {
        return new TwoPhaseBootstrapImpl(info, map);
    }

    /* end of TwoPhaseBootstrapCapable methods */
}

