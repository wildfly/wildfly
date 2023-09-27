/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.concurrent.deployers.injection.ContextServiceResourceReferenceProcessor;
import org.jboss.as.ee.concurrent.deployers.injection.ManagedExecutorServiceResourceReferenceProcessor;
import org.jboss.as.ee.concurrent.deployers.injection.ManagedScheduledExecutorServiceResourceReferenceProcessor;
import org.jboss.as.ee.concurrent.deployers.injection.ManagedThreadFactoryResourceReferenceProcessor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * DUP that adds the {@Link EEResourceReferenceProcessorRegistry} to the deployment, and adds the bean validation resolvers.
 *
 * @author Stuart Douglas
 */
public class ResourceReferenceRegistrySetupProcessor implements DeploymentUnitProcessor {


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(deploymentUnit.getParent() == null) {
            final EEResourceReferenceProcessorRegistry registry = new EEResourceReferenceProcessorRegistry();
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
            deploymentUnit.putAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY, registry);
        } else{
            deploymentUnit.putAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY, deploymentUnit.getParent().getAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY));
        }
    }
}
