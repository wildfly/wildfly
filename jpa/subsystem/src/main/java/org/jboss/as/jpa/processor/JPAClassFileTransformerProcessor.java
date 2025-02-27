/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.processor;

import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.module.DelegatingClassTransformer;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * Deployment processor which ensures the persistence provider ClassFileTransformer are used.
 *
 * @author Scott Marlow
 */
public class JPAClassFileTransformerProcessor implements DeploymentUnitProcessor {

    public JPAClassFileTransformerProcessor() {
    }

    /**
     * Add dependencies for modules required for Jakarta Persistence deployments
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        setClassLoaderTransformer(deploymentUnit);
    }

    private void setClassLoaderTransformer(DeploymentUnit deploymentUnit) {
        // (AS7-2233) each persistence unit can use a persistence provider, that might need
        // to use ClassTransformers.  Providers that need class transformers will add them
        // during the call to CreateContainerEntityManagerFactory.

        DelegatingClassTransformer transformer = deploymentUnit.getAttachment(DelegatingClassTransformer.ATTACHMENT_KEY);
        if ( transformer != null) {

            for (ResourceRoot resourceRoot : DeploymentUtils.allResourceRoots(deploymentUnit)) {
                PersistenceUnitMetadataHolder holder = resourceRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
                if (holder != null) {
                    for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                        if (pu.needsJPADelegatingClassFileTransformer()) {
                            transformer.addTransformer(new JPADelegatingClassFileTransformer(pu));
                        }
                    }
                }
            }
        }
    }
}
