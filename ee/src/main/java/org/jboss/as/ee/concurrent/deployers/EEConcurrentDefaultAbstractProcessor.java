/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.concurrent.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

import java.util.Collection;
import java.util.List;

/**
 * The abstract {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} which sets up the default concurrent managed objects for each EE component in the deployment unit.
 *
 * @author Eduardo Martins
 */
abstract class EEConcurrentDefaultAbstractProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if(eeModuleDescription == null) {
            return;
        }
        if (DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            addBindingsConfigurations("java:module/", eeModuleDescription.getBindingConfigurations());
        }

        final Collection<ComponentDescription> componentDescriptions = eeModuleDescription.getComponentDescriptions();
        if (componentDescriptions == null) {
            return;
        }
        for (ComponentDescription componentDescription : componentDescriptions) {
            if (componentDescription.getNamingMode() == ComponentNamingMode.CREATE) {
                addBindingsConfigurations("java:comp/", componentDescription.getBindingConfigurations());
            }
        }
    }

    abstract void addBindingsConfigurations(String bindingNamePrefix, List<BindingConfiguration> bindingConfigurations);

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
