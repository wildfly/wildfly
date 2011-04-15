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

package org.jboss.as.connector.deployers;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.deployers.processors.DriverProcessor;
import org.jboss.as.connector.deployers.processors.IronJacamarDeploymentParsingProcessor;
import org.jboss.as.connector.deployers.processors.ParsedRaDeploymentProcessor;
import org.jboss.as.connector.deployers.processors.RaDeploymentParsingProcessor;
import org.jboss.as.connector.deployers.processors.RaStructureProcessor;
import org.jboss.as.connector.deployers.processors.RaXmlDeploymentProcessor;
import org.jboss.as.connector.deployers.processors.RarDependencyProcessor;
import org.jboss.as.connector.mdr.MdrService;
import org.jboss.as.connector.rarepository.RaRepositoryService;
import org.jboss.as.connector.registry.ResourceAdapterDeploymentRegistryService;
import org.jboss.as.connector.services.ManagementRepositoryService;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.deployment.Phase;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.msc.service.ServiceTarget;

/**
 * Service activator which installs the various service required for rar
 * deployments.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class RaDeploymentActivator {
    /**
     * Activate the services required for service deployments.
     * @param updateContext The update context
     */
    public void activate(final BootOperationContext updateContext, final ServiceTarget serviceTarget) {
        // add resources here
        MdrService mdrService = new MdrService();
        serviceTarget.addService(ConnectorServices.IRONJACAMAR_MDR, mdrService).install();

        RaRepositoryService raRepositoryService = new RaRepositoryService();
        serviceTarget
                .addService(ConnectorServices.RA_REPOSISTORY_SERVICE, raRepositoryService)
                .addDependency(ConnectorServices.IRONJACAMAR_MDR, MetadataRepository.class,
                        raRepositoryService.getMdrInjector()).install();

        ManagementRepositoryService managementRepositoryService = new ManagementRepositoryService();
        serviceTarget.addService(ConnectorServices.MANAGEMENT_REPOSISTORY_SERVICE, managementRepositoryService).install();

        ResourceAdapterDeploymentRegistryService registryService = new ResourceAdapterDeploymentRegistryService();
        serviceTarget.addService(ConnectorServices.RESOURCE_ADAPTER_REGISTRY_SERVICE, registryService)
                .addDependency(ConnectorServices.IRONJACAMAR_MDR).install();

        updateContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_RAR_CONFIG, new RarDependencyProcessor());
        updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_RA_DEPLOYMENT, new RaDeploymentParsingProcessor());
        updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_IRON_JACAMAR_DEPLOYMENT,
                new IronJacamarDeploymentParsingProcessor());
        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_RA_DEPLOYMENT, new ParsedRaDeploymentProcessor());
        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_RA_XML_DEPLOYMENT, new RaXmlDeploymentProcessor(
                mdrService.getValue()));
        updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_RAR, new RaStructureProcessor());
        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_JDBC_DRIVER, new DriverProcessor());
    }
}
