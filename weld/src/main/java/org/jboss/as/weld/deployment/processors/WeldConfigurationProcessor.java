/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
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

package org.jboss.as.weld.deployment.processors;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldJBossAllConfiguration;

/**
 * Merges the per-deployment configuration defined in <code>jboss-all.xml</code> with the global configuration and attaches the result under
 * {@link WeldConfiguration#ATTACHMENT_KEY}.
 *
 * @author Jozef Hartinger
 *
 */
public class WeldConfigurationProcessor implements DeploymentUnitProcessor {

    private final boolean requireBeanDescriptorGlobal;
    private final boolean nonPortableModeGlobal;
    private final boolean developmentModeGlobal;

    public WeldConfigurationProcessor(boolean requireBeanDescriptorGlobal, boolean nonPortableModeGlobal, boolean developmentModeGlobal) {
        this.requireBeanDescriptorGlobal = requireBeanDescriptorGlobal;
        this.nonPortableModeGlobal = nonPortableModeGlobal;
        this.developmentModeGlobal = developmentModeGlobal;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (deploymentUnit.getParent() != null) {
            return; // only attach the configuration to the root deployment
        }

        boolean requireBeanDescriptor = requireBeanDescriptorGlobal;
        boolean nonPortableMode = nonPortableModeGlobal;
        boolean developmentMode = developmentModeGlobal;

        WeldJBossAllConfiguration configuration = deploymentUnit.getAttachment(WeldJBossAllConfiguration.ATTACHMENT_KEY);
        if (configuration != null) {
            requireBeanDescriptor = getValue(configuration.getRequireBeanDescriptor(), requireBeanDescriptorGlobal);
            nonPortableMode = getValue(configuration.getNonPortableMode(), nonPortableModeGlobal);
            developmentMode = getValue(configuration.getDevelopmentMode(), developmentModeGlobal);
        }
        WeldConfiguration mergedConfiguration = new WeldConfiguration(requireBeanDescriptor, nonPortableMode, developmentMode);
        deploymentUnit.putAttachment(WeldConfiguration.ATTACHMENT_KEY, mergedConfiguration);
    }

    private static boolean getValue(Boolean value, boolean globalValue) {
        if (value != null) {
            return value;
        } else {
            return globalValue;
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
