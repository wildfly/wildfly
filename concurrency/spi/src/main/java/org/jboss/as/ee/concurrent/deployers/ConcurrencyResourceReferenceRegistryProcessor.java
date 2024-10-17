/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessorRegistry;
import org.jboss.as.ee.concurrent.deployers.injection.ContextServiceResourceReferenceProcessor;
import org.jboss.as.ee.concurrent.deployers.injection.ManagedExecutorServiceResourceReferenceProcessor;
import org.jboss.as.ee.concurrent.deployers.injection.ManagedScheduledExecutorServiceResourceReferenceProcessor;
import org.jboss.as.ee.concurrent.deployers.injection.ManagedThreadFactoryResourceReferenceProcessor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * DUP that registers the Concurrency ResourceReferenceProcessors.
 * @author emartins
 */
public class ConcurrencyResourceReferenceRegistryProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(deploymentUnit.getParent() == null) {
            final EEResourceReferenceProcessorRegistry registry = deploymentUnit.getAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY);
            if(registry != null) {
                final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
                if (eeModuleDescription != null) {
                    if (eeModuleDescription.getDefaultResourceJndiNames().getContextService() != null) {
                        registry.registerResourceReferenceProcessor(ContextServiceResourceReferenceProcessor.INSTANCE);
                    }
                    if (eeModuleDescription.getDefaultResourceJndiNames().getManagedExecutorService() != null) {
                        registry.registerResourceReferenceProcessor(ManagedExecutorServiceResourceReferenceProcessor.INSTANCE);
                    }
                    if (eeModuleDescription.getDefaultResourceJndiNames().getManagedScheduledExecutorService() != null) {
                        registry.registerResourceReferenceProcessor(ManagedScheduledExecutorServiceResourceReferenceProcessor.INSTANCE);
                    }
                    if (eeModuleDescription.getDefaultResourceJndiNames().getManagedThreadFactory() != null) {
                        registry.registerResourceReferenceProcessor(ManagedThreadFactoryResourceReferenceProcessor.INSTANCE);
                    }
                }
            }
        }
    }
}
