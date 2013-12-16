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

package org.jboss.as.jaxrs.deployment;

import org.jboss.as.jaxrs.JaxrsMessages;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

import java.io.Closeable;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Recognize Spring deployment and add the JAX-RS integration to it
 */
public class JaxrsSpringProcessor implements DeploymentUnitProcessor {


    public static final String SPRING_INT_JAR = "resteasy-spring.jar";
    public static final String SPRING_LISTENER = "org.resteasy.plugins.spring.SpringContextLoaderListener";
    public static final String SPRING_SERVLET = "org.springframework.web.servlet.DispatcherServlet";
    public static final String DISABLE_PROPERTY = "org.jboss.as.jaxrs.disableSpringIntegration";


    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getParent() != null) {
            return;
        }

        final List<DeploymentUnit> deploymentUnits = new ArrayList<DeploymentUnit>();
        deploymentUnits.add(deploymentUnit);
        deploymentUnits.addAll(deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS));

        boolean found = false;
        for (DeploymentUnit unit : deploymentUnits) {

            WarMetaData warMetaData = unit.getAttachment(WarMetaData.ATTACHMENT_KEY);
            if (warMetaData == null) {
                continue;
            }
            JBossWebMetaData md = warMetaData.getMergedJBossWebMetaData();
            if (md == null) {
                continue;
            }
            if (md.getContextParams() != null) {
                boolean skip = false;
                for (ParamValueMetaData prop : md.getContextParams()) {
                    if (prop.getParamName().equals(DISABLE_PROPERTY) && "true".equals(prop.getParamValue())) {
                        skip = true;
                    }
                }
                if (skip) {
                    continue;
                }
            }

            if (md.getListeners() != null) {
                for (ListenerMetaData listener : md.getListeners()) {
                    if (SPRING_LISTENER.equals(listener.getListenerClass())) {
                        found = true;
                        break;
                    }
                }
            }
            if(md.getServlets() != null) {
                for (JBossServletMetaData servlet : md.getServlets()) {
                    if (SPRING_SERVLET.equals(servlet.getServletClass())) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) {
                try {
                    URL url = this.getClass().getClassLoader().getResource(SPRING_INT_JAR);
                    if (url == null) {
                        throw JaxrsMessages.MESSAGES.noSpringIntegrationJar();
                    }

                    File file = new File(url.toURI());
                    VirtualFile vf = VFS.getChild(file.toURI());
                    final Closeable mountHandle = VFS.mountZip(file, vf, TempFileProviderService.provider());

                    MountHandle mh = new MountHandle(mountHandle); // actual close is done by the MSC service above
                    ResourceRoot resourceRoot = new ResourceRoot(vf, mh);
                    ModuleRootMarker.mark(resourceRoot);
                    deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, resourceRoot);
                } catch (Exception e) {
                    throw new DeploymentUnitProcessingException(e);
                }
                return;
            }
        }
    }


    public void undeploy(DeploymentUnit context) {
    }
}
