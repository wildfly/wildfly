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
package org.wildfly.extension.picketlink.federation.deployment;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.wildfly.extension.picketlink.federation.service.PicketLinkFederationService;

import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * <p> {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} that configures a {@link PicketLinkFederationService}
 * associated with deployments.. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class FederationDeploymentProcessor implements DeploymentUnitProcessor {

    public static final Phase PHASE = Phase.INSTALL;
    public static final int PRIORITY = 1;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        PicketLinkFederationService federationService = deploymentUnit.getAttachment(FederationDependencyProcessor.DEPLOYMENT_ATTACHMENT_KEY);

        if (federationService != null) {
            ROOT_LOGGER.federationConfiguringDeployment(deploymentUnit.getName());

            federationService.configure(deploymentUnit);
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        // no-op
    }
}
