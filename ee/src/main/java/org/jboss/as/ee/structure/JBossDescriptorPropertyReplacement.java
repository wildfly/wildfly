package org.jboss.as.ee.structure;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.metadata.property.PropertyReplacers;
import org.jboss.metadata.property.PropertyResolver;

/**
 * @author Stuart Douglas
 */
public class JBossDescriptorPropertyReplacement {

    public static PropertyReplacer propertyReplacer(final DeploymentUnit deploymentUnit) {
        Boolean replacement = deploymentUnit.getAttachment(Attachments.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT);
        if (replacement == null || replacement) {
            final PropertyResolver propertyResolver = deploymentUnit.getAttachment(org.jboss.as.ee.metadata.property.Attachments.FINAL_PROPERTY_RESOLVER);
            return PropertyReplacers.resolvingReplacer(propertyResolver);
        } else {
            return PropertyReplacers.noop();
        }
    }

    private JBossDescriptorPropertyReplacement() {

    }
}
