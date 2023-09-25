/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.deployers;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.deployers.AbstractPlatformBindingProcessor;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Processor responsible for binding the default EE concurrency resources to the naming context of EE modules/components.
 *
 * @author Eduardo Martins
 */
public class EEConcurrentDefaultBindingProcessor extends AbstractPlatformBindingProcessor {

    public static final String DEFAULT_CONTEXT_SERVICE_JNDI_NAME = "DefaultContextService";
    public static final String COMP_DEFAULT_CONTEXT_SERVICE_JNDI_NAME = "java:comp/"+DEFAULT_CONTEXT_SERVICE_JNDI_NAME;

    public static final String DEFAULT_MANAGED_EXECUTOR_SERVICE_JNDI_NAME = "DefaultManagedExecutorService";
    public static final String COMP_DEFAULT_MANAGED_EXECUTOR_SERVICE_JNDI_NAME = "java:comp/"+DEFAULT_MANAGED_EXECUTOR_SERVICE_JNDI_NAME;

    public static final String DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_JNDI_NAME = "DefaultManagedScheduledExecutorService";
    public static final String COMP_DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_JNDI_NAME = "java:comp/"+DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_JNDI_NAME;

    public static final String DEFAULT_MANAGED_THREAD_FACTORY_JNDI_NAME = "DefaultManagedThreadFactory";
    public static final String COMP_DEFAULT_MANAGED_THREAD_FACTORY_JNDI_NAME = "java:comp/"+DEFAULT_MANAGED_THREAD_FACTORY_JNDI_NAME;

    @Override
    protected void addBindings(DeploymentUnit deploymentUnit, EEModuleDescription moduleDescription) {
        final String contextService = moduleDescription.getDefaultResourceJndiNames().getContextService();
        if(contextService != null) {
            addBinding(contextService, DEFAULT_CONTEXT_SERVICE_JNDI_NAME, deploymentUnit, moduleDescription);
        }
        final String managedExecutorService = moduleDescription.getDefaultResourceJndiNames().getManagedExecutorService();
        if(managedExecutorService != null) {
            addBinding(managedExecutorService, DEFAULT_MANAGED_EXECUTOR_SERVICE_JNDI_NAME, deploymentUnit, moduleDescription);
        }
        final String managedScheduledExecutorService = moduleDescription.getDefaultResourceJndiNames().getManagedScheduledExecutorService();
        if(managedScheduledExecutorService != null) {
            addBinding(managedScheduledExecutorService, DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_JNDI_NAME, deploymentUnit, moduleDescription);
        }
        final String managedThreadFactory = moduleDescription.getDefaultResourceJndiNames().getManagedThreadFactory();
        if(managedThreadFactory != null) {
            addBinding(managedThreadFactory, DEFAULT_MANAGED_THREAD_FACTORY_JNDI_NAME, deploymentUnit, moduleDescription);
        }
    }
}
