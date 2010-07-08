/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.deployment.processor;

import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.attachment.Dependencies;
import org.jboss.as.deployment.item.ModuleDeploymentItem;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.Location;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import java.io.IOException;
import java.util.jar.Manifest;

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.getVirtualFileAttachment;

/**
 * DeploymentUnitProcessor that will extract module dependencies from an archive. 
 *
 * @author John E. Bailey
 */
public class ModuleDependencyProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.PARSE_DESCRIPTORS.plus(100L);

    @Override
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = getVirtualFileAttachment(context);
        final Manifest manifest;
        try {
            manifest = VFSUtils.getManifest(deploymentRoot);
        } catch(IOException e) {
            throw new DeploymentUnitProcessingException("Failed to get manifest for deployment " + deploymentRoot, e, new Location(e.getStackTrace()[0].getFileName(), e.getStackTrace()[0].getLineNumber(), -1, null));
        }
        if(manifest == null)
            return;

        final String dependencyString = manifest.getMainAttributes().getValue("Dependencies");
        final String[] dependencyDefs = dependencyString.split(",");
        for(String dependencyDef : dependencyDefs) {
            final String[] dependencyParts = dependencyDef.split(" ");
            final int dependencyPartsLength = dependencyParts.length;
            if(dependencyPartsLength == 0)
                throw new RuntimeException("Invalid dependency: " + dependencyString);

            final ModuleIdentifier dependencyId = ModuleIdentifier.fromString(dependencyParts[0]);
            boolean export = parseOptionalExportParams(dependencyParts, "export");
            boolean optional = parseOptionalExportParams(dependencyParts, "export");
            ModuleDeploymentItem.Dependency dependency = new ModuleDeploymentItem.Dependency(dependencyId, true, optional, export);
            Dependencies.addDependency(context, dependency);
        }
    }

    private boolean parseOptionalExportParams(final String[] parts, final String expected) {
        if(parts.length > 1) {
            final String part = parts[1];
            if(expected.equals(part))
                return true;
        }
        if(parts.length > 2) {
            final String part = parts[2];
            if(expected.equals(part))
                return true;
        }
        return false;
    }
}
