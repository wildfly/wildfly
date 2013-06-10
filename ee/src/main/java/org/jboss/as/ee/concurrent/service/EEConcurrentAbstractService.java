/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.concurrent.service;

import org.jboss.as.naming.ImmediateManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;

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
        final ImmediateManagedReferenceFactory managedReferenceFactory = new ImmediateManagedReferenceFactory(getValue());
        context.getChildTarget().addService(bindInfo.getBinderServiceName(),binderService)
                .addInjectionValue(binderService.getManagedObjectInjector(),new ImmediateValue<ManagedReferenceFactory>(managedReferenceFactory))
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
