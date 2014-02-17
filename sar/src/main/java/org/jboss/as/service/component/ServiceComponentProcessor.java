/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.service.component;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.service.ServiceAttachments;
import org.jboss.as.service.descriptor.JBossServiceConfig;
import org.jboss.as.service.descriptor.JBossServiceXmlDescriptor;

/**
 * Creates EE component descriptions for mbeans, to support {@link javax.annotation.Resource} injection.
 *
 * @author Eduardo Martins
 *
 */
public class ServiceComponentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final JBossServiceXmlDescriptor serviceXmlDescriptor = deploymentUnit
                .getAttachment(JBossServiceXmlDescriptor.ATTACHMENT_KEY);
        if (serviceXmlDescriptor == null) {
            // Skip deployments without a service xml descriptor
            return;
        }
        final EEModuleDescription moduleDescription = deploymentUnit
                .getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return; // not an EE deployment
        }
        final EEApplicationClasses applicationClassesDescription = deploymentUnit
                .getAttachment(org.jboss.as.ee.component.Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final Map<String, ServiceComponentInstantiator> serviceComponents = new HashMap<String, ServiceComponentInstantiator>();
        for (final JBossServiceConfig serviceConfig : serviceXmlDescriptor.getServiceConfigs()) {
            ServiceComponentDescription componentDescription = new ServiceComponentDescription(serviceConfig.getName(),
                    serviceConfig.getCode(), moduleDescription, deploymentUnit.getServiceName(), applicationClassesDescription);
            moduleDescription.addComponent(componentDescription);
            serviceComponents.put(serviceConfig.getName(), new ServiceComponentInstantiator(deploymentUnit,
                    componentDescription));
        }
        deploymentUnit.putAttachment(ServiceAttachments.SERVICE_COMPONENT_INSTANTIATORS, serviceComponents);
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

}
