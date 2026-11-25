/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment.processor;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.jboss.as.weld.deployment.AttachmentKeys.START_COMPLETION_DEPENDENCIES;

import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.BeanManager;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.beanvalidation.BeanValidationAttachments;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.CdiValidatorFactoryService;
import org.jboss.as.weld.ServiceNames;
import org.jboss.as.weld.WeldCapability;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Deployment processor that replaces the delegate of LazyValidatorFactory with a CDI-enabled ValidatorFactory.
 *
 * @author Farah Juma
 * @author Martin Kouba
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class CdiBeanValidationFactoryProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit topLevelDeployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final ServiceName weldStartService = topLevelDeployment.getServiceName().append(ServiceNames.WELD_START_SERVICE_NAME);
        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

        final WeldCapability weldCapability;
        try {
            weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
        } catch (CapabilityServiceSupport.NoSuchCapabilityException ignored) {
            return;
        }

        if (!weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
            return;
        }

        if (!deploymentUnit.hasAttachment(BeanValidationAttachments.VALIDATOR_FACTORY)) {
            return;
        }

        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        final ServiceName serviceName = deploymentUnit.getServiceName().append(CdiValidatorFactoryService.SERVICE_NAME);
        final ServiceBuilder<?> sb = serviceTarget.addService(serviceName);
        final Supplier<BeanManager> beanManagerSupplier = weldCapability.addBeanManagerService(deploymentUnit, sb);
        sb.requires(weldStartService);
        sb.setInstance(new CdiValidatorFactoryService(deploymentUnit, beanManagerSupplier));
        sb.install();

        // Make sure CdiValidatorFactoryService is started before WeldStartCompletionService sends out lifecycle events
        deploymentUnit.addToAttachmentList(START_COMPLETION_DEPENDENCIES, serviceName);
    }
}
