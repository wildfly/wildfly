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

package org.jboss.as.connector.deployers.ra.processors;

import org.jboss.as.connector.dynamicresource.StatisticsResourceDefinition;
import org.jboss.as.connector.dynamicresource.ClearWorkManagerStatisticsHandler;
import org.jboss.as.connector.subsystems.common.pool.PoolMetrics;
import org.jboss.as.connector.subsystems.common.pool.PoolStatisticsRuntimeAttributeReadHandler;
import org.jboss.as.connector.subsystems.common.pool.PoolStatisticsRuntimeAttributeWriteHandler;
import org.jboss.as.connector.subsystems.resourceadapters.CommonAttributes;
import org.jboss.as.connector.subsystems.resourceadapters.Constants;
import org.jboss.as.connector.subsystems.resourceadapters.IronJacamarResource;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension;
import org.jboss.as.connector.subsystems.resourceadapters.WorkManagerRuntimeAttributeReadHandler;
import org.jboss.as.connector.subsystems.resourceadapters.WorkManagerRuntimeAttributeWriteHandler;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.api.workmanager.DistributedWorkManager;
import org.jboss.jca.core.api.workmanager.WorkManager;
import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

/**
 * @author Stefano Maestri (c) 2011 Red Hat Inc.
 */
public abstract class AbstractResourceAdapterDeploymentServiceListener extends AbstractServiceListener<Object> {
    private final ManagementResourceRegistration registration;
    private final String deploymentUnitName;
    private final Resource deploymentResource;
    private final String bootstrapCtx;
    private final String raName;

    public AbstractResourceAdapterDeploymentServiceListener(ManagementResourceRegistration registration, String deploymentUnitName, Resource deploymentResource, final String bootstrapCtx, final String raName) {
        this.registration = registration;
        this.deploymentUnitName = deploymentUnitName;
        this.deploymentResource = deploymentResource;
        this.bootstrapCtx = bootstrapCtx;
        this.raName = raName;
    }

