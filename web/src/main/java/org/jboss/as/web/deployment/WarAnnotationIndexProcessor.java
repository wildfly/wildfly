/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web.deployment;

import org.jboss.as.deployment.AttachmentKey;
import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.attachment.VirtualFileAttachment;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.vfs.VirtualFile;

/**
 * Creates a WarAnnotationIndex.
 *
 * TODO we should not create a specific annotation index, since most other deployers
 * would not care about the .jar name...
 *
 * @author Emanuel Muckenhuber
 */
public class WarAnnotationIndexProcessor implements DeploymentUnitProcessor {

    public static final AttachmentKey<WarAnnotationIndex> ATTACHMENT_KEY = new AttachmentKey<WarAnnotationIndex>(WarAnnotationIndex.class);
    public static final long PRIORITY = DeploymentPhases.PARSE_DESCRIPTORS.plus(200L);

    /** {@inheritDoc} */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        if(context.getAttachment(ATTACHMENT_KEY) != null) {
            return;
        }
        final VirtualFile deploymentRoot = VirtualFileAttachment.getVirtualFileAttachment(context);
        // Create the web annotation index
        final WarAnnotationIndex index = WarAnnotationIndex.create(deploymentRoot);
        context.putAttachment(ATTACHMENT_KEY, index);
    }

}
