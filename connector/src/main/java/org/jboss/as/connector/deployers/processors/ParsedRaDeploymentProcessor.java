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

package org.jboss.as.connector.deployers.processors;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.StatisticsDescriptionProvider;
import org.jboss.as.connector.StatisticsElementDescriptionProvider;
import org.jboss.as.connector.SubSystemExtensionDescriptionProvider;
import org.jboss.as.connector.annotations.repository.jandex.JandexAnnotationRepositoryImpl;
import org.jboss.as.connector.mdr.AS7MetadataRepository;
import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeploymentService;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.metadata.xmldescriptors.IronJacamarXmlDescriptor;
import org.jboss.as.connector.pool.PoolMetrics;
import org.jboss.as.connector.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.subsystems.ClearStatisticsHandler;
import org.jboss.as.connector.subsystems.jca.JcaSubsystemConfiguration;
import org.jboss.as.connector.subsystems.resourceadapters.Constants;
import org.jboss.as.connector.subsystems.resourceadapters.IronJacamarRegistrator;
import org.jboss.as.connector.subsystems.resourceadapters.IronJacamarResource;
import org.jboss.as.connector.subsystems.resourceadapters.IronJacamarResourceCreator;
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
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentModelUtils;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.AnnotationIndexUtils;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.Index;
import org.jboss.jca.common.annotations.Annotations;
import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.common.metadata.merge.Merger;
import org.jboss.jca.common.spi.annotations.repository.AnnotationRepository;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.modules.Module;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.SubjectFactory;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import static org.jboss.as.connector.ConnectorMessages.MESSAGES;

