/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ee.component.processor;

import java.util.List;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.service.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;

/**
 * Abstract deployment unit processors used to process {@link ComponentConfiguration} instances.
 *
 * @author John Bailey
 */
public abstract class AbstractComponentConfigProcessor implements DeploymentUnitProcessor {

    /**
     * {@inheritDoc} *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<ComponentConfiguration> componentConfigurations = deploymentUnit.getAttachment(Attachments.COMPONENT_CONFIGS);
        if (componentConfigurations == null || componentConfigurations.isEmpty()) {
            return;
        }

        for (ComponentConfiguration componentConfiguration : componentConfigurations) {
            processComponentConfig(deploymentUnit, phaseContext, componentConfiguration);
        }
    }

    /**
     * Process the component configuration instance.
     *
     * @param deploymentUnit         The deployment unit
     * @param phaseContext           The phase context
     * @param componentConfiguration The component configuration
     * @throws DeploymentUnitProcessingException if any problems occur
     */
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index == null) {
            return;
        }

        processComponentConfig(deploymentUnit, phaseContext, index, componentConfiguration);
    }

    /**
     * Process the component configuration instance.
     *
     * @param deploymentUnit         The deployment unit
     * @param phaseContext           The phase context
     * @param index                  The annotation index
     * @param componentConfiguration The component configuration
     * @throws DeploymentUnitProcessingException if any problems occur
     */
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
    }


    /**
     * {@inheritDoc} *
     */
    public void undeploy(DeploymentUnit context) {
    }
}
