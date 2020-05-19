/*
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2019, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.jpa.mockprovider.skipquerydetach;

import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;

import org.jipijapa.plugin.spi.JtaManager;
import org.jipijapa.plugin.spi.ManagementAdaptor;
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.jipijapa.plugin.spi.Platform;

/**
 * persistence provider adaptor test class
 *
 * @author Scott Marlow
 */
public class TestAdapter implements PersistenceProviderAdaptor {

    private static volatile boolean initialized = false;

    @Override
    public void injectJtaManager(JtaManager jtaManager) {

    }

    @Override
    public void injectPlatform(Platform platform) {

    }

    @Override
    public void addProviderProperties(Map properties, PersistenceUnitMetadata pu) {

    }

    @Override
    public void addProviderDependencies(PersistenceUnitMetadata pu) {

    }

    @Override
    public void beforeCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        initialized = true;
    }

    @Override
    public void afterCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {

    }

    @Override
    public ManagementAdaptor getManagementAdaptor() {
        return null;
    }

    @Override
    public boolean doesScopedPersistenceUnitNameIdentifyCacheRegionName(PersistenceUnitMetadata pu) {
        return false;
    }

    @Override
    public void cleanup(PersistenceUnitMetadata pu) {

    }

    @Override
    public Object beanManagerLifeCycle(BeanManager beanManager) {
        return null;
    }

    @Override
    public void markPersistenceUnitAvailable(Object wrapperBeanManagerLifeCycle) {

    }

    public static boolean wasInitialized() {
        return initialized;
    }

    public static void clearInitialized() {
        initialized = false;
    }
}
