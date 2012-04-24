package org.jboss.as.connector.util;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.StatisticsDescriptionProvider;
import org.jboss.as.connector.StatisticsElementDescriptionProvider;
import org.jboss.as.connector.SubSystemExtensionDescriptionProvider;
import org.jboss.as.connector.mdr.AS7MetadataRepository;
import org.jboss.as.connector.metadata.deployment.ResourceAdapterXmlDeploymentService;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.pool.PoolMetrics;
import org.jboss.as.connector.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.subsystems.ClearStatisticsHandler;
import org.jboss.as.connector.subsystems.jca.JcaSubsystemConfiguration;
import org.jboss.as.connector.subsystems.resourceadapters.Constants;
import org.jboss.as.connector.subsystems.resourceadapters.IronJacamarResource;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.modules.Module;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.SubjectFactory;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class RaServicesFactory {
    public static void createDeploymentService(final ManagementResourceRegistration registration, ConnectorXmlDescriptor connectorXmlDescriptor, Module module, ServiceTarget serviceTarget, final String deploymentUnitName, String deployment, ResourceAdapter raxml, final Resource deploymentResource) {
        // Create the service
        ServiceName serviceName = ConnectorServices.registerDeployment(raxml.getArchive());

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
        builder.addListener(new AbstractServiceListener<Object>() {
            public void transition(final ServiceController<? extends Object> controller,
                                   final ServiceController.Transition transition) {
                switch (transition) {
                    case STARTING_to_UP: {

                        synchronized (registration) {
                            CommonDeployment deploymentMD = ((ResourceAdapterXmlDeploymentService) controller.getService()).getRaxmlDeployment();


                            if (deploymentMD.getConnectionManagers() != null) {
                                for (ConnectionManager cm : deploymentMD.getConnectionManagers()) {
                                    if (cm.getPool() != null) {
                                        StatisticsPlugin poolStats = cm.getPool().getStatistics();

                                        if (poolStats.getNames().size() != 0) {
                                            DescriptionProvider statsResourceDescriptionProvider = new StatisticsDescriptionProvider(ResourceAdaptersSubsystemProviders.RESOURCE_NAME, "statistics", poolStats);
                                            PathElement pe = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, ResourceAdaptersExtension.SUBSYSTEM_NAME);
                                            PathElement peStats = PathElement.pathElement(Constants.STATISTICS_NAME, Constants.STATISTICS_NAME);
                                            PathElement peCD = PathElement.pathElement(Constants.CONNECTIONDEFINITIONS_NAME, cm.getJndiName());

                                            ManagementResourceRegistration overrideRegistration = registration;
                                            //when you are in deploy you have a registration pointing to deployment=*
                                            //when you are in re-deploy it points to specific deploymentUnit
                                            if (registration.isAllowsOverride() && registration.getOverrideModel(deploymentUnitName) == null) {
                                                overrideRegistration = registration.registerOverrideModel(deploymentUnitName, new OverrideDescriptionProvider() {
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

                                            ManagementResourceRegistration subRegistration = overrideRegistration.getSubModel(PathAddress.pathAddress(pe));
                                            if (subRegistration == null) {
                                                subRegistration = overrideRegistration.registerSubModel(pe, new SubSystemExtensionDescriptionProvider(ResourceAdaptersSubsystemProviders.RESOURCE_NAME, "deployment-subsystem"));
                                            }
                                            final Resource subsystemResource = new IronJacamarResource.IronJacamarRuntimeResource();

                                            deploymentResource.registerChild(pe, subsystemResource);

                                            ManagementResourceRegistration statsRegistration = subRegistration.getSubModel(PathAddress.pathAddress(peStats));
                                            if (statsRegistration == null) {
                                                statsRegistration = subRegistration.registerSubModel(peStats, new StatisticsElementDescriptionProvider(ResourceAdaptersSubsystemProviders.RESOURCE_NAME, "statistics"));
                                            }
                                            final Resource statisticsResource = new IronJacamarResource.IronJacamarRuntimeResource();

                                            subsystemResource.registerChild(peStats, statisticsResource);

                                            if (statsRegistration.getSubModel(PathAddress.pathAddress(peCD)) == null) {
                                                ManagementResourceRegistration cdSubRegistration = statsRegistration.registerSubModel(peCD, statsResourceDescriptionProvider);
                                                final Resource cdResource = new IronJacamarResource.IronJacamarRuntimeResource();

                                                statisticsResource.registerChild(peCD, cdResource);

                                                for (String statName : poolStats.getNames()) {
                                                    cdSubRegistration.registerMetric(statName, new PoolMetrics.ParametrizedPoolMetricsHandler(poolStats));
                                                }
                                                cdSubRegistration.registerOperationHandler("clear-statistics", new ClearStatisticsHandler(poolStats), ResourceAdaptersSubsystemProviders.CLEAR_STATISTICS_DESC, false);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break;

                    }
                    case UP_to_STOP_REQUESTED: {

                        synchronized (registration) {
                            PathElement pe = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, ResourceAdaptersExtension.SUBSYSTEM_NAME);
                            ManagementResourceRegistration overrideRegistration = registration;
                            //when you are in deploy you have a registration pointing to deployment=*
                            //when you are in re-deploy it points to specific deploymentUnit
                            if (registration.isAllowsOverride() && registration.getOverrideModel(deploymentUnitName) == null) {
                                overrideRegistration = registration.getOverrideModel(deploymentUnitName);
                            }
                            if (overrideRegistration.getSubModel(PathAddress.pathAddress(pe)) != null) {
                                overrideRegistration.unregisterSubModel(pe);
                            }
                        }
                        break;

                    }

                }
            }
        });


        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }
}
