/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.security;

import static org.jboss.as.ee.subsystem.EeCapabilities.ELYTRON_JACC_CAPABILITY;
import static org.jboss.as.ee.subsystem.EeCapabilities.ELYTRON_JAKARTA_AUTHORIZATION;

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

    public JaccEarDeploymentProcessor() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            CapabilityServiceSupport capabilitySupport = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

            // See the comment in EeCapabilities for more information regarding this pair of capabilities.
            boolean usesElytronJaccPolicy = capabilitySupport.hasCapability(ELYTRON_JACC_CAPABILITY);
            boolean requireJakartaAuthorization = usesElytronJaccPolicy || capabilitySupport.hasCapability(ELYTRON_JAKARTA_AUTHORIZATION);

            if (requireJakartaAuthorization) {
                AbstractSecurityDeployer<?> deployer = new EarSecurityDeployer();
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

                    if (usesElytronJaccPolicy) {
                        builder.requires(capabilitySupport.getCapabilityServiceName(ELYTRON_JACC_CAPABILITY));
                    }

                    builder.setInitialMode(Mode.ACTIVE).install();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undeploy(DeploymentUnit context) {
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, context)) {
            AbstractSecurityDeployer<?> deployer = new EarSecurityDeployer();
            deployer.undeploy(context);
        }
    }

}
