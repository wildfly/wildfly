/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.beanvalidation;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessorRegistry;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * DU Processor responsible for registering EEResourceReferenceProcessors wrt Jakarta Bean Validation.
 *
 * @author Eduardo Martins
 */
public class BeanValidationResourceReferenceProcessorRegistryProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(deploymentUnit.getParent() == null) {
            final EEResourceReferenceProcessorRegistry eeResourceReferenceProcessorRegistry = deploymentUnit.getAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY);
            if(eeResourceReferenceProcessorRegistry != null) {
                eeResourceReferenceProcessorRegistry.registerResourceReferenceProcessor(BeanValidationResourceReferenceProcessor.INSTANCE);
                eeResourceReferenceProcessorRegistry.registerResourceReferenceProcessor(BeanValidationFactoryResourceReferenceProcessor.INSTANCE);
            }
        }
    }
}
