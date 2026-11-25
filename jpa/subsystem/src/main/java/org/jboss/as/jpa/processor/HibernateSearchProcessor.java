/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.processor;

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
import org.jboss.modules.ModuleLoader;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

import java.util.List;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

/**
 * Deployment processor which adds a Hibernate Search module dependency.
 *
 * @author Scott Marlow
 */
public class HibernateSearchProcessor implements DeploymentUnitProcessor {

    private static final DotName ANNOTATION_INDEXED_NAME = DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed");
    private static final String NONE = "none";
    private static final String IGNORE = "auto";  // if set to `auto`, will behave like not having set the property

    @Override
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

        String searchMapperModuleName = null;
        PersistenceUnitsInApplication persistenceUnitsInApplication = DeploymentUtils.getTopDeploymentUnit(deploymentUnit).getAttachment(PersistenceUnitsInApplication.PERSISTENCE_UNITS_IN_APPLICATION);
        for (PersistenceUnitMetadataHolder holder : persistenceUnitsInApplication.getPersistenceUnitHolders()) {
            for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                String providerModule = pu.getProperties().getProperty(Configuration.HIBERNATE_SEARCH_MODULE);
                if (providerModule != null) {
                    // one persistence unit specifying the Hibernate search module is allowed
                    if (searchMapperModuleName == null) {
                        searchMapperModuleName = providerModule;
                    }
                    // more than one persistence unit specifying different Hibernate search module names is not allowed
                    else if (!providerModule.equals(searchMapperModuleName)) {
                        throw JpaLogger.ROOT_LOGGER.differentSearchModuleDependencies(deploymentUnit.getName(), searchMapperModuleName, providerModule);
                    }
                }
            }
        }

        if (NONE.equals(searchMapperModuleName)) {
            // Hibernate Search module will not be added to deployment
            ROOT_LOGGER.debugf("Not adding Hibernate Search dependency to deployment %s", deploymentUnit.getName());
            return;
        }

        // use Search module name specified in persistence unit definition
        if (searchMapperModuleName != null && !IGNORE.equals(searchMapperModuleName)) {
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, searchMapperModuleName).
                    setOptional(false).setExport(true).setImportServices(true).setUserSpecified(false).build());

            ROOT_LOGGER.debugf("added %s dependency to %s", searchMapperModuleName, deploymentUnit.getName());
        } else {
            // add Hibernate Search module dependency if application is using the Hibernate Search Indexed annotation
            final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
            List<AnnotationInstance> annotations = index.getAnnotations(ANNOTATION_INDEXED_NAME);
            if (annotations != null && !annotations.isEmpty()) {
                moduleSpecification.addSystemDependency(
                        ModuleDependency.Builder.of(moduleLoader, Configuration.HIBERNATE_SEARCH_MODULE_MAPPER_ORM).
                                setOptional(false).setExport(true).setImportServices(true).setUserSpecified(false).build());
                ROOT_LOGGER.debugf("deployment %s contains %s annotation, added %s dependency", deploymentUnit.getName(),
                        ANNOTATION_INDEXED_NAME, Configuration.HIBERNATE_SEARCH_MODULE_MAPPER_ORM);
            }
        }

        // Configure sourcing of Jandex indexes in Hibernate Search,
        // so that it can look for @ProjectionConstructor annotations
        deploymentUnit.addToAttachmentList(JpaAttachments.INTEGRATOR_ADAPTOR_MODULE_NAMES,
                Configuration.HIBERNATE_SEARCH_INTEGRATOR_ADAPTOR_MODULE_NAME);

        List<String> backendTypes = HibernateSearchDeploymentMarker.getBackendTypes(deploymentUnit);
        if (backendTypes != null) {
            if (backendTypes.contains(Configuration.HIBERNATE_SEARCH_BACKEND_TYPE_VALUE_LUCENE)) {
                moduleSpecification.addSystemDependency(
                        ModuleDependency.Builder.of(moduleLoader, Configuration.HIBERNATE_SEARCH_MODULE_BACKEND_LUCENE).
                                setOptional(false).setExport(true).setImportServices(true).setUserSpecified(false).build());
            }
            if (backendTypes.contains(Configuration.HIBERNATE_SEARCH_BACKEND_TYPE_VALUE_ELASTICSEARCH)) {
                moduleSpecification.addSystemDependency(
                        ModuleDependency.Builder.of(moduleLoader, Configuration.HIBERNATE_SEARCH_MODULE_BACKEND_ELASTICSEARCH).
                                        setOptional(false).setExport(true).setImportServices(true).setUserSpecified(false).build());
            }
        }

        List<String> coordinationStrategies = HibernateSearchDeploymentMarker.getCoordinationStrategies(deploymentUnit);
        if (coordinationStrategies != null) {
            if (coordinationStrategies.contains(Configuration.HIBERNATE_SEARCH_COORDINATION_STRATEGY_VALUE_OUTBOX_POLLING)) {
                moduleSpecification.addSystemDependency(
                        ModuleDependency.Builder.of(moduleLoader, Configuration.HIBERNATE_SEARCH_MODULE_MAPPER_ORM_OUTBOXPOLLING).
                                setOptional(false).setExport(true).setExport(true).setImportServices(true).setUserSpecified(false).build());
            }
        }
    }
}
