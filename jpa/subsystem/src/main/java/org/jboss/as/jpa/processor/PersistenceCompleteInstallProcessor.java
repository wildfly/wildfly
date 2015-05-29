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

package org.jboss.as.jpa.processor;

import org.jboss.as.jpa.config.PersistenceProviderDeploymentHolder;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jipijapa.plugin.spi.Platform;

/**
 * complete installation of persistence unit service and persistence providers in deployment
 *
 * @author Scott Marlow
 */
public class PersistenceCompleteInstallProcessor implements DeploymentUnitProcessor {

    private final Platform platform;

    public PersistenceCompleteInstallProcessor(Platform platform) {
        this.platform = platform;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        if (deploymentHasPersistenceProvider(phaseContext.getDeploymentUnit())) {
            // finish registration of persistence provider
            PersistenceProviderHandler.finishDeploy(phaseContext);
        }

        // only install PUs with property Configuration.JPA_CONTAINER_CLASS_TRANSFORMER = false (since they weren't started before)
        // this allows @DataSourceDefinition to work (which don't start until the Install phase)
        PersistenceUnitServiceHandler.deploy(phaseContext, false, platform);

    }

    @Override
    public void undeploy(DeploymentUnit context) {
        PersistenceUnitServiceHandler.undeploy(context); // always uninstall persistent unit services from here
    }

    private static boolean deploymentHasPersistenceProvider(DeploymentUnit deploymentUnit) {
        deploymentUnit = DeploymentUtils.getTopDeploymentUnit(deploymentUnit);
        PersistenceProviderDeploymentHolder persistenceProviderDeploymentHolder =  deploymentUnit.getAttachment(JpaAttachments.DEPLOYED_PERSISTENCE_PROVIDER);
        return (persistenceProviderDeploymentHolder != null && persistenceProviderDeploymentHolder.getProviders() != null ? persistenceProviderDeploymentHolder.getProviders().size() > 0: false);
    }

}
