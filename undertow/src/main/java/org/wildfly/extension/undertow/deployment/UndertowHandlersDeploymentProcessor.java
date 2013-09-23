/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import org.jboss.modules.Module;
import org.jboss.vfs.VirtualFile;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * DUP that handles undertow-handlers.conf
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
        if(module == null) {
            return;
        }
        final List<PredicatedHandler> handlerWrappers = new ArrayList<>();
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
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
