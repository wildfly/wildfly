/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.eclipselink;

import java.util.Map;


import jakarta.enterprise.inject.spi.BeanManager;

import org.jipijapa.plugin.spi.JtaManager;
import org.jipijapa.plugin.spi.ManagementAdaptor;
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.jipijapa.plugin.spi.Platform;

public class EclipseLinkPersistenceProviderAdaptor implements
        PersistenceProviderAdaptor {

    public static final String
        ECLIPSELINK_TARGET_SERVER = "eclipselink.target-server",
        ECLIPSELINK_ARCHIVE_FACTORY = "eclipselink.archive.factory",
        ECLIPSELINK_LOGGING_LOGGER = "eclipselink.logging.logger";

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void addProviderProperties(Map properties, PersistenceUnitMetadata pu) {

        if (!pu.getProperties().containsKey(ECLIPSELINK_ARCHIVE_FACTORY)) {
            properties.put(ECLIPSELINK_ARCHIVE_FACTORY, JBossArchiveFactoryImpl.class.getName());
        }

        if (!pu.getProperties().containsKey(ECLIPSELINK_TARGET_SERVER)) {
            properties.put(ECLIPSELINK_TARGET_SERVER, WildFlyServerPlatform.class.getName());
        }

        if (!pu.getProperties().containsKey(ECLIPSELINK_LOGGING_LOGGER)) {
            properties.put(ECLIPSELINK_LOGGING_LOGGER, JBossLogger.class.getName());
        }
    }

    @Override
    public void injectJtaManager(JtaManager jtaManager) {
        // No action required, EclipseLink looks this up from JNDI
    }

    @Override
    public void injectPlatform(Platform platform) {

    }

    @Override
    public void addProviderDependencies(PersistenceUnitMetadata pu) {
        // No action required
    }

    @Override
    public void beforeCreateContainerEntityManagerFactory(
            PersistenceUnitMetadata pu) {
        // no action required
    }

    @Override
    public void afterCreateContainerEntityManagerFactory(
            PersistenceUnitMetadata pu) {
        // TODO: Force creation of metamodel here?
    }

    @Override
    public ManagementAdaptor getManagementAdaptor() {
        // no action required
        return null;
    }

    @Override
    public boolean doesScopedPersistenceUnitNameIdentifyCacheRegionName(PersistenceUnitMetadata pu) {
        return false;
    }

    @Override
    public void cleanup(PersistenceUnitMetadata pu) {
        // no action required
    }

    @Override
    public Object beanManagerLifeCycle(BeanManager beanManager) {
        return null;
    }

    @Override
    public void markPersistenceUnitAvailable(Object wrapperBeanManagerLifeCycle) {

    }

    @Override
    public void addClassFileTransformer(PersistenceUnitMetadata pu) {

    }


}
