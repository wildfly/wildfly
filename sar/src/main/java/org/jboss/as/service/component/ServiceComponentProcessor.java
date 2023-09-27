/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
}
