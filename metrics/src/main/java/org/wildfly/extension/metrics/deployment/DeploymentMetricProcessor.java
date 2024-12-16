/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        rootResource = deploymentUnit.getAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE);
        managementResourceRegistration = deploymentUnit.getAttachment(DeploymentModelUtils.MUTABLE_REGISTRATION_ATTACHMENT);

        DeploymentMetricService.install(phaseContext.getRequirementServiceTarget(), deploymentUnit, rootResource, managementResourceRegistration,
                exposeAnySubsystem, exposedSubsystems, prefix);
    }
}
