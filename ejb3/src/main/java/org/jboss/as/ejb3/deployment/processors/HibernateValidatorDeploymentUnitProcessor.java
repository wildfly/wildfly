/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}