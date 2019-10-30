/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jipijapa.eclipselink;

import java.util.Map;


import javax.enterprise.inject.spi.BeanManager;

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

}
