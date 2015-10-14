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

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jipijapa.plugin.spi.Platform;

/**
 * begin installation of persistence unit service and persistence providers in deployment
 *
 * @author Scott Marlow
 */
public class PersistenceBeginInstallProcessor implements DeploymentUnitProcessor {

    private final Platform platform;

    public PersistenceBeginInstallProcessor(Platform platform) {
        this.platform = platform;
    }
    /**
     * {@inheritDoc}
     */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        // deploy any persistence providers found in deployment
        PersistenceProviderHandler.deploy(phaseContext, platform);

        // start each PU service (except the PUs with property Configuration.JPA_CONTAINER_CLASS_TRANSFORMER = false)
        PersistenceUnitServiceHandler.deploy(phaseContext, true, platform);
    }

    /**
     * {@inheritDoc}
     */
    public void undeploy(final DeploymentUnit deploymentUnit) {
        PersistenceProviderHandler.undeploy(deploymentUnit);
    }

}

