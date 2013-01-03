/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.reflect;

import static org.jboss.as.server.ServerMessages.MESSAGES;

import java.util.jar.Manifest;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.ManifestHelper;
import org.jboss.modules.Module;

/**
 * The processor to install the reflection index.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class InstallReflectionIndexProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        // OSGi fragments do not have a module
        Manifest manifest = deploymentUnit.getAttachment(Attachments.OSGI_MANIFEST);
        if (ManifestHelper.hasMainAttributeValue(manifest, "Fragment-Host")) {
            return;
        }

        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            throw MESSAGES.nullModuleAttachment(deploymentUnit);
        }

        if(deploymentUnit.getParent() == null) {
            final DeploymentReflectionIndex index = DeploymentReflectionIndex.create();
            deploymentUnit.putAttachment(Attachments.REFLECTION_INDEX, index);
            deploymentUnit.putAttachment(Attachments.PROXY_REFLECTION_INDEX, new ProxyMetadataSource(index));
            deploymentUnit.putAttachment(Attachments.CLASS_INDEX, new DeploymentClassIndex(index, module));
        } else {
            final DeploymentReflectionIndex index = deploymentUnit.getParent().getAttachment(Attachments.REFLECTION_INDEX);
            deploymentUnit.putAttachment(Attachments.REFLECTION_INDEX, index);
            deploymentUnit.putAttachment(Attachments.PROXY_REFLECTION_INDEX, deploymentUnit.getParent().getAttachment(Attachments.PROXY_REFLECTION_INDEX));
            deploymentUnit.putAttachment(Attachments.CLASS_INDEX, new DeploymentClassIndex(index, module));
        }
    }

    public void undeploy(final DeploymentUnit context) {
        context.removeAttachment(Attachments.REFLECTION_INDEX);
    }
}
