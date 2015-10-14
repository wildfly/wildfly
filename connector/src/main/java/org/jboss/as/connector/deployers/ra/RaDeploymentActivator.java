/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.deployers.ra;

import org.jboss.as.connector.deployers.ds.processors.DriverProcessor;
import org.jboss.as.connector.deployers.ds.processors.StructureDriverProcessor;
import org.jboss.as.connector.deployers.ra.processors.IronJacamarDeploymentParsingProcessor;
import org.jboss.as.connector.deployers.ra.processors.ParsedRaDeploymentProcessor;
import org.jboss.as.connector.deployers.ra.processors.RaDeploymentParsingProcessor;
import org.jboss.as.connector.deployers.ra.processors.RaNativeProcessor;
import org.jboss.as.connector.deployers.ra.processors.RaStructureProcessor;
import org.jboss.as.connector.deployers.ra.processors.RaXmlDependencyProcessor;
import org.jboss.as.connector.deployers.ra.processors.RaXmlDeploymentProcessor;
import org.jboss.as.connector.deployers.ra.processors.RarDependencyProcessor;
import org.jboss.as.connector.services.mdr.MdrService;
import org.jboss.as.connector.services.rarepository.NonJTADataSourceRaRepositoryService;
import org.jboss.as.connector.services.rarepository.RaRepositoryService;
import org.jboss.as.connector.services.resourceadapters.deployment.registry.ResourceAdapterDeploymentRegistryService;
import org.jboss.as.connector.services.resourceadapters.repository.ManagementRepositoryService;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.msc.service.ServiceTarget;

/**
 * Service activator which installs the various service required for rar
 * deployments.
 *
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class RaDeploymentActivator {
    private final boolean appclient;
    private final MdrService mdrService = new MdrService();

    public RaDeploymentActivator(final boolean appclient) {
        this.appclient = appclient;
    }

    public void activateServices(final ServiceTarget serviceTarget) {
        // add resources here

        serviceTarget.addService(ConnectorServices.IRONJACAMAR_MDR, mdrService)
                .install();

        RaRepositoryService raRepositoryService = new RaRepositoryService();
        serviceTarget.addService(ConnectorServices.RA_REPOSITORY_SERVICE, raRepositoryService)
                .addDependency(ConnectorServices.IRONJACAMAR_MDR, MetadataRepository.class, raRepositoryService.getMdrInjector())
                .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class,
                        raRepositoryService.getTransactionIntegrationInjector())
                .install();

        // Special resource adapter repository and bootstrap context for non-JTA datasources

        NonJTADataSourceRaRepositoryService nonJTADataSourceRaRepositoryService = new NonJTADataSourceRaRepositoryService();
        serviceTarget.addService(ConnectorServices.NON_JTA_DS_RA_REPOSITORY_SERVICE, nonJTADataSourceRaRepositoryService)
                .addDependency(ConnectorServices.IRONJACAMAR_MDR, MetadataRepository.class, nonJTADataSourceRaRepositoryService.getMdrInjector())
                .install();

        ManagementRepositoryService managementRepositoryService = new ManagementRepositoryService();
        serviceTarget.addService(ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE, managementRepositoryService)
                .install();

        ResourceAdapterDeploymentRegistryService registryService = new ResourceAdapterDeploymentRegistryService();
        serviceTarget.addService(ConnectorServices.RESOURCE_ADAPTER_REGISTRY_SERVICE, registryService)
                .addDependency(ConnectorServices.IRONJACAMAR_MDR)
                .install();
    }


    public void activateProcessors(final DeploymentProcessorTarget updateContext) {
        updateContext.addDeploymentProcessor(ResourceAdaptersExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_RAR, new RaStructureProcessor());
        updateContext.addDeploymentProcessor(ResourceAdaptersExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_JDBC_DRIVER, new StructureDriverProcessor());
        updateContext.addDeploymentProcessor(ResourceAdaptersExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_RA_DEPLOYMENT, new RaDeploymentParsingProcessor());
        updateContext.addDeploymentProcessor(ResourceAdaptersExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_IRON_JACAMAR_DEPLOYMENT,
                new IronJacamarDeploymentParsingProcessor());
        updateContext.addDeploymentProcessor(ResourceAdaptersExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_RESOURCE_DEF_ANNOTATION_CONNECTION_FACTORY,
                new ConnectionFactoryDefinitionAnnotationProcessor());
        updateContext.addDeploymentProcessor(ResourceAdaptersExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_RESOURCE_DEF_ANNOTATION_ADMINISTERED_OBJECT,
                new AdministeredObjectDefinitionAnnotationProcessor());
        updateContext.addDeploymentProcessor(ResourceAdaptersExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_RAR_CONFIG, new RarDependencyProcessor(appclient));
        if (!appclient)
            updateContext.addDeploymentProcessor(ResourceAdaptersExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_RAR_SERVICES_DEPS, new RaXmlDependencyProcessor());
        updateContext.addDeploymentProcessor(ResourceAdaptersExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_RESOURCE_DEF_XML_CONNECTION_FACTORY,
                new ConnectionFactoryDefinitionDescriptorProcessor());
        updateContext.addDeploymentProcessor(ResourceAdaptersExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_RESOURCE_DEF_XML_ADMINISTERED_OBJECT,
                new AdministeredObjectDefinitionDescriptorProcessor());
        updateContext.addDeploymentProcessor(ResourceAdaptersExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_RA_NATIVE, new RaNativeProcessor());
        updateContext.addDeploymentProcessor(ResourceAdaptersExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_RA_DEPLOYMENT, new ParsedRaDeploymentProcessor());
        updateContext.addDeploymentProcessor(ResourceAdaptersExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_RA_XML_DEPLOYMENT, new RaXmlDeploymentProcessor());
        updateContext.addDeploymentProcessor(ResourceAdaptersExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_JDBC_DRIVER, new DriverProcessor());
    }
}
