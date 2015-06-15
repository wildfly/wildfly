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

package org.jboss.as.connector.services.resourceadapters;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.connector.dynamicresource.ClearStatisticsHandler;
import org.jboss.as.connector.dynamicresource.StatisticsResourceDefinition;
import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
import org.jboss.as.connector.services.mdr.AS7MetadataRepository;
import org.jboss.as.connector.subsystems.common.pool.PoolMetrics;
import org.jboss.as.connector.subsystems.resourceadapters.CommonAttributes;
import org.jboss.as.connector.subsystems.resourceadapters.Constants;
import org.jboss.as.connector.subsystems.resourceadapters.IronJacamarResource;
import org.jboss.as.connector.subsystems.resourceadapters.IronJacamarResourceCreator;
import org.jboss.as.connector.subsystems.resourceadapters.IronJacamarResourceDefinition;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.management.AdminObject;
import org.jboss.jca.core.api.management.ConnectionFactory;
import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

public class IronJacamarActivationResourceService implements Service<ManagementResourceRegistration> {

    private static PathElement SUBSYSTEM_PATH_ELEMENT = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, ResourceAdaptersExtension.SUBSYSTEM_NAME);
    private static PathElement IJ_PATH_ELEMENT = PathElement.pathElement(Constants.IRONJACAMAR_NAME, Constants.IRONJACAMAR_NAME);


    private final ManagementResourceRegistration registration;
    private final Resource deploymentResource;
    private final boolean statsEnabled;

    protected final InjectedValue<ResourceAdapterDeployment> deployment = new InjectedValue<>();

    protected final InjectedValue<AS7MetadataRepository> mdr = new InjectedValue<AS7MetadataRepository>();

    /**
     * create an instance *
     */
    public IronJacamarActivationResourceService(final ManagementResourceRegistration registration, final Resource deploymentResource,
                                                final boolean statsEnabled) {
        super();
        this.registration = registration;
        this.deploymentResource = deploymentResource;
        this.statsEnabled = statsEnabled;
    }


    @Override
    public void start(StartContext context) throws StartException {
        ManagementResourceRegistration subRegistration;
        final CommonDeployment deploymentMD = deployment.getValue().getDeployment();
        ROOT_LOGGER.infof("Starting IronJacamarActivationResourceService %s", deploymentMD.getDeploymentName());

        try {
            ResourceBuilder resourceBuilder = ResourceBuilder.Factory.create(SUBSYSTEM_PATH_ELEMENT,
                    new StandardResourceDescriptionResolver(Constants.STATISTICS_NAME, CommonAttributes.RESOURCE_NAME, CommonAttributes.class.getClassLoader()));
            subRegistration = registration.registerSubModel(resourceBuilder.build());

        } catch (IllegalArgumentException iae) {
            subRegistration = registration.getSubModel(PathAddress.pathAddress(SUBSYSTEM_PATH_ELEMENT));
        }
        ManagementResourceRegistration ijRegistration;

        try {
            ijRegistration = subRegistration.registerSubModel(new IronJacamarResourceDefinition());

        } catch (IllegalArgumentException iae) {
            ijRegistration = subRegistration.getSubModel(PathAddress.pathAddress(Constants.IRONJACAMAR_NAME, Constants.IRONJACAMAR_NAME));

        }

        try {
            if (deploymentResource != null) {


                PathElement peLocalStats = PathElement.pathElement(Constants.STATISTICS_NAME, "extended");

                if (deploymentMD.getConnector() != null && deploymentMD.getConnector().getResourceAdapter() != null) {
                    ManagementResourceRegistration raRegistration = ijRegistration.
                            getSubModel(PathAddress.EMPTY_ADDRESS.append(PathElement.pathElement(Constants.RESOURCEADAPTER_NAME)))
                            .registerOverrideModel(deploymentMD.getDeploymentName(), new OverrideDescriptionProvider() {
                                @Override
                                public Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale) {
                                    return Collections.emptyMap();
                                }

                                @Override
                                public Map<String, ModelNode> getChildTypeOverrideDescriptions(Locale locale) {
                                    return Collections.emptyMap();
                                }

                            });

                    ResourceBuilder resourceBuilder = ResourceBuilder.Factory.create(peLocalStats,
                            new StandardResourceDescriptionResolver(Constants.STATISTICS_NAME + "." + Constants.WORKMANAGER_NAME, CommonAttributes.RESOURCE_NAME, CommonAttributes.class.getClassLoader()));

                    ManagementResourceRegistration raStatsSubRegistration = raRegistration.registerSubModel(resourceBuilder.build());


                    if (deploymentMD.getConnector().getResourceAdapter().getStatistics() != null) {
                        StatisticsPlugin raStats = deploymentMD.getConnector().getResourceAdapter().getStatistics();
                        raStats.setEnabled(statsEnabled);
                        PoolMetrics.ParametrizedPoolMetricsHandler handler = new PoolMetrics.ParametrizedPoolMetricsHandler(raStats);
                        for (AttributeDefinition attribute : StatisticsResourceDefinition.getAttributesFromPlugin(raStats)) {
                            raStatsSubRegistration.registerMetric(attribute, handler);
                        }
                        raStatsSubRegistration.registerOperationHandler(ClearStatisticsHandler.DEFINITION, new ClearStatisticsHandler(raStats));
                    }
                    if (deploymentMD.getConnector() != null && deploymentMD.getConnector().getConnectionFactories() != null) {
                        for (ConnectionFactory cf : deploymentMD.getConnector().getConnectionFactories()) {
                            if (cf.getManagedConnectionFactory() != null && cf.getManagedConnectionFactory().getStatistics() != null) {
                                PathElement peCD = PathElement.pathElement(Constants.CONNECTIONDEFINITIONS_NAME, cf.getJndiName());
                                PathElement peCdStats = PathElement.pathElement(Constants.STATISTICS_NAME, "extended");
                                StatisticsPlugin extendStats = cf.getManagedConnectionFactory().getStatistics();
                                extendStats.setEnabled(statsEnabled);
                                if (extendStats.getNames().size() != 0) {


                                    if (extendStats.getNames().size() != 0) {
                                        ManagementResourceRegistration cdRegistration = raRegistration.getSubModel(PathAddress.pathAddress(peCD));
                                        ManagementResourceRegistration overrideCdRegistration = cdRegistration.registerOverrideModel(cf.getJndiName(), new OverrideDescriptionProvider() {
                                            @Override
                                            public Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale) {
                                                return Collections.emptyMap();
                                            }

                                            @Override
                                            public Map<String, ModelNode> getChildTypeOverrideDescriptions(Locale locale) {
                                                return Collections.emptyMap();
                                            }

                                        });
                                        if (extendStats.getNames().size() != 0 && overrideCdRegistration.getSubModel(PathAddress.pathAddress(peCdStats)) == null) {
                                            overrideCdRegistration.registerSubModel(new StatisticsResourceDefinition(peCdStats, CommonAttributes.RESOURCE_NAME, extendStats));
                                        }


                                    }
                                }
                            }
                        }
                    }

                    if (deploymentMD.getConnectionManagers() != null) {
                        for (ConnectionManager cm : deploymentMD.getConnectionManagers()) {
                            if (cm.getPool() != null) {
                                PathElement peCD = PathElement.pathElement(Constants.CONNECTIONDEFINITIONS_NAME, cm.getJndiName());
                                PathElement peCdStats = PathElement.pathElement(Constants.STATISTICS_NAME, "pool");
                                StatisticsPlugin poolStats = cm.getPool().getStatistics();
                                poolStats.setEnabled(statsEnabled);

                                if (poolStats.getNames().size() != 0) {
                                    ManagementResourceRegistration cdRegistration = raRegistration.getSubModel(PathAddress.pathAddress(peCD));
                                    ManagementResourceRegistration overrideCdRegistration = cdRegistration.registerOverrideModel(cm.getJndiName(), new OverrideDescriptionProvider() {
                                        @Override
                                        public Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale) {
                                            return Collections.emptyMap();
                                        }

                                        @Override
                                        public Map<String, ModelNode> getChildTypeOverrideDescriptions(Locale locale) {
                                            return Collections.emptyMap();
                                        }

                                    });
                                    if (poolStats.getNames().size() != 0 && overrideCdRegistration.getSubModel(PathAddress.pathAddress(peCdStats)) == null) {
                                        overrideCdRegistration.registerSubModel(new StatisticsResourceDefinition(peCdStats, CommonAttributes.RESOURCE_NAME, poolStats));
                                    }


                                }
                            }
                        }
                    }

                    if (deploymentMD.getConnector() != null && deploymentMD.getConnector().getAdminObjects() != null) {
                        for (AdminObject ao : deploymentMD.getConnector().getAdminObjects()) {
                            if (ao.getStatistics() != null) {
                                PathElement peCD = PathElement.pathElement(Constants.ADMIN_OBJECTS_NAME, ao.getJndiName());
                                PathElement peCdStats = PathElement.pathElement(Constants.STATISTICS_NAME, "extended");
                                StatisticsPlugin extendStats = ao.getStatistics();
                                extendStats.setEnabled(statsEnabled);
                                if (extendStats.getNames().size() != 0) {


                                    if (extendStats.getNames().size() != 0) {
                                        ManagementResourceRegistration cdRegistration = raRegistration.getSubModel(PathAddress.pathAddress(peCD));
                                        ManagementResourceRegistration overrideCdRegistration = cdRegistration.registerOverrideModel(ao.getJndiName(), new OverrideDescriptionProvider() {
                                            @Override
                                            public Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale) {
                                                return Collections.emptyMap();
                                            }

                                            @Override
                                            public Map<String, ModelNode> getChildTypeOverrideDescriptions(Locale locale) {
                                                return Collections.emptyMap();
                                            }

                                        });
                                        if (extendStats.getNames().size() != 0 && overrideCdRegistration.getSubModel(PathAddress.pathAddress(peCdStats)) == null) {
                                            overrideCdRegistration.registerSubModel(new StatisticsResourceDefinition(peCdStats, CommonAttributes.RESOURCE_NAME, extendStats));
                                        }


                                    }
                                }
                            }
                        }
                    }

                }
            }
        } catch (IllegalArgumentException e) {
            //ignore it, already restered
        }
        Resource subsystemResource;

        if (!deploymentResource.hasChild(SUBSYSTEM_PATH_ELEMENT)) {
            subsystemResource = new IronJacamarResource.IronJacamarRuntimeResource();
            deploymentResource.registerChild(SUBSYSTEM_PATH_ELEMENT, subsystemResource);
        } else {
            subsystemResource = deploymentResource.getChild(SUBSYSTEM_PATH_ELEMENT);
        }


        IronJacamarResourceCreator.INSTANCE.execute(subsystemResource, mdr.getValue());


    }

    @Override
    public void stop(StopContext context) {
        ManagementResourceRegistration subsystemResourceRegistration;

        subsystemResourceRegistration = registration.getSubModel(PathAddress.pathAddress(SUBSYSTEM_PATH_ELEMENT));

        if (subsystemResourceRegistration != null) {
            if (subsystemResourceRegistration.getSubModel(PathAddress.pathAddress(IJ_PATH_ELEMENT)) != null) {
                subsystemResourceRegistration.unregisterSubModel(IJ_PATH_ELEMENT);
            }
            registration.unregisterSubModel(SUBSYSTEM_PATH_ELEMENT);
        }
        deploymentResource.removeChild(SUBSYSTEM_PATH_ELEMENT);
    }


    @Override
    public ManagementResourceRegistration getValue() throws IllegalStateException, IllegalArgumentException {
        return registration;
    }

    public Injector<AS7MetadataRepository> getMdrInjector() {
        return mdr;
    }

    public Injector<ResourceAdapterDeployment> getResourceAdapterDeploymentInjector() {
        return deployment;
    }

}
