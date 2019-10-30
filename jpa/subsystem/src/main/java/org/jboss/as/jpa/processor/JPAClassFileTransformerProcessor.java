/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;


import org.jboss.as.hibernate.Hibernate51CompatibilityTransformer;
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
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Deployment processor which ensures the persistence provider ClassFileTransformer are used.
 *
 * @author Scott Marlow
 */
public class JPAClassFileTransformerProcessor implements DeploymentUnitProcessor {

    private final boolean hibernate51CompatibilityTransformer = Boolean.parseBoolean(
            WildFlySecurityManager.getPropertyPrivileged("Hibernate51CompatibilityTransformer","false"));

    public JPAClassFileTransformerProcessor() {
        if(hibernate51CompatibilityTransformer) {
            ROOT_LOGGER.hibernate51CompatibilityTransformerEnabled();
        }
    }

    /**
     * Add dependencies for modules required for JPA deployments
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        setClassLoaderTransformer(deploymentUnit);
    }

    private void setClassLoaderTransformer(DeploymentUnit deploymentUnit) {
        // (AS7-2233) each persistence unit can use a persistence provider, that might need
        // to use ClassTransformers.  Providers that need class transformers will add them
        // during the call to CreateContainerEntityManagerFactory.

        DelegatingClassFileTransformer transformer = deploymentUnit.getAttachment(DelegatingClassFileTransformer.ATTACHMENT_KEY);
        boolean appContainsPersistenceProviderJars = false;  // remove when we revert WFLY-10520
        if ( transformer != null) {

            for (ResourceRoot resourceRoot : DeploymentUtils.allResourceRoots(deploymentUnit)) {
                PersistenceUnitMetadataHolder holder = resourceRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
                if (holder != null) {
                    for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                        if (Configuration.needClassFileTransformer(pu)) {
                            transformer.addTransformer(new JPADelegatingClassFileTransformer(pu));
                        }
                        // remove this check when we revert WFLY-10520
                        String provider = pu.getProperties().getProperty(Configuration.PROVIDER_MODULE);
                        if (provider != null) {
                            if (provider.equals(Configuration.PROVIDER_MODULE_APPLICATION_SUPPLIED)) {
                                appContainsPersistenceProviderJars = true;
                            }
                        }

                    }
                }
            }
            // WFLY-10520 Add Hibernate ORM 5.3 compatibility transformer
            if(!appContainsPersistenceProviderJars && hibernate51CompatibilityTransformer) {
                transformer.addTransformer(Hibernate51CompatibilityTransformer.getInstance());
            }
        }
    }


    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
