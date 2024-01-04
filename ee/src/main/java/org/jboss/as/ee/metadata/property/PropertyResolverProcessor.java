/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.metadata.property;

import java.util.function.Function;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * @author John Bailey
 */
public class PropertyResolverProcessor implements DeploymentUnitProcessor {
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final PropertyReplacer propertyReplacer = deploymentUnit.getAttachment(Attachments.FINAL_PROPERTY_REPLACER);
        //We pass a function basically to be used by wildfly-core which does not the dependencies used to instantiate a PropertyReplacer
        final Function<String, String> functionExpand = (value) -> propertyReplacer.replaceProperties(value);

        // setup the expression expand function for spec descriptors, if property expansion is enabled for them. If it does not exist, then by default we assume they are enabled.
        final Boolean specDescriptorPropReplacement = deploymentUnit.getAttachment(org.jboss.as.ee.structure.Attachments.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT);
        if (specDescriptorPropReplacement == null || specDescriptorPropReplacement) {
            deploymentUnit.putAttachment(org.jboss.as.server.deployment.Attachments.SPEC_DESCRIPTOR_EXPR_EXPAND_FUNCTION, functionExpand);
        }
        // setup the expression expand function for JBoss/WildFly descriptors, if property expansion is enabled for them
        final Boolean jbossDescriptorPropReplacement = deploymentUnit.getAttachment(org.jboss.as.ee.structure.Attachments.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT);
        if (jbossDescriptorPropReplacement == null || jbossDescriptorPropReplacement) {
            deploymentUnit.putAttachment(org.jboss.as.server.deployment.Attachments.WFLY_DESCRIPTOR_EXPR_EXPAND_FUNCTION, functionExpand);
        }
    }

    public void undeploy(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(Attachments.FINAL_PROPERTY_REPLACER);

        deploymentUnit.removeAttachment(org.jboss.as.server.deployment.Attachments.SPEC_DESCRIPTOR_EXPR_EXPAND_FUNCTION);
        deploymentUnit.removeAttachment(org.jboss.as.server.deployment.Attachments.WFLY_DESCRIPTOR_EXPR_EXPAND_FUNCTION);
    }
}
