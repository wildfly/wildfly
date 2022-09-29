/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
