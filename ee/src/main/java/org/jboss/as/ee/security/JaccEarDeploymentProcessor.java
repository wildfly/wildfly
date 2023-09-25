/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.security;

import jakarta.security.jacc.PolicyConfiguration;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * A {@code DeploymentUnitProcessor} for Jakarta Authorization policies.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class JaccEarDeploymentProcessor implements DeploymentUnitProcessor {

    private final String jaccCapabilityName;

    public JaccEarDeploymentProcessor(final String jaccCapabilityName) {
        this.jaccCapabilityName = jaccCapabilityName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        AbstractSecurityDeployer<?> deployer = null;
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            deployer = new EarSecurityDeployer();
            JaccService<?> service = deployer.deploy(deploymentUnit);
            if (service != null) {
                final ServiceName jaccServiceName = deploymentUnit.getServiceName().append(JaccService.SERVICE_NAME);
                final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
                ServiceBuilder<?> builder = serviceTarget.addService(jaccServiceName, service);
                if (deploymentUnit.getParent() != null) {
                    // add dependency to parent policy
                    final DeploymentUnit parentDU = deploymentUnit.getParent();
                    builder.addDependency(parentDU.getServiceName().append(JaccService.SERVICE_NAME), PolicyConfiguration.class,
                            service.getParentPolicyInjector());
                }
                CapabilityServiceSupport capabilitySupport = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
                builder.requires(capabilitySupport.getCapabilityServiceName(jaccCapabilityName));
                builder.setInitialMode(Mode.ACTIVE).install();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undeploy(DeploymentUnit context) {
        AbstractSecurityDeployer<?> deployer = null;
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, context)) {
            deployer = new EarSecurityDeployer();
            deployer.undeploy(context);
        }
    }

}
