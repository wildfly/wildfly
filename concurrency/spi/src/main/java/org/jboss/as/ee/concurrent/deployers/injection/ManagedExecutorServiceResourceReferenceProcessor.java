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

import jakarta.enterprise.concurrent.ManagedExecutorService;

/**
 * @author Eduardo Martins
 */
public class ManagedExecutorServiceResourceReferenceProcessor implements EEResourceReferenceProcessor {

    private static final String TYPE = ManagedExecutorService.class.getName();
    private static final LookupInjectionSource injectionSource = new LookupInjectionSource(EEConcurrentDefaultBindingProcessor.COMP_DEFAULT_MANAGED_EXECUTOR_SERVICE_JNDI_NAME);

    public static final ManagedExecutorServiceResourceReferenceProcessor INSTANCE = new ManagedExecutorServiceResourceReferenceProcessor();

    private ManagedExecutorServiceResourceReferenceProcessor() {
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
