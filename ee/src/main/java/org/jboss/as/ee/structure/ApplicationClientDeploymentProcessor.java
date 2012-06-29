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
package org.jboss.as.ee.structure;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.vfs.VirtualFile;

import java.util.List;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Processor that sets up application clients. If this is an ear deployment it will mark the client as
 * a sub deployment.
 * If this is an application client deployment it will set the deployment type.
 * <p/>
 *
 * @author Stuart Douglas
 */
public class ApplicationClientDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String META_INF_APPLICATION_CLIENT_XML = "META-INF/application-client.xml";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            List<ResourceRoot> potentialSubDeployments = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
            for (ResourceRoot resourceRoot : potentialSubDeployments) {
                if (ModuleRootMarker.isModuleRoot(resourceRoot)) {
                    // module roots cannot be ejb jars
                    continue;
                }
                VirtualFile appclientClientXml = resourceRoot.getRoot().getChild(META_INF_APPLICATION_CLIENT_XML);
                if (appclientClientXml.exists()) {
                    SubDeploymentMarker.mark(resourceRoot);
                    ModuleRootMarker.mark(resourceRoot);
                } else {
                    final Manifest manifest = resourceRoot.getAttachment(Attachments.MANIFEST);
                    if (manifest != null) {
                        Attributes main = manifest.getMainAttributes();
                        if (main != null) {
                            String mainClass = main.getValue("Main-Class");
                            if (mainClass != null && !mainClass.isEmpty()) {
                                SubDeploymentMarker.mark(resourceRoot);
                                ModuleRootMarker.mark(resourceRoot);
                            }
                        }
                    }
                }
            }
        } else if (deploymentUnit.getName().toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
            final ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            final ModuleMetaData md = root.getAttachment(org.jboss.as.ee.structure.Attachments.MODULE_META_DATA);
            if (md != null) {
                if (md.getType() == ModuleMetaData.ModuleType.Client) {
                    DeploymentTypeMarker.setType(DeploymentType.APPLICATION_CLIENT, deploymentUnit);
                }
            } else {
                VirtualFile appclientClientXml = root.getRoot().getChild(META_INF_APPLICATION_CLIENT_XML);
                if (appclientClientXml.exists()) {
                    DeploymentTypeMarker.setType(DeploymentType.APPLICATION_CLIENT, deploymentUnit);
                } else {
                    final Manifest manifest = root.getAttachment(Attachments.MANIFEST);
                    if (manifest != null) {
                        Attributes main = manifest.getMainAttributes();
                        if (main != null) {
                            String mainClass = main.getValue("Main-Class");
                            if (mainClass != null && !mainClass.isEmpty()) {
                                DeploymentTypeMarker.setType(DeploymentType.APPLICATION_CLIENT, deploymentUnit);
                            }
                        }
                    }
                }
            }
        }
    }


    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
