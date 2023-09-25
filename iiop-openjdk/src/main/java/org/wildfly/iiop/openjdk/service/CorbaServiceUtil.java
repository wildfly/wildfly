/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk.service;

import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.ServiceTarget;

/**
 * <p>
 * Utility class used by the CORBA related services.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class CorbaServiceUtil {

    /**
     * <p>
     * Private constructor as required by the {@code Singleton} pattern.
     * </p>
     */
    private CorbaServiceUtil() {
    }

    /**
     * <p>
     * Adds a {@code BinderService} to the specified target. The service binds the specified value to JNDI under the
     * {@code java:/jboss/contextName} context.
     * </p>
     *
     * @param target      the {@code ServiceTarget} where the service will be added.
     * @param contextName the JNDI context name where the value will be bound.
     * @param value       the value to be bound.
     */
    public static void bindObject(final ServiceTarget target, final String contextName, final Object value) {
        final BinderService binderService = new BinderService(contextName);
        binderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(value));
        target.addService(ContextNames.buildServiceName(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, contextName), binderService)
                .addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .install();
    }
}
