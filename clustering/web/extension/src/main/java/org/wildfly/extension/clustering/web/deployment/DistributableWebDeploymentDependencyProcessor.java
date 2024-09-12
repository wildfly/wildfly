/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.web.session.SharedSessionManagerConfig;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * {@link DeploymentUnitProcessor} that establishes the requisite {@link DistributableSessionManagementProvider} dependency
 * for distributable web and shared session deployments.
 * @author Paul Ferraro
 */
public class DistributableWebDeploymentDependencyProcessor implements DeploymentUnitProcessor {

    public static final AttachmentKey<DistributableWebDeploymentConfiguration> CONFIGURATION_KEY = AttachmentKey.create(DistributableWebDeploymentConfiguration.class);

    private static final Logger LOGGER = Logger.getLogger(DistributableWebDeploymentDependencyProcessor.class);

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();

        WarMetaData warMetaData = unit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        SharedSessionManagerConfig sharedConfig = unit.getAttachment(SharedSessionManagerConfig.ATTACHMENT_KEY);

        if (((warMetaData != null) && (warMetaData.getMergedJBossWebMetaData() != null && warMetaData.getMergedJBossWebMetaData().getDistributable() != null)) || ((sharedConfig != null) && sharedConfig.isDistributable())) {
            DistributableWebDeploymentConfiguration config = unit.getAttachment(CONFIGURATION_KEY);

            String name = (config != null) ? config.getSessionManagementName() : null;
            DistributableSessionManagementProvider provider = (name == null) && (config != null) ? config.getSessionManagementProvider() : null;
            List<String> immutableClasses = (config != null) ? config.getImmutableClasses() : Collections.emptyList();
            for (String immutableClass : immutableClasses) {
                unit.addToAttachmentList(DistributableSessionManagementProvider.IMMUTABILITY_ATTACHMENT_KEY, immutableClass);
            }

            if (provider != null) {
                LOGGER.debugf("%s will use a deployment-specific distributable session management provider", unit.getName());
                DeploymentUnit parentUnit = unit.getParent();
                String deploymentName = (parentUnit != null) ? parentUnit.getName() + "." + unit.getName() : unit.getName();
                ServiceName serviceName = ServiceNameFactory.resolveServiceName(DistributableSessionManagementProvider.SERVICE_DESCRIPTOR, deploymentName);

                ServiceInstaller.builder(provider).provides(serviceName).build().install(context);

                context.addDependency(serviceName, DistributableSessionManagementProvider.ATTACHMENT_KEY);
            } else {
                if (name != null) {
                    LOGGER.debugf("%s will use the '%s' distributable session management provider", unit.getName(), name);
                } else {
                    LOGGER.debugf("%s will use the default distributable session management provider", unit.getName());
                }
                context.addDependency(ServiceNameFactory.resolveServiceName(DistributableSessionManagementProvider.SERVICE_DESCRIPTOR, name), DistributableSessionManagementProvider.ATTACHMENT_KEY);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit unit) {
        unit.removeAttachment(DistributableSessionManagementProvider.IMMUTABILITY_ATTACHMENT_KEY);
    }
}
