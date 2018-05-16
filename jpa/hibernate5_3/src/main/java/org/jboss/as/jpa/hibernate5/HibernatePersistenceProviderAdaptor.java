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

package org.jboss.as.jpa.hibernate5;

import static org.jboss.as.jpa.hibernate5.JpaLogger.JPA_LOGGER;

import java.util.Map;
import java.util.Properties;

import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.SharedCacheMode;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.cfg.AvailableSettings;
import org.jboss.as.jpa.hibernate5.management.HibernateManagementAdaptor;
import org.jipijapa.cache.spi.Classification;
import org.jipijapa.event.impl.internal.Notification;
import org.jipijapa.plugin.spi.EntityManagerFactoryBuilder;
import org.jipijapa.plugin.spi.JtaManager;
import org.jipijapa.plugin.spi.ManagementAdaptor;
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.jipijapa.plugin.spi.Platform;
import org.jipijapa.plugin.spi.TwoPhaseBootstrapCapable;

/**
 * Implements the PersistenceProviderAdaptor for Hibernate
 *
 * @author Scott Marlow
 */
public class HibernatePersistenceProviderAdaptor implements PersistenceProviderAdaptor, TwoPhaseBootstrapCapable {

    public static final String NAMING_STRATEGY_JPA_COMPLIANT_IMPL = "org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl";
    private volatile Platform platform;
    private static final String SHARED_CACHE_MODE = "javax.persistence.sharedCache.mode";
    private static final String NONE = SharedCacheMode.NONE.name();
    private static final String HIBERNATE_EXTENDED_BEANMANAGER = "org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager";

    @Override
    public void injectJtaManager(JtaManager jtaManager) {
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
        putPropertyIfAbsent(pu, properties, AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true");
        putPropertyIfAbsent(pu, properties, AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED,"false");
        putPropertyIfAbsent(pu, properties, AvailableSettings.IMPLICIT_NAMING_STRATEGY, NAMING_STRATEGY_JPA_COMPLIANT_IMPL);
        putPropertyIfAbsent(pu, properties, AvailableSettings.SCANNER, HibernateArchiveScanner.class);
        properties.put(AvailableSettings.APP_CLASSLOADER, pu.getClassLoader());
        putPropertyIfAbsent(pu,properties, org.hibernate.ejb.AvailableSettings.ENTITY_MANAGER_FACTORY_NAME, pu.getScopedPersistenceUnitName());
        putPropertyIfAbsent(pu, properties, AvailableSettings.SESSION_FACTORY_NAME, pu.getScopedPersistenceUnitName());
        if (!pu.getProperties().containsKey(AvailableSettings.SESSION_FACTORY_NAME)) {
            putPropertyIfAbsent(pu, properties, AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, Boolean.FALSE);
        }
        // the following properties were added to Hibernate ORM 5.3, for JPA 2.2 spec compliance.
        putPropertyIfAbsent( pu, properties, AvailableSettings.PREFER_GENERATOR_NAME_AS_DEFAULT_SEQUENCE_NAME, true );
        putPropertyIfAbsent( pu, properties, AvailableSettings.JPA_TRANSACTION_COMPLIANCE, true );
        putPropertyIfAbsent( pu, properties, AvailableSettings.JPA_CLOSED_COMPLIANCE, true );
        putPropertyIfAbsent( pu, properties, AvailableSettings.JPA_QUERY_COMPLIANCE, true );
        putPropertyIfAbsent( pu, properties, AvailableSettings.JPA_LIST_COMPLIANCE, true );
        putPropertyIfAbsent( pu, properties, AvailableSettings.JPA_CACHING_COMPLIANCE, true );
        putPropertyIfAbsent( pu, properties, AvailableSettings.JPA_PROXY_COMPLIANCE, true );
        putPropertyIfAbsent( pu, properties, AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, false );
        putPropertyIfAbsent( pu, properties, AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, true);

        // Search hint
        putPropertyIfAbsent( pu, properties,"hibernate.search.index_uninverting_allowed", true);
    }

    @Override
    public void addProviderDependencies(PersistenceUnitMetadata pu) {
        final Properties properties = pu.getProperties();
        final String sharedCacheMode = properties.getProperty(SHARED_CACHE_MODE);

        if ( Classification.NONE.equals(platform.defaultCacheClassification())) {
            JPA_LOGGER.tracef("second level cache is not supported in platform, ignoring shared cache mode");
            pu.setSharedCacheMode(SharedCacheMode.NONE);
        }
        // check if 2lc is explicitly disabled which takes precedence over other settings
        boolean sharedCacheDisabled = SharedCacheMode.NONE.equals(pu.getSharedCacheMode())
                ||
                NONE.equals(sharedCacheMode);
        if (!sharedCacheDisabled &&
                (null == properties.getProperty(AvailableSettings.USE_SECOND_LEVEL_CACHE) ||
                        Boolean.parseBoolean(properties.getProperty(AvailableSettings.USE_SECOND_LEVEL_CACHE))))
                {
            HibernateSecondLevelCache.addSecondLevelCacheDependencies(pu.getProperties(), pu.getScopedPersistenceUnitName());
            JPA_LOGGER.tracef("second level cache enabled for %s", pu.getScopedPersistenceUnitName());
        } else {
            JPA_LOGGER.tracef("second level cache disabled for %s, pu %s property = %s, pu.getSharedCacheMode = %s",
                    pu.getScopedPersistenceUnitName(),
                    SHARED_CACHE_MODE,
                    sharedCacheMode,
                    pu.getSharedCacheMode().toString());
        }
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
     * @return false if a custom AvailableSettings.CACHE_REGION_PREFIX property is specified.
     *         true if the scoped persistence unit name is used to prefix cache entries.
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

        if( isHibernateExtendedBeanManagerSupported()) {
            return new HibernateExtendedBeanManager(beanManager);
        }
        // for ORM 5.0, return null to indicate that the org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager extension should not be used.
        return null;
    }

    @Override
    public void markPersistenceUnitAvailable(Object wrapperBeanManagerLifeCycle) {
        if(isHibernateExtendedBeanManagerSupported()) {
            HibernateExtendedBeanManager hibernateExtendedBeanManager = (HibernateExtendedBeanManager) wrapperBeanManagerLifeCycle;
            // notify Hibernate ORM ExtendedBeanManager extension that the entity listener(s) can now be registered.
            hibernateExtendedBeanManager.beanManagerIsAvailableForUse();
        }
    }

    /**
     * org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager is added to Hibernate 5.1 as an extension for delaying registration
     * of entity listeners until the CDI AfterDeploymentValidation event is triggered.
     * This allows entity listener classes to reference the (origin) persistence unit (WFLY-2387).
     *
     * return true for Hibernate ORM 5.1+, which should contain the ExtendedBeanManager contract
     */
    private boolean isHibernateExtendedBeanManagerSupported() {
        try {
            Class.forName(HIBERNATE_EXTENDED_BEANMANAGER);
            return true;
        } catch (ClassNotFoundException ignore) {
            return false;
        } catch (NoClassDefFoundError ignore) {
            return false;
        }

    }

    /* start of TwoPhaseBootstrapCapable methods */

    public EntityManagerFactoryBuilder getBootstrap(final PersistenceUnitInfo info, final Map map) {
        return new TwoPhaseBootstrapImpl(info, map);
    }

    /* end of TwoPhaseBootstrapCapable methods */
}

