package org.jboss.as.connector.util;

import org.jboss.as.connector.deployers.ra.processors.AbstractResourceAdapterDeploymentServiceListener;
import org.jboss.as.connector.services.mdr.AS7MetadataRepository;
import org.jboss.as.connector.services.resourceadapters.deployment.ResourceAdapterXmlDeploymentService;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.services.resourceadapters.deployment.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.subsystems.jca.JcaSubsystemConfiguration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.SubjectFactory;

public class RaServicesFactory {
    public static void createDeploymentService(final ManagementResourceRegistration registration, ConnectorXmlDescriptor connectorXmlDescriptor, Module module, ServiceTarget serviceTarget, final String deploymentUnitName, String deployment, ResourceAdapter raxml, final Resource deploymentResource) {
        // Create the service
        ServiceName serviceName = ConnectorServices.registerDeployment(deploymentUnitName);

        ResourceAdapterXmlDeploymentService service = new ResourceAdapterXmlDeploymentService(connectorXmlDescriptor,
                raxml, module, deployment, serviceName);

        ServiceBuilder builder = serviceTarget
                .addService(serviceName, service)
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
                .addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class,
                        service.getSubjectFactoryInjector())
                .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class, service.getCcmInjector())
                .addDependency(ConnectorServices.IDLE_REMOVER_SERVICE)
                .addDependency(ConnectorServices.CONNECTION_VALIDATOR_SERVICE)
                .addDependency(NamingService.SERVICE_NAME)
                .addDependency(ConnectorServices.RESOURCE_ADAPTER_DEPLOYER_SERVICE_PREFIX.append(connectorXmlDescriptor.getDeploymentName()));
        builder.addListener(new AbstractResourceAdapterDeploymentServiceListener(registration, deploymentUnitName, deploymentResource) {

            @Override
            protected void registerIronjacamar(ServiceController<? extends Object> controller, ManagementResourceRegistration subRegistration, Resource subsystemResource) {
                //do nothing, no ironjacamar registration for raxml activated ra
            }

            @Override
            protected CommonDeployment getDeploymentMetadata(final ServiceController<? extends Object> controller) {
                return ((ResourceAdapterXmlDeploymentService) controller.getService()).getRaxmlDeployment();
            }
        });

        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }
}
