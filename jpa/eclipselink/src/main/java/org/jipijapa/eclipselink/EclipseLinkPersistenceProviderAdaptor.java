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

package org.jipijapa.eclipselink;

import java.util.Map;


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
        if (!properties.containsKey(ECLIPSELINK_TARGET_SERVER)) {
            properties.put(ECLIPSELINK_TARGET_SERVER, WildFlyServerPlatform.class.getName());
            properties.put(ECLIPSELINK_ARCHIVE_FACTORY, JBossArchiveFactoryImpl.class.getName());
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

}
