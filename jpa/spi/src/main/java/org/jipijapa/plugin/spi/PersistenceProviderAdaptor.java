/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import java.util.Map;

import jakarta.enterprise.inject.spi.BeanManager;

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
     * Adds any provider specific properties
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

    void addClassFileTransformer(PersistenceUnitMetadata pu);
}

