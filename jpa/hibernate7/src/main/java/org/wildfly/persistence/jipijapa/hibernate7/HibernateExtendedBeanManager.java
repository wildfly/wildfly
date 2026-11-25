/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.persistence.jipijapa.hibernate7;

import java.util.ArrayList;

import jakarta.enterprise.inject.spi.BeanManager;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;

/**
 * HibernateExtendedBeanManager helps defer the registering of entity listeners, with the Jakarta Contexts and Dependency Injection BeanManager until
 * after the persistence unit is available for lookup by Jakarta Contexts and Dependency Injection bean(s).
 * This solves the WFLY-2387 issue of Jakarta Persistence entity listeners referencing the Jakarta Contexts and Dependency Injection bean, when the bean cycles back
 * to the persistence unit, or a different persistence unit.
 *
 * @author Scott Marlow
 */
public class HibernateExtendedBeanManager implements ExtendedBeanManager {
    private final BeanManager beanManager;
    private final ArrayList<LifecycleListener> lifecycleListeners = new ArrayList<>();

    public HibernateExtendedBeanManager(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    /**
     * Hibernate calls registerLifecycleListener to register callback to be notified
     * when the Jakarta Contexts and Dependency Injection BeanManager can safely be used.
     * The Jakarta Contexts and Dependency Injection BeanManager can safely be used
     * when the Jakarta Contexts and Dependency Injection AfterDeploymentValidation event is reached.
     * <p>
     * Note: Caller (BeanManagerAfterDeploymentValidation) is expected to synchronize calls to
     * registerLifecycleListener() + beanManagerIsAvailableForUse(), which protects
     * HibernateExtendedBeanManager.lifecycleListeners from being read/written from multiple concurrent threads.
     * There are many writer threads (one per deployed persistence unit) and one reader/writer thread expected
     * to be triggered by one AfterDeploymentValidation event per deployment.
     */
    public void beanManagerIsAvailableForUse() {
        if (lifecycleListeners.isEmpty()) {
            throw JpaLogger.JPA_LOGGER.HibernateORMDidNotRegisterLifeCycleListener();
        }
        for (LifecycleListener hibernateCallback : lifecycleListeners) {
            hibernateCallback.beanManagerInitialized(beanManager);
        }
    }

    @Override
    public void registerLifecycleListener(LifecycleListener lifecycleListener) {
        lifecycleListeners.add(lifecycleListener);
    }
}
