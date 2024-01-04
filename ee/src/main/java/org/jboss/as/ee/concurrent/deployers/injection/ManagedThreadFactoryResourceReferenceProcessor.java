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

import jakarta.enterprise.concurrent.ManagedThreadFactory;

/**
 * @author Eduardo Martins
 */
public class ManagedThreadFactoryResourceReferenceProcessor implements EEResourceReferenceProcessor {

    private static final String TYPE = ManagedThreadFactory.class.getName();
    private static final LookupInjectionSource injectionSource = new LookupInjectionSource(EEConcurrentDefaultBindingProcessor.COMP_DEFAULT_MANAGED_THREAD_FACTORY_JNDI_NAME);

    public static final ManagedThreadFactoryResourceReferenceProcessor INSTANCE = new ManagedThreadFactoryResourceReferenceProcessor();

    private ManagedThreadFactoryResourceReferenceProcessor() {
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
