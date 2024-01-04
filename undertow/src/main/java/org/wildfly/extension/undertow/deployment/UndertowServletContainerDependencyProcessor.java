/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
}
