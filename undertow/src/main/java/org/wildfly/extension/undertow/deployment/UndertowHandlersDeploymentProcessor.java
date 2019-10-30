/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow.deployment;

import io.undertow.server.handlers.builder.PredicatedHandler;
import io.undertow.server.handlers.builder.PredicatedHandlersParser;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.HttpHandlerMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DUP that handles undertow-handlers.conf, and handlers definded in jboss-web.xml
 *
 * @author Stuart Douglas
 */
public class UndertowHandlersDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String WEB_INF = "WEB-INF/undertow-handlers.conf";
    private static final String META_INF = "META-INF/undertow-handlers.conf";

    public static final AttachmentKey<List<PredicatedHandler>> PREDICATED_HANDLERS = AttachmentKey.create(List.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            return;
        }
        handleInfoFile(deploymentUnit, module);
        handleJbossWebXml(deploymentUnit, module);
    }

    private void handleJbossWebXml(DeploymentUnit deploymentUnit, Module module) throws DeploymentUnitProcessingException {
        WarMetaData warMetadata = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetadata == null) {
            return;
        }
        JBossWebMetaData merged = warMetadata.getMergedJBossWebMetaData();
        if (merged == null) {
            return;
        }
        List<HttpHandlerMetaData> handlers = merged.getHandlers();
        if (handlers == null) {
            return;
        }
        for (HttpHandlerMetaData hander : handlers) {
            try {
                ClassLoader cl = module.getClassLoader();
                if (hander.getModule() != null) {
                    Module handlerModule = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER).loadModule(ModuleIdentifier.fromString(hander.getModule()));
                    cl = handlerModule.getClassLoader();

                }
                Class<?> handlerClass = cl.loadClass(hander.getHandlerClass());
                Map<String, String> params = new HashMap<>();
                if(hander.getParams() != null) {
                    for(ParamValueMetaData param : hander.getParams()) {
                        params.put(param.getParamName(), param.getParamValue());
                    }
                }
                deploymentUnit.addToAttachmentList(UndertowAttachments.UNDERTOW_OUTER_HANDLER_CHAIN_WRAPPERS, new ConfiguredHandlerWrapper(handlerClass, params));
            } catch (Exception e) {
                throw UndertowLogger.ROOT_LOGGER.failedToConfigureHandlerClass(hander.getHandlerClass(), e);
            }
        }

    }

    private void handleInfoFile(DeploymentUnit deploymentUnit, Module module) {
        final List<PredicatedHandler> handlerWrappers = new ArrayList<>();
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            VirtualFile config = root.getRoot().getChild(WEB_INF);
            try {
                if (config.exists()) {
                    handlerWrappers.addAll(PredicatedHandlersParser.parse(config.openStream(), module.getClassLoader()));
                }
                Enumeration<URL> paths = module.getClassLoader().getResources(META_INF);
                while (paths.hasMoreElements()) {
                    URL path = paths.nextElement();
                    handlerWrappers.addAll(PredicatedHandlersParser.parse(path.openStream(), module.getClassLoader()));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!handlerWrappers.isEmpty()) {
                deploymentUnit.putAttachment(PREDICATED_HANDLERS, handlerWrappers);
            }

        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
