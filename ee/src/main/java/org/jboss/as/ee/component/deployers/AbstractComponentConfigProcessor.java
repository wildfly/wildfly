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

    /**
     * {@inheritDoc} *
     */
    public void undeploy(DeploymentUnit context) {
    }
}
