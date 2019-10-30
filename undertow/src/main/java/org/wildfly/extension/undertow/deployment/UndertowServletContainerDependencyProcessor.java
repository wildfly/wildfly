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

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.wildfly.extension.undertow.UndertowService;

/**
 * @author Stuart Douglas
 */
public class UndertowServletContainerDependencyProcessor implements DeploymentUnitProcessor {

    private final String defaultServletContainer;

    public UndertowServletContainerDependencyProcessor(String defaultContainer) {
        this.defaultServletContainer = defaultContainer;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData != null) {
            String servletContainerName = defaultServletContainer;
            final JBossWebMetaData metaData = warMetaData.getMergedJBossWebMetaData();
            if(metaData != null && metaData.getServletContainerName() != null) {
                servletContainerName = metaData.getServletContainerName();
            }
            phaseContext.addDeploymentDependency(UndertowService.SERVLET_CONTAINER.append(servletContainerName), UndertowAttachments.SERVLET_CONTAINER_SERVICE);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
