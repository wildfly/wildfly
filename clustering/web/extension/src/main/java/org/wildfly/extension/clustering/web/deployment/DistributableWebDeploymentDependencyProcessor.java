/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.web.deployment;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.web.session.SharedSessionManagerConfig;
import org.jboss.logging.Logger;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.web.service.WebProviderRequirement;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;

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
            CapabilityServiceSupport support = unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
            DistributableWebDeploymentConfiguration config = unit.getAttachment(CONFIGURATION_KEY);

            String name = (config != null) ? config.getSessionManagementName() : null;
            DistributableSessionManagementProvider<? extends DistributableSessionManagementConfiguration<DeploymentUnit>> management = (name == null) && (config != null) ? config.getSessionManagement() : null;
            List<String> immutableClasses = (config != null) ? config.getImmutableClasses() : Collections.emptyList();
            for (String immutableClass : immutableClasses) {
                unit.addToAttachmentList(DistributableSessionManagementProvider.IMMUTABILITY_ATTACHMENT_KEY, immutableClass);
            }

            if (management != null) {
                LOGGER.debugf("%s will use a deployment-specific distributable session management provider", unit.getName());
                ServiceTarget target = context.getServiceTarget();
                DeploymentUnit parentUnit = unit.getParent();
                String deploymentName = (parentUnit != null) ? parentUnit.getName() + "." + unit.getName() : unit.getName();
                ServiceName serviceName = WebProviderRequirement.SESSION_MANAGEMENT_PROVIDER.getServiceName(support, deploymentName);

                ServiceBuilder<?> builder = target.addService(serviceName);
                Consumer<DistributableSessionManagementProvider<? extends DistributableSessionManagementConfiguration<DeploymentUnit>>> injector = builder.provides(serviceName);
                Service service = Service.newInstance(injector, management);
                builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

                context.addDependency(serviceName, DistributableSessionManagementProvider.ATTACHMENT_KEY);
            } else {
                if (name != null) {
                    LOGGER.debugf("%s will use the '%s' distributable session management provider", unit.getName(), name);
                } else {
                    LOGGER.debugf("%s will use the default distributable session management provider", unit.getName());
                }
                context.addDependency(WebProviderRequirement.SESSION_MANAGEMENT_PROVIDER.getServiceName(support, name), DistributableSessionManagementProvider.ATTACHMENT_KEY);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit unit) {
        unit.removeAttachment(DistributableSessionManagementProvider.IMMUTABILITY_ATTACHMENT_KEY);
    }
}
