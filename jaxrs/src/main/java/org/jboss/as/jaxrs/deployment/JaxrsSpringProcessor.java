/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs.deployment;

import java.io.Closeable;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * Recognize Spring deployment and add the Jakarta RESTful Web Services integration to it
 */
public class JaxrsSpringProcessor implements DeploymentUnitProcessor {

    private static final String JAR_LOCATION = "resteasy-spring-jar";
    private static final String MODULE = "org.jboss.resteasy.resteasy-spring";

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
            final Path dir = Path.of(fileUrl.toURI());
            File file;
            try (Stream<Path> stream = Files.walk(dir)) {
                file = stream.filter((f) -> f.getFileName().toString().endsWith(".jar"))
                        .map(Path::toFile)
                        .findFirst().orElseThrow(JaxrsLogger.JAXRS_LOGGER::noSpringIntegrationJar);
            }
            VirtualFile vf = VFS.getChild(file.toURI());
            final Closeable mountHandle = VFS.mountZip(file, vf, TempFileProviderService.provider());
            final ServiceBuilder<?> builder = serviceTarget.addService();
            final Consumer<Closeable> consumer = builder.provides(ServiceName.JBOSS.append(SERVICE_NAME));
            builder.setInstance(new org.jboss.msc.Service() {
                @Override
                public void start(final StartContext context) {
                    consumer.accept(mountHandle);
                }

                @Override
                public void stop(final StopContext context) {
                    VFSUtils.safeClose(mountHandle);
                }
            });
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

        final List<DeploymentUnit> deploymentUnits = new ArrayList<>();
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
                    MountHandle mh = MountHandle.create(null); // actual close is done by the MSC service above
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
}
