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

package org.jboss.as.arquillian.service;

import java.util.jar.Manifest;

import org.jboss.as.deployment.Phase;
import org.jboss.as.deployment.attachment.ManifestAttachment;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;

/**
 * Service responsible for creating and managing the life-cycle of the Arquillian service.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ArquillianManifestProcessor implements DeploymentUnitProcessor {

    public static final int PRIORITY = Phase.ARQUILLIAN_MANIFEST_PROCESSOR;


    @Override
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        Manifest manifest = context.getAttachment(ManifestAttachment.KEY);
        if (manifest == null) {
            return; // No manifest
        }
        String value = manifest.getMainAttributes().getValue("Arquillian-deployment");
        if (value != null && Boolean.valueOf(value)) {
            context.putAttachment(ArquillianConfig.ATTACHMENT_KEY, new ArquillianConfig());
        }
    }

}
