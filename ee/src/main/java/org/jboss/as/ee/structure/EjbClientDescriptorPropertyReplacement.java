package org.jboss.as.ee.structure;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.metadata.property.PropertyReplacers;

/**
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */
public class EjbClientDescriptorPropertyReplacement {

    public static PropertyReplacer propertyReplacer(final DeploymentUnit deploymentUnit) {
        Boolean replacement = deploymentUnit.getAttachment(Attachments.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT);
        if (replacement == null || replacement) {
            PropertyReplacer replacer = deploymentUnit
                    .getAttachment(org.jboss.as.ee.metadata.property.Attachments.FINAL_PROPERTY_REPLACER);
            return replacer != null ? replacer : PropertyReplacers.noop();
        } else {
            return PropertyReplacers.noop();
        }
    }

    private EjbClientDescriptorPropertyReplacement() {
    }
}
