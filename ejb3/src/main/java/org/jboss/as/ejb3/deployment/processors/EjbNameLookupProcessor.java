/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EjbLookup;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Processor that builds a data structure to enable lookup of all EJB's in the application by name.
 *
 * @author Stuart Douglas
 */
public class EjbNameLookupProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit parent = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final List<DeploymentUnit> subDeployments = parent.getAttachmentList(Attachments.SUB_DEPLOYMENTS);
        //do not build the EjbLookup for parent deployments
        if(deploymentUnit.getParent() == null && !subDeployments.isEmpty()) {
            return;
        }

        final Set<DeploymentUnit> allDeploymentUnits = new HashSet<DeploymentUnit>();
        allDeploymentUnits.add(parent);
        allDeploymentUnits.addAll(subDeployments);


        final EjbLookup.Builder builder = EjbLookup.builder(deploymentUnit);
        for(final DeploymentUnit deployment : allDeploymentUnits) {
            final Module module = deployment.getAttachment(Attachments.MODULE);
            if(module == null) {
                continue;
            }
            final ModuleClassLoader classLoader = module.getClassLoader();
            final EEModuleDescription eeModuleDescription = deployment.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            for(final ComponentDescription componentDescription : eeModuleDescription.getComponentDescriptions()) {
                if(componentDescription instanceof EJBComponentDescription) {
                    builder.addEjb(deployment, (EJBComponentDescription) componentDescription, classLoader);
                }
            }
        }
        deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.EJB_LOOKUP, builder.build());
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
