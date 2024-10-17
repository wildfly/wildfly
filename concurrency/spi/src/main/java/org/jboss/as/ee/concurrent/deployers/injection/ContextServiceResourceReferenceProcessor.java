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

import jakarta.enterprise.concurrent.ContextService;

/**
 * @author Eduardo Martins
 */
public class ContextServiceResourceReferenceProcessor implements EEResourceReferenceProcessor {

    private static final String TYPE = ContextService.class.getName();
    private static final LookupInjectionSource injectionSource = new LookupInjectionSource(EEConcurrentDefaultBindingProcessor.COMP_DEFAULT_CONTEXT_SERVICE_JNDI_NAME);

    public static final ContextServiceResourceReferenceProcessor INSTANCE = new ContextServiceResourceReferenceProcessor();

    private ContextServiceResourceReferenceProcessor() {
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
