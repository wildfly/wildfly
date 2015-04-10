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

import org.jboss.as.jaxrs.logging.JaxrsLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
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

    private static final String JAR_LOCATION = "resteasy-spring-jar";
    private static final ModuleIdentifier MODULE = ModuleIdentifier.create("org.jboss.resteasy.resteasy-spring");

    public static final String SPRING_LISTENER = "org.jboss.resteasy.plugins.spring.SpringContextLoaderListener";
    public static final String SPRING_SERVLET = "org.springframework.web.servlet.DispatcherServlet";
    @Deprecated
    public static final String DISABLE_PROPERTY = "org.jboss.as.jaxrs.disableSpringIntegration";
    public static final String ENABLE_PROPERTY = "org.jboss.as.jaxrs.enableSpringIntegration";
    public static final String SERVICE_NAME = "resteasy-spring-integration-resource-root";

    private final ServiceTarget serviceTarget;
    private VirtualFile resourceRoot;

    public JaxrsSpringProcessor(ServiceTarget serviceTarget) {
        this.serviceTarget = serviceTarget;
    }

    /**
     * Lookup Seam integration resource loader.
     *
     * @return the Seam integration resource loader
     * @throws DeploymentUnitProcessingException
     *          for any error
     */
    protected synchronized VirtualFile getResteasySpringVirtualFile() throws DeploymentUnitProcessingException {
        if(resourceRoot != null) {
            return resourceRoot;
        }
        try {
            Module module = Module.getBootModuleLoader().loadModule(MODULE);
            URL fileUrl = module.getClassLoader().getResource(JAR_LOCATION);

            if (fileUrl == null) {
                throw JaxrsLogger.JAXRS_LOGGER.noSpringIntegrationJar();
            }
            File dir = new File(fileUrl.toURI());
            File file = null;
            for (String jar : dir.list()) {
                if (jar.endsWith(".jar")) {
                    file = new File(dir, jar);
                    break;
                }
            }
            if (file == null) {
                throw JaxrsLogger.JAXRS_LOGGER.noSpringIntegrationJar();
            }
            VirtualFile vf = VFS.getChild(file.toURI());
            final Closeable mountHandle = VFS.mountZip(file, vf, TempFileProviderService.provider());
            Service<Closeable> mountHandleService = new Service<Closeable>() {
                public void start(StartContext startContext) throws StartException {
                }

                public void stop(StopContext stopContext) {
                    VFSUtils.safeClose(mountHandle);
                }

                public Closeable getValue() throws IllegalStateException, IllegalArgumentException {
                    return mountHandle;
                }
            };
            ServiceBuilder<Closeable> builder = serviceTarget.addService(ServiceName.JBOSS.append(SERVICE_NAME),
                    mountHandleService);
            builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
            resourceRoot = vf;

            return resourceRoot;
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

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
                    if (prop.getParamName().equals(ENABLE_PROPERTY)) {
                        boolean explicitEnable = Boolean.parseBoolean(prop.getParamValue());
                        if (explicitEnable) {
                            found = true;
                        } else {
                            skip = true;
                        }
                        break;
                    } else if (prop.getParamName().equals(DISABLE_PROPERTY) && "true".equals(prop.getParamValue())) {
                        skip = true;
                        JaxrsLogger.JAXRS_LOGGER.disablePropertyDeprecated();
                        break;
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
            if (md.getServlets() != null) {
                for (JBossServletMetaData servlet : md.getServlets()) {
                    if (SPRING_SERVLET.equals(servlet.getServletClass())) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) {
                try {
                    MountHandle mh = new MountHandle(null); // actual close is done by the MSC service above
                    ResourceRoot resourceRoot = new ResourceRoot(getResteasySpringVirtualFile(), mh);
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
