/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;

import java.util.Collection;

/**
 * Abstract deployment unit processors used to process {@link org.jboss.as.ee.component.ComponentDescription} instances.
 *
 * @author John Bailey
 */
public abstract class AbstractComponentConfigProcessor implements DeploymentUnitProcessor {

    /**
     * {@inheritDoc} *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Collection<ComponentDescription> componentConfigurations = eeModuleDescription.getComponentDescriptions();
        if (componentConfigurations == null || componentConfigurations.isEmpty()) {
            return;
        }

        for (ComponentDescription componentConfiguration : componentConfigurations) {
            final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
            if (index != null) {
                processComponentConfig(deploymentUnit, phaseContext, index, componentConfiguration);
            }
        }
    }

    /**
     * Process the component configuration instance.
     *
     * @param deploymentUnit         The deployment unit
     * @param phaseContext           The phase context
     * @param index                  The annotation index
     * @param componentDescription The component configuration
     * @throws DeploymentUnitProcessingException if any problems occur
     */
    protected abstract void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final ComponentDescription componentDescription) throws DeploymentUnitProcessingException;
}
