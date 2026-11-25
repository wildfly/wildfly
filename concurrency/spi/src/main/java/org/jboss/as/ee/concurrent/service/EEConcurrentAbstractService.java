/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent.service;

import org.jboss.as.naming.ImmediateManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Abstract service responsible for managing the lifecyle of EE Concurrent managed resources.
 *
 * @author Eduardo Martins
 */
abstract class EEConcurrentAbstractService<T> implements Service<T> {

    private final String jndiName;

    /**
     *
     * @param jndiName
     */
    EEConcurrentAbstractService(String jndiName) {
        this.jndiName = jndiName;
    }

    public void start(final StartContext context) throws StartException {
        startValue(context);
        // every ee concurrent resource is bound to jndi, so EE components may reference it.
        bindValueToJndi(context);
    }

    /**
     * Starts the service's value.
     * @param context
     * @throws StartException
     */
    abstract void startValue(final StartContext context) throws StartException;

    private void bindValueToJndi(final StartContext context) {
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        final BinderService binderService = new BinderService(bindInfo.getBindName());
        binderService.getManagedObjectInjector().inject(new ImmediateManagedReferenceFactory(getValue()));
        context.getChildTarget().addService(bindInfo.getBinderServiceName(),binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .install();
    }

    public void stop(final StopContext context) {
        stopValue(context);
    }

    /**
     * Stops the service's value.
     * @param context
     */
    abstract void stopValue(final StopContext context);

}
