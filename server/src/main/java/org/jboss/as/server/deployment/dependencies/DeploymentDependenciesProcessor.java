/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.dependencies;

import org.jboss.as.server.DeployerChainAddHandler;
import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.ServerService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.Services;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXmlParserRegisteringProcessor;
import org.jboss.msc.service.ServiceName;

import javax.xml.namespace.QName;

/**
 * Processor that handles inter-deployment dependencies. If this deployment has a dependency specified on
 * another deployment then the next phase will be set to passive, and a dependency on the other deployment will
 * be added.
 *
 * @author Stuart Douglas
 */
public class DeploymentDependenciesProcessor implements DeploymentUnitProcessor {

    private static final QName ROOT_1_0 = new QName(DeploymentDependenciesParserV_1_0.NAMESPACE_1_0, "jboss-deployment-dependencies");

    public static void registerJBossXMLParsers() {
        DeployerChainAddHandler.addDeploymentProcessor(ServerService.SERVER_NAME, Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_XML_PARSER, new JBossAllXmlParserRegisteringProcessor<DeploymentDependencies>(ROOT_1_0, DeploymentDependencies.ATTACHMENT_KEY, DeploymentDependenciesParserV_1_0.INSTANCE));
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.hasAttachment(DeploymentDependencies.ATTACHMENT_KEY)) {
            if (deploymentUnit.getParent() != null) {
                ServerLogger.DEPLOYMENT_LOGGER.deploymentDependenciesAreATopLevelElement(deploymentUnit.getName());
            } else {
                processDependencies(phaseContext, deploymentUnit);
            }
        }

        if (deploymentUnit.getParent() != null) {
            DeploymentUnit parent = deploymentUnit.getParent();
            if (parent.hasAttachment(DeploymentDependencies.ATTACHMENT_KEY)) {
                processDependencies(phaseContext, parent);
            }
        }
    }

    private void processDependencies(final DeploymentPhaseContext phaseContext, final DeploymentUnit deploymentUnit) {
        final DeploymentDependencies deps = deploymentUnit.getAttachment(DeploymentDependencies.ATTACHMENT_KEY);
        if (!deps.getDependencies().isEmpty()) {
            phaseContext.putAttachment(Attachments.NEXT_PHASE_PASSIVE, true);
            for (final String deployment : deps.getDependencies()) {
                final ServiceName name = Services.deploymentUnitName(deployment);
                phaseContext.addToAttachmentList(Attachments.NEXT_PHASE_DEPS, name);
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }

}
