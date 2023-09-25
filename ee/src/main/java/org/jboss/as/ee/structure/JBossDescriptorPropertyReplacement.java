/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.structure;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.metadata.property.PropertyReplacers;

/**
 * @author Stuart Douglas
 */
public class JBossDescriptorPropertyReplacement {

    public static PropertyReplacer propertyReplacer(final DeploymentUnit deploymentUnit) {
        Boolean replacement = deploymentUnit.getAttachment(Attachments.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT);
        if (replacement == null || replacement) {
            PropertyReplacer replacer = deploymentUnit.getAttachment(org.jboss.as.ee.metadata.property.Attachments.FINAL_PROPERTY_REPLACER);
            // Replacer might be null if the EE subsystem isn't installed (e.g. sar w/o ee) TODO clean up this relationship
            return replacer != null ? replacer : PropertyReplacers.noop();
        } else {
            return PropertyReplacers.noop();
        }
    }

    private JBossDescriptorPropertyReplacement() {

    }
}
