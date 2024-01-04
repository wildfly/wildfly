/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
    private final boolean legacyEmptyBeansXmlTreatmentGlobal;

    public WeldConfigurationProcessor(boolean requireBeanDescriptorGlobal, boolean nonPortableModeGlobal, boolean developmentModeGlobal, boolean legacyEmptyBeansXmlTreatmentGlobal) {
        this.requireBeanDescriptorGlobal = requireBeanDescriptorGlobal;
        this.nonPortableModeGlobal = nonPortableModeGlobal;
        this.developmentModeGlobal = developmentModeGlobal;
        this.legacyEmptyBeansXmlTreatmentGlobal = legacyEmptyBeansXmlTreatmentGlobal;
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
        boolean legacyEmptyBeansXmlTreatment = legacyEmptyBeansXmlTreatmentGlobal;

        WeldJBossAllConfiguration configuration = deploymentUnit.getAttachment(WeldJBossAllConfiguration.ATTACHMENT_KEY);
        if (configuration != null) {
            requireBeanDescriptor = getValue(configuration.getRequireBeanDescriptor(), requireBeanDescriptorGlobal);
            nonPortableMode = getValue(configuration.getNonPortableMode(), nonPortableModeGlobal);
            developmentMode = getValue(configuration.getDevelopmentMode(), developmentModeGlobal);
            legacyEmptyBeansXmlTreatment = getValue(configuration.getLegacyEmptyBeansXmlTreatment(), legacyEmptyBeansXmlTreatmentGlobal);
        }
        WeldConfiguration mergedConfiguration = new WeldConfiguration(requireBeanDescriptor, nonPortableMode, developmentMode, legacyEmptyBeansXmlTreatment);
        deploymentUnit.putAttachment(WeldConfiguration.ATTACHMENT_KEY, mergedConfiguration);
    }

    private static boolean getValue(Boolean value, boolean globalValue) {
        if (value != null) {
            return value;
        } else {
            return globalValue;
        }
    }
}
