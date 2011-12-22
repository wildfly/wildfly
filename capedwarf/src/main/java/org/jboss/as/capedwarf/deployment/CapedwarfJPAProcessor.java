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

package org.jboss.as.capedwarf.deployment;

import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Properties;

/**
 * Fix CapeDwarf JPA usage - atm we use Hibernate.
 * DataNucleus support is on the roadmap.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfJPAProcessor implements DeploymentUnitProcessor {

    static final String DIALECT_PROPERTY_KEY = "hibernate.dialect";
    static final String DEFAULT_DIALECT = "org.hibernate.dialect.H2Dialect";

    private Logger log = Logger.getLogger(getClass());

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (CapedwarfDeploymentMarker.isCapedwarfDeployment(unit) == false)
            return;

        final ResourceRoot deploymentRoot = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        modifyPersistenceInfo(deploymentRoot);

        final List<ResourceRoot> resourceRoots = unit.getAttachment(Attachments.RESOURCE_ROOTS);
        for (ResourceRoot rr : resourceRoots) {
            modifyPersistenceInfo(rr);
        }
    }

    protected void modifyPersistenceInfo(ResourceRoot resourceRoot) {
        final PersistenceUnitMetadataHolder holder = resourceRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
        if (holder != null) {
            final List<PersistenceUnitMetadata> pus = holder.getPersistenceUnits();
            if (pus != null && pus.isEmpty() == false) {
                for (PersistenceUnitMetadata pumd : pus) {
                    final String providerClassName = pumd.getPersistenceProviderClassName();
                    if (Configuration.getProviderModuleNameFromProviderClassName(providerClassName) == null) {
                        log.debug("Changing JPA configuration - " + providerClassName + " not yet supported.");
                        pumd.setPersistenceProviderClassName(Configuration.PROVIDER_CLASS_HIBERNATE); // TODO OGM usage
                        final Properties properties = pumd.getProperties();
                        if (properties.contains(DIALECT_PROPERTY_KEY) == false)
                            properties.put(DIALECT_PROPERTY_KEY, DEFAULT_DIALECT);
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
