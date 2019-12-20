/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.faulttolerance.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.wildfly.extension.microprofile.faulttolerance.MicroProfileFaultToleranceLogger;

/**
 * Utility class which marks MP FT deployments.
 *
 * @author Radoslav Husar
 */
public class MicroProfileFaultToleranceMarker {

    private static final AttachmentKey<Boolean> ATTACHMENT_KEY = AttachmentKey.create(Boolean.class);

    static void mark(DeploymentUnit deployment) {
        deployment.putAttachment(ATTACHMENT_KEY, true);
    }

    /**
     * @return whether the deployment was marked as MP FT deployment
     */
    public static boolean isMarked(DeploymentUnit deploymentUnit) {
        Boolean b = deploymentUnit.getAttachment(ATTACHMENT_KEY);
        return (b != null) && b;
    }

    static void clearMark(DeploymentUnit deployment) {
        deployment.removeAttachment(ATTACHMENT_KEY);
    }

    public static boolean hasMicroProfileFaultToleranceAnnotations(DeploymentUnit deploymentUnit) {
        CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        for (MicroProfileFaultToleranceAnnotation ftAnnotation : MicroProfileFaultToleranceAnnotation.values()) {
            if (!index.getAnnotations(ftAnnotation.getDotName()).isEmpty()) {
                MicroProfileFaultToleranceLogger.ROOT_LOGGER.debugf("Deployment '%s' is a MicroProfile Fault Tolerance deployment – @%s annotation found.", deploymentUnit.getName(), ftAnnotation.getDotName());
                return true;
            }
        }

        MicroProfileFaultToleranceLogger.ROOT_LOGGER.debugf("No MicroProfile Fault Tolerance annotations found in deployment '%s'.", deploymentUnit.getName());

        return false;
    }

    private MicroProfileFaultToleranceMarker() {
        // Utility class.
    }
}
