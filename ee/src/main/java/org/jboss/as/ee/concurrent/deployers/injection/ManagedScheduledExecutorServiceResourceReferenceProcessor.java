/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.deployers.injection;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessor;
import org.jboss.as.ee.concurrent.deployers.EEConcurrentDefaultBindingProcessor;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;

/**
 * @author Eduardo Martins
 */
public class ManagedScheduledExecutorServiceResourceReferenceProcessor implements EEResourceReferenceProcessor {

    private static final String TYPE = ManagedScheduledExecutorService.class.getName();
    private static final LookupInjectionSource injectionSource = new LookupInjectionSource(EEConcurrentDefaultBindingProcessor.COMP_DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_JNDI_NAME);

    public static final ManagedScheduledExecutorServiceResourceReferenceProcessor INSTANCE = new ManagedScheduledExecutorServiceResourceReferenceProcessor();

    private ManagedScheduledExecutorServiceResourceReferenceProcessor() {
    }

    @Override
    public String getResourceReferenceType() {
        return TYPE;
    }

    @Override
    public InjectionSource getResourceReferenceBindingSource() throws DeploymentUnitProcessingException {
        return injectionSource;
    }
}
