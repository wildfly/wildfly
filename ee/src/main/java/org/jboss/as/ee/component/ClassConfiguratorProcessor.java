/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import java.util.Collection;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Deployment processor responsible for executing the {@link ClassConfigurator} instances for a class within
 * this deployment.
 *
 * @author John Bailey
 */
public class ClassConfiguratorProcessor implements DeploymentUnitProcessor {
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if(moduleDescription == null) {
            return;
        }
        final EEModuleConfiguration moduleConfiguration = deploymentUnit.getAttachment(Attachments.EE_MODULE_CONFIGURATION);
        if(moduleConfiguration == null) {
            throw new DeploymentUnitProcessingException("EE module description found, but no module configuration attached to the deployment.");
        }
        final Collection<EEModuleClassDescription> classDescriptions = moduleDescription.getClassDescriptions();
        if(classDescriptions != null) for(EEModuleClassDescription classDescription : classDescriptions) {
            final EEModuleClassConfiguration classConfiguration = moduleConfiguration.getClassConfiguration(classDescription.getClassName());
            for(ClassConfigurator classConfigurator : classDescription.getConfigurators()) {
                classConfigurator.configure(phaseContext, classDescription, classConfiguration);
            }
        }
    }

    public void undeploy(DeploymentUnit context) {
    }
}
