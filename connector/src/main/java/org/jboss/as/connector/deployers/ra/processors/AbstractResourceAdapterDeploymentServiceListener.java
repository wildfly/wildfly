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

import org.jboss.as.connector.dynamicresource.descriptionproviders.StatisticsDescriptionProvider;
import org.jboss.as.connector.dynamicresource.descriptionproviders.StatisticsElementDescriptionProvider;
import org.jboss.as.connector.dynamicresource.descriptionproviders.SubSystemExtensionDescriptionProvider;
import org.jboss.as.connector.dynamicresource.operations.ClearStatisticsHandler;
import org.jboss.as.connector.subsystems.common.pool.PoolMetrics;
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
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

/**
*
*
* @author Stefano Maestri (c) 2011 Red Hat Inc.
*/
public abstract class AbstractResourceAdapterDeploymentServiceListener extends AbstractServiceListener<Object> {
    private final ManagementResourceRegistration registration;
    private final String deploymentUnitName;
    private final Resource deploymentResource;

    public AbstractResourceAdapterDeploymentServiceListener(ManagementResourceRegistration registration, String deploymentUnitName, Resource deploymentResource) {
        this.registration = registration;
        this.deploymentUnitName = deploymentUnitName;
        this.deploymentResource = deploymentResource;
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

                            if (poolStats.getNames().size() != 0) {
                                DescriptionProvider statsResourceDescriptionProvider = new StatisticsDescriptionProvider(ResourceAdaptersSubsystemProviders.RESOURCE_NAME, "statistics", poolStats);
                                PathElement pe = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, ResourceAdaptersExtension.SUBSYSTEM_NAME);
                                PathElement peStats = PathElement.pathElement(Constants.STATISTICS_NAME, Constants.STATISTICS_NAME);
                                PathElement peCD = PathElement.pathElement(Constants.CONNECTIONDEFINITIONS_NAME, cm.getJndiName());
                                ManagementResourceRegistration overrideRegistration = registration;
                                //when you are in deploy you have a registration pointing to deployment=*
                                //when you are in re-deploy it points to specific deploymentUnit
                                if (registration.isAllowsOverride() && registration.getOverrideModel(deploymentUnitName)== null) {
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
                                } else {
                                    overrideRegistration = registration.getOverrideModel(deploymentUnitName);
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

                                registerIronjacamar(controller, subRegistration, subsystemResource);
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

    protected abstract void registerIronjacamar(final ServiceController<? extends Object> controller, final ManagementResourceRegistration subRegistration, final Resource subsystemResource);

    protected abstract CommonDeployment getDeploymentMetadata(final ServiceController<? extends Object> controller);
}
