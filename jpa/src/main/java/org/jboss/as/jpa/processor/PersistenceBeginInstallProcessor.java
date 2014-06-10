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

import org.jboss.as.jpa.classloader.JPADelegatingClassFileTransformer;
import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.module.DelegatingClassFileTransformer;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
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

        setClassLoaderTransformer(phaseContext.getDeploymentUnit());

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

    private static void setClassLoaderTransformer(DeploymentUnit deploymentUnit) {
        // (AS7-2233) each persistence unit can use a persistence provider, that might need
        // to use ClassTransformers.  Providers that need class transformers will add them
        // during the call to CreateContainerEntityManagerFactory.

        DelegatingClassFileTransformer transformer = deploymentUnit.getAttachment(DelegatingClassFileTransformer.ATTACHMENT_KEY);

        if ( transformer != null) {
            for (ResourceRoot resourceRoot : DeploymentUtils.allResourceRoots(deploymentUnit)) {
                PersistenceUnitMetadataHolder holder = resourceRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
                if (holder != null) {
                    for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                        if (Configuration.needClassFileTransformer(pu)) {
                            transformer.addTransformer(new JPADelegatingClassFileTransformer(pu));
                        }
                    }
                }
            }
        }
    }

}

