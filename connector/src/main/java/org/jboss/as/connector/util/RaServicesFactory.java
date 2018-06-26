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

package org.jboss.as.connector.util;

import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_NAME;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.services.mdr.AS7MetadataRepository;
import org.jboss.as.connector.services.resourceadapters.deployment.ResourceAdapterXmlDeploymentService;
import org.jboss.as.connector.services.resourceadapters.deployment.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.subsystems.jca.JcaSubsystemConfiguration;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

public class RaServicesFactory {

    public static void createDeploymentService(final ManagementResourceRegistration registration, ConnectorXmlDescriptor connectorXmlDescriptor, Module module,
                                               ServiceTarget serviceTarget, final String deploymentUnitName, ServiceName deploymentUnitServiceName, String deployment,
                                               Activation raxml, final Resource deploymentResource, final ServiceRegistry serviceRegistry) {
        // Create the service

        ServiceName serviceName = ConnectorServices.getDeploymentServiceName(deploymentUnitName,raxml);
        ResourceAdapterXmlDeploymentService service = new ResourceAdapterXmlDeploymentService(connectorXmlDescriptor,
                raxml, module, deployment, serviceName, deploymentUnitServiceName);
        String bootStrapCtxName = DEFAULT_NAME;
        if (raxml.getBootstrapContext() != null && !raxml.getBootstrapContext().equals("undefined")) {
            bootStrapCtxName = raxml.getBootstrapContext();
        }
        ServiceBuilder<ResourceAdapterDeployment> builder =
                Services.addServerExecutorDependency(
                        serviceTarget.addService(serviceName, service),
                        service.getExecutorServiceInjector())
                .addDependency(ConnectorServices.IRONJACAMAR_MDR, AS7MetadataRepository.class, service.getMdrInjector())
                .addDependency(ConnectorServices.RA_REPOSITORY_SERVICE, ResourceAdapterRepository.class,
                        service.getRaRepositoryInjector())
                .addDependency(ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE, ManagementRepository.class,
                        service.getManagementRepositoryInjector())
                .addDependency(ConnectorServices.RESOURCE_ADAPTER_REGISTRY_SERVICE,
                        ResourceAdapterDeploymentRegistry.class, service.getRegistryInjector())
                .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class,
                        service.getTxIntegrationInjector())
                .addDependency(ConnectorServices.CONNECTOR_CONFIG_SERVICE, JcaSubsystemConfiguration.class,
                        service.getConfigInjector())
                .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class, service.getCcmInjector())
                .addDependency(ConnectorServices.IDLE_REMOVER_SERVICE)
                .addDependency(ConnectorServices.CONNECTION_VALIDATOR_SERVICE)
                .addDependency(NamingService.SERVICE_NAME)
                .addDependency(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append(bootStrapCtxName))
                .addDependency(ConnectorServices.RESOURCE_ADAPTER_DEPLOYER_SERVICE_PREFIX.append(connectorXmlDescriptor.getDeploymentName()));

        String raName = deployment;
        if (raxml.getId() != null) {
            raName = raxml.getId();
        }
        ServiceName parentName = ServiceName.of(ConnectorServices.RA_SERVICE, raName);
        for(ServiceName subServiceName: serviceRegistry.getServiceNames()) {
            if (parentName.isParentOf(subServiceName)
                    && !subServiceName.getSimpleName().equals(ConnectorServices.STATISTICS_SUFFIX)) {
                builder.addDependency(subServiceName);
            }
        }

        if (registration != null && deploymentResource != null) {
            if (registration.isAllowsOverride() && registration.getOverrideModel(deploymentUnitName) == null) {
                registration.registerOverrideModel(deploymentUnitName, new OverrideDescriptionProvider() {
                    @Override
                    public Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale) {
                        return Collections.emptyMap();
                    }

                    @Override
                    public Map<String, ModelNode> getChildTypeOverrideDescriptions(Locale locale) {
                        return Collections.emptyMap();
                    }
                });
            }
        }



        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();

    }
}
