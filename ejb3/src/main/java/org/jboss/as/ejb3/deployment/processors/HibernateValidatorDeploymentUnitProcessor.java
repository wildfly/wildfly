/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ejb3.validator.EjbProxyNormalizerCdiExtension;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.wildfly.common.Assert;

/**
 * This processor is used to register {@link org.jboss.as.ejb3.validator.EjbProxyBeanMetaDataClassNormalizer} in the
 * hibernate validator subsystem. Normalizer is used to provide validator with an interface implemented by a proxy.
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class HibernateValidatorDeploymentUnitProcessor implements DeploymentUnitProcessor {

    private static final String BEAN_VALIDATION_CAPABILITY = "org.wildfly.bean-validation";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

        if (support.hasCapability(WELD_CAPABILITY_NAME) && support.hasCapability(BEAN_VALIDATION_CAPABILITY)) {
            try {
                final WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
                weldCapability.registerExtensionInstance(new EjbProxyNormalizerCdiExtension(), deploymentUnit);
            } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
                Assert.unreachableCode();
            }
        }
    }
}