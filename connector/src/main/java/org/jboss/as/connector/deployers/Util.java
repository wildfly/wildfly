/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers;


import org.jboss.as.ee.structure.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Utilities
 *
 * @author Jason T. Greene
 */
public class Util {
    public static boolean shouldResolveSpec(DeploymentUnit deploymentUnit) {
        Boolean attachment = deploymentUnit.getAttachment(Attachments.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT);
        return attachment != null && attachment.booleanValue();
    }

    public static boolean shouldResolveJBoss(DeploymentUnit deploymentUnit) {
        Boolean attachment = deploymentUnit.getAttachment(Attachments.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT);
        return attachment != null && attachment.booleanValue();
    }
}
