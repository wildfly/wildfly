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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.deployers.processors.DriverProcessor;
import org.jboss.as.connector.deployers.processors.IronJacamarDeploymentParsingProcessor;
import org.jboss.as.connector.deployers.processors.ParsedRaDeploymentProcessor;
import org.jboss.as.connector.deployers.processors.RaDeploymentParsingProcessor;
import org.jboss.as.connector.deployers.processors.RaNativeProcessor;
import org.jboss.as.connector.deployers.processors.RaStructureProcessor;
import org.jboss.as.connector.deployers.processors.RaXmlDeploymentProcessor;
import org.jboss.as.connector.deployers.processors.RarDependencyProcessor;
import org.jboss.as.connector.deployers.processors.StructureDriverProcessor;
import org.jboss.as.connector.mdr.MdrService;
import org.jboss.as.connector.rarepository.RaRepositoryService;
import org.jboss.as.connector.registry.ResourceAdapterDeploymentRegistryService;
import org.jboss.as.connector.services.ManagementRepositoryService;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceTarget;

/**
 * Service activator which installs the various service required for rar
 * deployments.
 *
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class RaDeploymentActivator {
    private final MdrService mdrService = new MdrService();

    public Collection<ServiceController<?>> activateServices(final ServiceTarget serviceTarget, final ServiceListener<Object>... listeners) {
        final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
        // add resources here

        controllers.add(serviceTarget.addService(ConnectorServices.IRONJACAMAR_MDR, mdrService)
            .addListener(listeners)
            .install());

        RaRepositoryService raRepositoryService = new RaRepositoryService();
        controllers.add(serviceTarget.addService(ConnectorServices.RA_REPOSITORY_SERVICE, raRepositoryService)
            .addDependency(ConnectorServices.IRONJACAMAR_MDR, MetadataRepository.class, raRepositoryService.getMdrInjector())
            .addListener(listeners)
            .install());

        ManagementRepositoryService managementRepositoryService = new ManagementRepositoryService();
        controllers.add(serviceTarget.addService(ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE, managementRepositoryService)
            .addListener(listeners)
            .install());

        ResourceAdapterDeploymentRegistryService registryService = new ResourceAdapterDeploymentRegistryService();
        controllers.add(serviceTarget.addService(ConnectorServices.RESOURCE_ADAPTER_REGISTRY_SERVICE, registryService)
            .addDependency(ConnectorServices.IRONJACAMAR_MDR)
            .addListener(listeners)
            .install());

        return controllers;
    }


    public void activateProcessors(final DeploymentProcessorTarget updateContext) {
        updateContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_RAR_CONFIG, new RarDependencyProcessor());
        updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_RA_DEPLOYMENT, new RaDeploymentParsingProcessor());
        updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_IRON_JACAMAR_DEPLOYMENT,
                new IronJacamarDeploymentParsingProcessor());
        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_RA_NATIVE, new RaNativeProcessor());
        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_RA_DEPLOYMENT, new ParsedRaDeploymentProcessor());
        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_RA_XML_DEPLOYMENT, new RaXmlDeploymentProcessor(
                mdrService.getValue()));
        updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_RAR, new RaStructureProcessor());
        updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_JDBC_DRIVER, new StructureDriverProcessor());
        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_JDBC_DRIVER, new DriverProcessor());
    }
}
