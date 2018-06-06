/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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


import java.util.List;

import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.config.PersistenceUnitsInApplication;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.JPADeploymentMarker;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * Deployment processor which adds a Hibernate Search module dependency.
 *
 * @author Scott Marlow
 */
public class HibernateSearchProcessor implements DeploymentUnitProcessor {

    private static final DotName SEARCH_INDEXED_ANNOTATION_NAME = DotName.createSimple("org.hibernate.search.annotations.Indexed");
    private static final ModuleIdentifier defaultSearchModule =
            ModuleIdentifier.fromString(Configuration.PROVIDER_MODULE_HIBERNATE_SEARCH);


    private static final String NONE = "none";
    private static final String IGNORE = "auto";  // if set to `auto`, will behave like not having set the property

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        if (JPADeploymentMarker.isJPADeployment(deploymentUnit)) {
            addSearchDependency(moduleSpecification, moduleLoader, deploymentUnit);
        }
    }


    private void addSearchDependency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
                                     DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {

        String searchModuleName = null;
        PersistenceUnitsInApplication persistenceUnitsInApplication = DeploymentUtils.getTopDeploymentUnit(deploymentUnit).getAttachment(PersistenceUnitsInApplication.PERSISTENCE_UNITS_IN_APPLICATION);
        for (PersistenceUnitMetadataHolder holder : persistenceUnitsInApplication.getPersistenceUnitHolders()) {
            for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                String providerModule = pu.getProperties().getProperty(Configuration.HIBERNATE_SEARCH_MODULE);
                if (providerModule != null) {
                    // one persistence unit specifying the Hibernate search module is allowed
                    if (searchModuleName == null) {
                        searchModuleName = providerModule;
                    }
                    // more than one persistence unit specifying different Hibernate search module names is not allowed
                    else if (!providerModule.equals(searchModuleName)) {
                        throw JpaLogger.ROOT_LOGGER.differentSearchModuleDependencies(deploymentUnit.getName(), searchModuleName, providerModule);
                    }
                }
            }
        }

        if (NONE.equals(searchModuleName)) {
            // Hibernate Search module will not be added to deployment
            ROOT_LOGGER.debugf("Not adding Hibernate Search dependency to deployment %s", deploymentUnit.getName());
            return;
        }

        // use Search module name specified in persistence unit definition
        if (searchModuleName != null && !IGNORE.equals(searchModuleName)) {
            ModuleIdentifier moduleIdentifier = ModuleIdentifier.fromString(searchModuleName);
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, moduleIdentifier, false, true, true, false));
            ROOT_LOGGER.debugf("added %s dependency to %s", moduleIdentifier, deploymentUnit.getName());
        } else {
            // add Hibernate Search module dependency if application is using the Hibernate Search Indexed annotation
            final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
            List<AnnotationInstance> annotations = index.getAnnotations(SEARCH_INDEXED_ANNOTATION_NAME);
            if (annotations != null && annotations.size() > 0) {
                moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, defaultSearchModule, false, true, true, false));
                ROOT_LOGGER.debugf("deployment %s contains %s annotation, added %s dependency", deploymentUnit.getName(), SEARCH_INDEXED_ANNOTATION_NAME, defaultSearchModule);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
