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

    private volatile JtaManager jtaManager;
    private volatile Platform platform;
    private static final String SHARED_CACHE_MODE = "javax.persistence.sharedCache.mode";
    private static final String NONE = SharedCacheMode.NONE.name();

    @Override
    public void injectJtaManager(JtaManager jtaManager) {
        if (this.jtaManager != jtaManager) {
            this.jtaManager = jtaManager;
        }

        // TODO: resolve http://lists.jboss.org/pipermail/hibernate-dev/2013-July/010140.html which seems to be a
        // Hibernate service loader related leak and uncomment the following)
        // also remove the appServerJtaPlatform and stop setting AvailableSettings.JTA_PLATFORM
/*
        // specify JTA integration to use with Hibernate
        if (DefaultJtaPlatform.getDelegate() == null ||
                DefaultJtaPlatform.getDelegate().getJtaManager() != jtaManager) {
            synchronized (DefaultJtaPlatform.class) {
                if (DefaultJtaPlatform.getDelegate() == null ||
                    DefaultJtaPlatform.getDelegate().getJtaManager() != jtaManager) {
                    DefaultJtaPlatform.setDelegate(new JBossAppServerJtaPlatform(jtaManager));
                }
            }
        }
*/
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
//      putPropertyIfAbsent(pu, properties, Configuration.USE_NEW_ID_GENERATOR_MAPPINGS, "true");
        putPropertyIfAbsent(pu, properties, org.hibernate.ejb.AvailableSettings.SCANNER, HibernateArchiveScanner.class.getName());
        properties.put(AvailableSettings.APP_CLASSLOADER, pu.getClassLoader());
        putPropertyIfAbsent(pu, properties, AvailableSettings.JTA_PLATFORM,  new JBossAppServerJtaPlatform(jtaManager));
        putPropertyIfAbsent(pu,properties, org.hibernate.ejb.AvailableSettings.ENTITY_MANAGER_FACTORY_NAME, pu.getScopedPersistenceUnitName());
        putPropertyIfAbsent(pu, properties, AvailableSettings.SESSION_FACTORY_NAME, pu.getScopedPersistenceUnitName());
        if (!pu.getProperties().containsKey(AvailableSettings.SESSION_FACTORY_NAME)) {
            putPropertyIfAbsent(pu, properties, AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, Boolean.FALSE);
        }
    }

    @Override
    public void addProviderDependencies(PersistenceUnitMetadata pu) {
        final Properties properties = pu.getProperties();
        final String sharedCacheMode = properties.getProperty(SHARED_CACHE_MODE);

        if ( Classification.NONE.equals(platform.defaultCacheClassification())) {
            if (!SharedCacheMode.NONE.equals(pu.getSharedCacheMode())) {
                JPA_LOGGER.tracef("second level cache is not supported in platform, ignoring shared cache mode");
            }
            pu.setSharedCacheMode(SharedCacheMode.NONE);
        }
        // check if 2lc is explicitly disabled which takes precedence over other settings
        boolean sharedCacheDisabled = SharedCacheMode.NONE.equals(pu.getSharedCacheMode())
                ||
                NONE.equals(sharedCacheMode);

        if (!sharedCacheDisabled &&
                Boolean.parseBoolean(properties.getProperty(AvailableSettings.USE_SECOND_LEVEL_CACHE))
                ||
                (sharedCacheMode != null && (!NONE.equals(sharedCacheMode)))
                || (!SharedCacheMode.NONE.equals(pu.getSharedCacheMode()) && (!SharedCacheMode.UNSPECIFIED.equals(pu.getSharedCacheMode())))) {
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

    /* start of TwoPhaseBootstrapCapable methods */

    public EntityManagerFactoryBuilder getBootstrap(final PersistenceUnitInfo info, final Map map) {
        return new TwoPhaseBootstrapImpl(info, map);
    }

    /* end of TwoPhaseBootstrapCapable methods */
}

