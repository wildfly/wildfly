/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.deployment.processors;

import java.util.Iterator;

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.PrivateSubDeploymentMarker;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.util.ServiceLoader;

/**
 * Deployment processor that loads CDI portable extensions.
 *
 * @author Stuart Douglas
 *
 */
public class WeldPortableExtensionProcessor implements DeploymentUnitProcessor {

    private static final Logger log = Logger.getLogger("org.jboss.weld");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // for war modules we require a beans.xml to load portable extensions
        if (PrivateSubDeploymentMarker.isPrivate(deploymentUnit)) {
            if (!WeldDeploymentMarker.isWeldDeployment(deploymentUnit)) {
                return;
            }
        } else if (deploymentUnit.getParent() == null) {
            // if any sub deployments have beans.xml then the top level deployment is
            // marked as a weld deplyment
            if (!WeldDeploymentMarker.isWeldDeployment(deploymentUnit)) {
                return;
            }
        } else {
            // if any deployments have a beans.xml we need to load portable extensions
            // even if this one does not.
            if (!WeldDeploymentMarker.isWeldDeployment(deploymentUnit.getParent())) {
                return;
            }
        }

        // we attach extensions directly to the top level deployment
        final DeploymentUnit topLevelDeployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit
                .getParent();

        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        loadAttachments(module, topLevelDeployment);
    }

    private void loadAttachments(Module module, DeploymentUnit deploymentUnit) {
        // now load extensions
        final ServiceLoader<Extension> loader = ServiceLoader.load(Extension.class, module.getClassLoader());
        final Iterator<Metadata<Extension>> iterator = loader.iterator();
        while (iterator.hasNext()) {
            Metadata<Extension> extension = iterator.next();
            log.debug("Loaded portable extension " + extension.getLocation());
            deploymentUnit.addToAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS, extension);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

}
