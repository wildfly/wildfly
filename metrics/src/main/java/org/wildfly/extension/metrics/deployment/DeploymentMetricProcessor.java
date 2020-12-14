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

package org.wildfly.extension.metrics.deployment;

import java.util.List;

import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentModelUtils;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.extension.metrics.MetricCollector;

public class DeploymentMetricProcessor implements DeploymentUnitProcessor {

    public static final AttachmentKey<MetricCollector> METRICS_COLLECTOR = AttachmentKey.create(MetricCollector.class);

    private final boolean exposeAnySubsystem;
    private final List<String> exposedSubsystems;
    private final String prefix;

    private Resource rootResource;
    private ManagementResourceRegistration managementResourceRegistration;

    public DeploymentMetricProcessor(boolean exposeAnySubsystem, List<String> exposedSubsystems, String prefix) {
        this.exposeAnySubsystem = exposeAnySubsystem;
        this.exposedSubsystems = exposedSubsystems;
        this.prefix = prefix;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        rootResource = phaseContext.getDeploymentUnit().getAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE);
        managementResourceRegistration = phaseContext.getDeploymentUnit().getAttachment(DeploymentModelUtils.MUTABLE_REGISTRATION_ATTACHMENT);

        DeploymentMetricService.install(phaseContext.getServiceTarget(), phaseContext.getDeploymentUnit(), rootResource, managementResourceRegistration,
                exposeAnySubsystem, exposedSubsystems, prefix);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
