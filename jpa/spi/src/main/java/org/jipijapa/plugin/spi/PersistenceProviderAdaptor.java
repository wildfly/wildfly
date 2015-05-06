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

package org.jipijapa.plugin.spi;

import java.util.Map;

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
}