/**
 * DeploymentUnitProcessor responsible for using IronJacamar metadata and create
 * service for ResourceAdapter.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class ParsedRaDeploymentProcessor implements DeploymentUnitProcessor {

    public ParsedRaDeploymentProcessor() {
    }

    /**
     * Process a deployment for a Connector. Will install a {@Code
     * JBossService} for this ResourceAdapter.
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final ConnectorXmlDescriptor connectorXmlDescriptor = phaseContext.getDeploymentUnit().getAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY);
        final ManagementResourceRegistration registration;
        final ManagementResourceRegistration baseRegistration = phaseContext.getDeploymentUnit().getAttachment(DeploymentModelUtils.MUTABLE_REGISTRATION_ATTACHMENT);
        final Resource deploymentResource = phaseContext.getDeploymentUnit().getAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE);

        if(connectorXmlDescriptor == null) {
            return;  // Skip non ra deployments
        }

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getParent() != null) {
            registration = baseRegistration.getSubModel(PathAddress.pathAddress(PathElement.pathElement("subdeployment")));
        }  else {
            registration = baseRegistration;
        }
        final IronJacamarXmlDescriptor ironJacamarXmlDescriptor = deploymentUnit
                .getAttachment(IronJacamarXmlDescriptor.ATTACHMENT_KEY);

        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null)
            throw MESSAGES.failedToGetModuleAttachment(phaseContext.getDeploymentUnit());

        final ClassLoader classLoader = module.getClassLoader();

        Connector cmd = connectorXmlDescriptor != null ? connectorXmlDescriptor.getConnector() : null;
        final IronJacamar ijmd = ironJacamarXmlDescriptor != null ? ironJacamarXmlDescriptor.getIronJacamar() : null;

        try {
            // Annotation merging
            Annotations annotator = new Annotations();
            Map<ResourceRoot, Index> indexes = AnnotationIndexUtils.getAnnotationIndexes(deploymentUnit);
            for (Entry<ResourceRoot, Index> entry : indexes.entrySet()) {
                AnnotationRepository repository = new JandexAnnotationRepositoryImpl(entry.getValue(), classLoader);
                    cmd = annotator.merge(cmd, repository, classLoader);
            }
            // FIXME: when the connector is null the Iron Jacamar data is ignored
            if (cmd != null) {
                // Validate metadata
                cmd.validate();

                // Merge metadata
                cmd = (new Merger()).mergeConnectorWithCommonIronJacamar(ijmd, cmd);
            }

            final ServiceName deployerServiceName = ConnectorServices.RESOURCE_ADAPTER_DEPLOYER_SERVICE_PREFIX.append(connectorXmlDescriptor.getDeploymentName());
            final ResourceAdapterDeploymentService raDeploymentService = new ResourceAdapterDeploymentService(connectorXmlDescriptor, cmd, ijmd, module, null);

            final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

            // Create the service
            ServiceBuilder builder = serviceTarget.addService(deployerServiceName, raDeploymentService)
                    .addDependency(ConnectorServices.IRONJACAMAR_MDR, AS7MetadataRepository.class, raDeploymentService.getMdrInjector())
                    .addDependency(ConnectorServices.RA_REPOSITORY_SERVICE, ResourceAdapterRepository.class, raDeploymentService.getRaRepositoryInjector())
                    .addDependency(ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE, ManagementRepository.class, raDeploymentService.getManagementRepositoryInjector())
                    .addDependency(ConnectorServices.RESOURCE_ADAPTER_REGISTRY_SERVICE, ResourceAdapterDeploymentRegistry.class, raDeploymentService.getRegistryInjector())
                    .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class, raDeploymentService.getTxIntegrationInjector())
                    .addDependency(ConnectorServices.CONNECTOR_CONFIG_SERVICE, JcaSubsystemConfiguration.class, raDeploymentService.getConfigInjector())
                    .addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class, raDeploymentService.getSubjectFactoryInjector())
                    .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class, raDeploymentService.getCcmInjector())
                    .addDependency(ConnectorServices.IDLE_REMOVER_SERVICE)
                    .addDependency(ConnectorServices.CONNECTION_VALIDATOR_SERVICE)
                    .addDependency(NamingService.SERVICE_NAME);
            builder.addListener(new AbstractServiceListener<Object>() {
                public void transition(final ServiceController<? extends Object> controller,
                                       final ServiceController.Transition transition) {
                    switch (transition) {
                        case STARTING_to_UP: {

                            CommonDeployment deploymentMD = ((ResourceAdapterDeploymentService) controller.getService()).getRaDeployment();

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
                                            if (registration.isAllowsOverride()) {
                                                overrideRegistration = registration.registerOverrideModel(deploymentUnit.getName(), new OverrideDescriptionProvider() {
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

                                            //register ironJacamar
                                            new IronJacamarRegistrator(subRegistration).invoke();
                                            AS7MetadataRepository mdr = ((ResourceAdapterDeploymentService) controller.getService()).getMdr();
                                            IronJacamarResourceCreator.INSTANCE.execute(subsystemResource,  mdr);
                                        }
                                    }
                                }
                            }
                            break;

                        }
                        case UP_to_STOP_REQUESTED: {

                            PathElement pe = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, ResourceAdaptersExtension.SUBSYSTEM_NAME);
                            PathElement ijPe = PathElement.pathElement(Constants.IRONJACAMAR_NAME, Constants.IRONJACAMAR_NAME);
                            PathElement peStats = PathElement.pathElement(Constants.STATISTICS_NAME, Constants.STATISTICS_NAME);
                            PathElement peCD = PathElement.pathElement(Constants.CONNECTIONDEFINITIONS_NAME);

                            ManagementResourceRegistration overrideRegistration = registration;
                            //when you are in deploy you have a registration pointing to deployment=*
                            //when you are in re-deploy it points to specific deploymentUnit
                            if (registration.isAllowsOverride() && registration.getOverrideModel(deploymentUnit.getName()) != null) {
                                overrideRegistration = registration.getOverrideModel(deploymentUnit.getName());
                            }
                            ManagementResourceRegistration subsystemReg= overrideRegistration.getSubModel(PathAddress.pathAddress(pe));
                            if (subsystemReg != null) {
                                if(subsystemReg.getSubModel(PathAddress.pathAddress(ijPe)) != null) {
                                    subsystemReg.unregisterSubModel(ijPe);
                                }
                                ManagementResourceRegistration statsReg =  subsystemReg.getSubModel(PathAddress.pathAddress(peStats));
                                if(statsReg != null) {
                                    if(statsReg.getSubModel(PathAddress.pathAddress(peCD)) != null) {
                                        statsReg.unregisterSubModel(peCD);
                                    }
                                    subsystemReg.unregisterSubModel(peStats);
                                }
                                overrideRegistration.unregisterSubModel(pe);
                            }

                            deploymentResource.removeChild(pe);



                        }

                    }
                }
            });


            builder.setInitialMode(Mode.ACTIVE).install();
        } catch (Throwable t) {
            throw new DeploymentUnitProcessingException(t);
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }


}
