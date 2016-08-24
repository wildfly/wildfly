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

package org.jipijapa.plugin.spi;

import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;

/**
 * PersistenceProvider adaptor
 *
 * @author Scott Marlow
 */
public interface PersistenceProviderAdaptor {

    /**
     * pass the JtaManager in for internal use by PersistenceProviderAdaptor implementer
     *
     * @param jtaManager
     */
    void injectJtaManager(JtaManager jtaManager);

    /**
     * pass the platform in use
     * @param platform
     */
    void injectPlatform(Platform platform);

    /**
     * Adds any provider specific properties (e.g. hibernate.transaction.manager_lookup_class)
     *
     * @param properties
     * @param pu
     */
    void addProviderProperties(Map properties, PersistenceUnitMetadata pu);

    /**
     * Persistence provider integration code might need dependencies that must be started
     * for the deployment.  Note that these dependency classes are expected to be already available to the provider.
     *
     * @param pu
     * @return
     */
    void addProviderDependencies(PersistenceUnitMetadata pu);

    /**
     * Called right before persistence provider is invoked to create container entity manager factory.
     * afterCreateContainerEntityManagerFactory() will always be called after the container entity manager factory
     * is created.
     *
     * @param pu
     */
    void beforeCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu);

    /**
     * Called right after persistence provider is invoked to create container entity manager factory.
     */
    void afterCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu);

    /**
     * Get the management adaptor
     *
     * @return ManagementAdaptor or null
     */
    ManagementAdaptor getManagementAdaptor();

    /**
     * for adapters that support getManagementAdaptor(), does the scoped persistence unit name
     * correctly identify cache entities.  This is intended for Hibernate, other adapters can return true.
     *
     * @return the Hibernate adapter will return false if
     * the persistence unit has specified a custom "hibernate.cache.region_prefix" property.  True otherwise.
     *
     */
    boolean doesScopedPersistenceUnitNameIdentifyCacheRegionName(PersistenceUnitMetadata pu);

    /**
     * Called when we are done with the persistence unit metadata
     */
    void cleanup(PersistenceUnitMetadata pu);

    /**
     * Some persistence provider adapters may handle life cycle notification services for when the CDI bean manager
     * can lookup the persistence unit that is using the CDI bean manager (e.g. for handling self referencing cycles).
     * <p>
     * persistence provider BeanManager extension.
     *
     * @param beanManager
     * @return wrapper object representing BeanManager lifecycle
     */
    Object beanManagerLifeCycle(BeanManager beanManager);

    void markPersistenceUnitAvailable(Object wrapperBeanManagerLifeCycle);
}

