/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.beanmanager;

import java.util.ArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;

import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;

/**
 * BeanManagerAfterDeploymentValidation
 *
 * @author Scott Marlow
 */
public class BeanManagerAfterDeploymentValidation implements Extension {

    public BeanManagerAfterDeploymentValidation(boolean afterDeploymentValidation) {
        this.afterDeploymentValidation = afterDeploymentValidation;
    }

    public BeanManagerAfterDeploymentValidation() {
    }

    void afterDeploymentValidation(@Observes AfterDeploymentValidation event, BeanManager manager) {
        markPersistenceUnitAvailable();
    }

    private boolean afterDeploymentValidation = false;
    private final ArrayList<DeferredCall> deferredCalls = new ArrayList();

    public synchronized void register(final PersistenceProviderAdaptor persistenceProviderAdaptor, final Object wrapperBeanManagerLifeCycle) {
        if (afterDeploymentValidation) {
            persistenceProviderAdaptor.markPersistenceUnitAvailable(wrapperBeanManagerLifeCycle);
        } else {
            deferredCalls.add(new DeferredCall(persistenceProviderAdaptor, wrapperBeanManagerLifeCycle));
        }
    }

    public synchronized void markPersistenceUnitAvailable() {
        afterDeploymentValidation = true;
        for(DeferredCall deferredCall: deferredCalls) {
            deferredCall.markPersistenceUnitAvailable();
        }
        deferredCalls.clear();
    }

    private static class DeferredCall {
        private final PersistenceProviderAdaptor persistenceProviderAdaptor;
        private final Object wrapperBeanManagerLifeCycle;

        DeferredCall(final PersistenceProviderAdaptor persistenceProviderAdaptor, final Object wrapperBeanManagerLifeCycle) {
            this.persistenceProviderAdaptor = persistenceProviderAdaptor;
            this.wrapperBeanManagerLifeCycle = wrapperBeanManagerLifeCycle;
        }

        void markPersistenceUnitAvailable() {
            persistenceProviderAdaptor.markPersistenceUnitAvailable(wrapperBeanManagerLifeCycle);
        }
    }

}