    public void transition(final ServiceController<? extends Object> controller,
                           final ServiceController.Transition transition) {
        switch (transition) {
            case STARTING_to_UP: {

                CommonDeployment deploymentMD = getDeploymentMetadata(controller);

                if (deploymentMD.getConnectionManagers() != null) {
                    for (ConnectionManager cm : deploymentMD.getConnectionManagers()) {
                        if (cm.getPool() != null) {
                            StatisticsPlugin poolStats = cm.getPool().getStatistics();
                            poolStats.setEnabled(false);
                            final ServiceController<?> bootstrapContextController = controller.getServiceContainer().getService(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append(bootstrapCtx));
                            WorkManager wm = null;
                            if (bootstrapContextController != null) {
                                wm = (WorkManager) ((CloneableBootstrapContext) bootstrapContextController.getValue()).getWorkManager();
                            }
                            if ((wm != null && wm.getStatistics() != null) || poolStats.getNames().size() != 0) {

                                PathElement pe = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, ResourceAdaptersExtension.SUBSYSTEM_NAME);
                                PathElement peStats = PathElement.pathElement(Constants.STATISTICS_NAME, Constants.STATISTICS_NAME);
                                PathElement peRa = PathElement.pathElement(Constants.RESOURCEADAPTER_NAME, raName);
                                PathElement peWm = PathElement.pathElement(Constants.WORKMANAGER_NAME, wm.getName());
                                PathElement peDistributedWm = PathElement.pathElement(Constants.DISTRIBUTED_WORKMANAGER_NAME, wm.getName());
                                PathElement peCD = PathElement.pathElement(Constants.CONNECTIONDEFINITIONS_NAME, cm.getJndiName());
                                ManagementResourceRegistration overrideRegistration = registration;
                                //when you are in deploy you have a registration pointing to deployment=*
                                //when you are in re-deploy it points to specific deploymentUnit
                                synchronized (this) {
                                    if (registration.isAllowsOverride()) {

                                        if (registration.getOverrideModel(deploymentUnitName) != null) {
                                            overrideRegistration = registration.getOverrideModel(deploymentUnitName);
                                        } else {
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

                                    }

                                    ManagementResourceRegistration subRegistration;
                                    try {
                                        ResourceBuilder resourceBuilder = ResourceBuilder.Factory.create(pe,
                                                new StandardResourceDescriptionResolver(Constants.STATISTICS_NAME, CommonAttributes.RESOURCE_NAME, CommonAttributes.class.getClassLoader()));
                                        subRegistration = overrideRegistration.registerSubModel(resourceBuilder.build());

                                        } catch (IllegalArgumentException iae) {
                                        subRegistration = overrideRegistration.getSubModel(PathAddress.pathAddress(pe));
                                    }
                                    Resource subsystemResource;

                                    if (!deploymentResource.hasChild(pe)) {
                                        subsystemResource = new IronJacamarResource.IronJacamarRuntimeResource();
                                        deploymentResource.registerChild(pe, subsystemResource);
                                    } else {
                                        subsystemResource = deploymentResource.getChild(pe);
                                    }

                                    ManagementResourceRegistration statsRegistration;
                                    try {
                                        ResourceBuilder resourceBuilder = ResourceBuilder.Factory.create(peStats,
                                                new StandardResourceDescriptionResolver(Constants.STATISTICS_NAME, CommonAttributes.RESOURCE_NAME, CommonAttributes.class.getClassLoader()));

                                        statsRegistration = subRegistration.registerSubModel(resourceBuilder.build());
                                    } catch (IllegalArgumentException iae) {
                                        statsRegistration = subRegistration.getSubModel(PathAddress.pathAddress(peStats));
                                    }
                                    Resource statisticsResource;

                                    if (!subsystemResource.hasChild(peStats)) {
                                        statisticsResource = new IronJacamarResource.IronJacamarRuntimeResource();
                                        subsystemResource.registerChild(peStats, statisticsResource);
                                    } else {
                                        statisticsResource = subsystemResource.getChild(peStats);
                                    }

                                    ManagementResourceRegistration raRegistration;
                                    try {
                                        ResourceBuilder resourceBuilder = ResourceBuilder.Factory.create(peRa,
                                                new StandardResourceDescriptionResolver(Constants.STATISTICS_NAME, CommonAttributes.RESOURCE_NAME, CommonAttributes.class.getClassLoader()));

                                        raRegistration = statsRegistration.registerSubModel(resourceBuilder.build());
                                    } catch (IllegalArgumentException iae) {
                                        raRegistration = statsRegistration.getSubModel(PathAddress.pathAddress(peRa));
                                    }
                                    Resource raResource;

                                    if (!statisticsResource.hasChild(peRa)) {
                                        raResource = new IronJacamarResource.IronJacamarRuntimeResource();
                                        statisticsResource.registerChild(peRa, raResource);
                                    } else {
                                        raResource = statisticsResource.getChild(peRa);
                                    }
                                    if (deploymentMD.getConnector() != null && deploymentMD.getConnector().getResourceAdapter() != null && deploymentMD.getConnector().getResourceAdapter().getStatistics() != null) {
                                        StatisticsPlugin raStats = deploymentMD.getConnector().getResourceAdapter().getStatistics();
                                        raStats.setEnabled(false);
                                        PoolMetrics.ParametrizedPoolMetricsHandler handler = new PoolMetrics.ParametrizedPoolMetricsHandler(raStats);
                                        for (AttributeDefinition attribute : StatisticsResourceDefinition.getAttributesFromPlugin(raStats)){
                                            raRegistration.registerMetric(attribute, handler);
                                        }
                                        //adding enable/disable for pool stats
                                        OperationStepHandler readHandler = new PoolStatisticsRuntimeAttributeReadHandler(raStats);
                                        OperationStepHandler writeHandler = new PoolStatisticsRuntimeAttributeWriteHandler(raStats);
                                        raRegistration.registerReadWriteAttribute(org.jboss.as.connector.subsystems.common.pool.Constants.POOL_STATISTICS_ENABLED, readHandler, writeHandler);

                                    }
                                    if (poolStats.getNames().size() != 0 && raRegistration.getSubModel(PathAddress.pathAddress(peCD)) == null) {
                                        ManagementResourceRegistration cdSubRegistration = raRegistration.registerSubModel(new StatisticsResourceDefinition(peCD, CommonAttributes.RESOURCE_NAME, poolStats));
                                        final Resource cdResource = new IronJacamarResource.IronJacamarRuntimeResource();

                                        if (!raResource.hasChild(peCD))
                                            raResource.registerChild(peCD, cdResource);
                                    }

                                    if (wm.getStatistics() != null) {
                                        if (wm instanceof DistributedWorkManager && ((DistributedWorkManager)wm).getDistributedStatistics() != null && raRegistration.getSubModel(PathAddress.pathAddress(peDistributedWm)) == null) {
                                            ResourceBuilder resourceBuilder = ResourceBuilder.Factory.create(peDistributedWm,
                                                    new StandardResourceDescriptionResolver(Constants.STATISTICS_NAME + "." + Constants.WORKMANAGER_NAME, CommonAttributes.RESOURCE_NAME, CommonAttributes.class.getClassLoader()));

                                            ManagementResourceRegistration dwmSubRegistration = raRegistration.registerSubModel(resourceBuilder.build());
                                            final Resource dwmResource = new IronJacamarResource.IronJacamarRuntimeResource();

                                            if (!raResource.hasChild(peDistributedWm))
                                                raResource.registerChild(peDistributedWm, dwmResource);

                                            OperationStepHandler metricsHandler = new WorkManagerRuntimeAttributeReadHandler(wm, ((DistributedWorkManager)wm).getDistributedStatistics(), false);
                                            for (SimpleAttributeDefinition metric : Constants.WORKMANAGER_METRICS) {
                                                dwmSubRegistration.registerMetric(metric, metricsHandler);
                                            }

                                            OperationStepHandler readHandler = new WorkManagerRuntimeAttributeReadHandler(wm, ((DistributedWorkManager)wm).getDistributedStatistics(), true);
                                            OperationStepHandler writeHandler = new WorkManagerRuntimeAttributeWriteHandler(wm, true, Constants.DISTRIBUTED_WORKMANAGER_RW_ATTRIBUTES);
                                            for (SimpleAttributeDefinition attribute : Constants.DISTRIBUTED_WORKMANAGER_RW_ATTRIBUTES) {
                                                dwmSubRegistration.registerReadWriteAttribute(attribute, readHandler, writeHandler);
                                            }

                                            dwmSubRegistration.registerOperationHandler(ClearWorkManagerStatisticsHandler.DEFINITION, new ClearWorkManagerStatisticsHandler(wm));
                                        }
                                        if (raRegistration.getSubModel(PathAddress.pathAddress(peWm)) == null) {
                                            ResourceBuilder resourceBuilder = ResourceBuilder.Factory.create(peWm,
                                                    new StandardResourceDescriptionResolver(Constants.STATISTICS_NAME + "." + Constants.WORKMANAGER_NAME, CommonAttributes.RESOURCE_NAME, CommonAttributes.class.getClassLoader()));

                                            ManagementResourceRegistration wmSubRegistration = raRegistration.registerSubModel(resourceBuilder.build());
                                            final Resource wmResource = new IronJacamarResource.IronJacamarRuntimeResource();

                                            if (!raResource.hasChild(peWm))
                                                raResource.registerChild(peWm, wmResource);

                                            OperationStepHandler metricHandler = new WorkManagerRuntimeAttributeReadHandler(wm, wm.getStatistics(), false);
                                            for (SimpleAttributeDefinition metric : Constants.WORKMANAGER_METRICS) {
                                                wmSubRegistration.registerMetric(metric, metricHandler);
                                            }

                                            OperationStepHandler readHandler = new WorkManagerRuntimeAttributeReadHandler(wm, wm.getStatistics(), false);
                                            OperationStepHandler writeHandler = new WorkManagerRuntimeAttributeWriteHandler(wm, false, Constants.WORKMANAGER_RW_ATTRIBUTES);
                                            for (SimpleAttributeDefinition attribute : Constants.WORKMANAGER_RW_ATTRIBUTES) {
                                                wmSubRegistration.registerReadWriteAttribute(attribute, readHandler, writeHandler);
                                            }

                                            wmSubRegistration.registerOperationHandler(ClearWorkManagerStatisticsHandler.DEFINITION, new ClearWorkManagerStatisticsHandler(wm));

                                        }
                                    }

                                    registerIronjacamar(controller, subRegistration, subsystemResource);
                                }
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
                if (registration.isAllowsOverride() && registration.getOverrideModel(deploymentUnitName) != null) {
                    overrideRegistration = registration.getOverrideModel(deploymentUnitName);
                }
                ManagementResourceRegistration subsystemReg = overrideRegistration.getSubModel(PathAddress.pathAddress(pe));
                if (subsystemReg != null) {
                    if (subsystemReg.getSubModel(PathAddress.pathAddress(ijPe)) != null) {
                        subsystemReg.unregisterSubModel(ijPe);
                    }
                    ManagementResourceRegistration statsReg = subsystemReg.getSubModel(PathAddress.pathAddress(peStats));
                    if (statsReg != null) {
                        if (statsReg.getSubModel(PathAddress.pathAddress(peCD)) != null) {
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

    protected abstract void registerIronjacamar(final ServiceController<? extends Object> controller, final ManagementResourceRegistration subRegistration, final Resource subsystemResource);

    protected abstract CommonDeployment getDeploymentMetadata(final ServiceController<? extends Object> controller);
}
