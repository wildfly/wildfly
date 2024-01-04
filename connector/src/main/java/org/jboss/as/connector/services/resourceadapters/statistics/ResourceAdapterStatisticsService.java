/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.resourceadapters.statistics;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.connector.dynamicresource.StatisticsResourceDefinition;
import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
import org.jboss.as.connector.subsystems.resourceadapters.CommonAttributes;
import org.jboss.as.connector.subsystems.resourceadapters.Constants;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

public class ResourceAdapterStatisticsService implements Service<ManagementResourceRegistration> {


    private final ManagementResourceRegistration overrideRegistration;
    private final boolean statsEnabled;

    protected final InjectedValue<ResourceAdapterDeployment> deployment = new InjectedValue<>();
    protected final InjectedValue<CloneableBootstrapContext> bootstrapContext = new InjectedValue<>();


    /**
     * create an instance *
     */
    public ResourceAdapterStatisticsService(final ManagementResourceRegistration registration,
                                            final String jndiName,
                                            final boolean statsEnabled) {
        super();
        if (registration.isAllowsOverride()) {
            overrideRegistration = registration.registerOverrideModel(jndiName, new OverrideDescriptionProvider() {
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
            overrideRegistration = registration;
        }
        this.statsEnabled = statsEnabled;

    }


    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("Starting ResourceAdapterStatusicService");
        synchronized (this) {
            PathElement peExtendedStats = PathElement.pathElement(Constants.STATISTICS_NAME, "extended");



            final CommonDeployment deploymentMD = deployment.getValue().getDeployment();

            if (deploymentMD.getConnector() != null && deploymentMD.getConnector().getResourceAdapter() != null && deploymentMD.getConnector().getResourceAdapter().getStatistics() != null) {
                StatisticsPlugin raStats = deploymentMD.getConnector().getResourceAdapter().getStatistics();
                raStats.setEnabled(statsEnabled);
                overrideRegistration.registerSubModel(new StatisticsResourceDefinition(peExtendedStats, CommonAttributes.RESOURCE_NAME, raStats));
            }
        }

    }

    @Override
    public void stop(StopContext context) {
        PathElement peLocaldWm = PathElement.pathElement(Constants.STATISTICS_NAME, "extended");
        if (overrideRegistration.getSubModel(PathAddress.pathAddress(peLocaldWm)) != null)
            overrideRegistration.unregisterSubModel(peLocaldWm);
    }


    @Override
    public ManagementResourceRegistration getValue() throws IllegalStateException, IllegalArgumentException {
        //TODO implement getValue
        throw new UnsupportedOperationException();
    }

    public Injector<ResourceAdapterDeployment> getResourceAdapterDeploymentInjector() {
        return deployment;
    }

    public Injector<CloneableBootstrapContext> getBootstrapContextInjector() {
        return bootstrapContext;
    }
}
