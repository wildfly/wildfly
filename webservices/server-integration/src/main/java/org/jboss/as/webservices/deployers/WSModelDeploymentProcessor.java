/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.deployers;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.webservices.config.ServerConfigImpl;
import org.jboss.as.webservices.deployers.deployment.WSDeploymentBuilder;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.as.webservices.util.WSServices;

/**
 * This deployer initializes JBossWS deployment meta data.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class WSModelDeploymentProcessor extends TCCLDeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void internalDeploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        WSDeploymentBuilder.getInstance().build(unit);

        if (isWebServiceDeployment(unit)) { //note, this check works only after the WSDeploymentBuilder above has run
            ServerConfigImpl config = (ServerConfigImpl)phaseContext.getServiceRegistry().getRequiredService(WSServices.CONFIG_SERVICE).getValue();
            config.incrementWSDeploymentCount();
        }
    }

    @Override
    public void internalUndeploy(final DeploymentUnit context) {
        if (isWebServiceDeployment(context)) {
            ServerConfigImpl config = (ServerConfigImpl)context.getServiceRegistry().getRequiredService(WSServices.CONFIG_SERVICE).getValue();
            config.decrementWSDeploymentCount();
        }

        // Cleans up reference established by AbstractDeploymentModelBuilder#propagateAttachments
        context.removeAttachment(WSAttachmentKeys.DEPLOYMENT_KEY);
    }

    private static boolean isWebServiceDeployment(final DeploymentUnit unit) {
        return unit.getAttachment(WSAttachmentKeys.DEPLOYMENT_KEY) != null;
    }
}
