/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors.security;

import static org.jboss.as.ee.component.Attachments.EE_MODULE_CONFIGURATION;

import jakarta.security.jacc.PolicyConfiguration;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.security.AbstractSecurityDeployer;
import org.jboss.as.ee.security.JaccService;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.deployment.EjbSecurityDeployer;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * A {@code DeploymentUnitProcessor} for JACC policies.
 *
 * @author Marcus Moyses
 * @author Anil Saldhana
 */
public class JaccEjbDeploymentProcessor implements DeploymentUnitProcessor {

    /*
     * Prior to Jakarta EE 11 the Policy was an instance of java.security.Policy, within the Elytron subsystem
     * we would make use of an MSC service to handle the global registration. Any deployments that were utilising JACC
     * would need to depend on the "org.wildfly.security.jacc-policy" capability to ensure registration was complete
     * before processing the deployment.
     *
     * The Elytron subsystem now also supports an immediate registration which is indicated by the
     * "org.wildfly.security.jakarta-authorization" capability, if this is registered it means Jakarta Authorization
     * has already been registered.
     */
    private static final String ELYTRON_JACC_CAPABILITY = "org.wildfly.security.jacc-policy";
    public static final String ELYTRON_JAKARTA_AUTHORIZATION = "org.wildfly.security.jakarta-authorization";

    public JaccEjbDeploymentProcessor() {
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentContainsEjbs(deploymentUnit) == false) {
            return;
        }

        CapabilityServiceSupport capabilitySupport = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

        boolean usesElytronJaccPolicy = capabilitySupport.hasCapability(ELYTRON_JACC_CAPABILITY);
        boolean requireJakartaAuthorization = usesElytronJaccPolicy || capabilitySupport.hasCapability(ELYTRON_JAKARTA_AUTHORIZATION);
        if (requireJakartaAuthorization == false) {
            return;
        }

        AbstractSecurityDeployer<?> deployer = null;
        deployer = new EjbSecurityDeployer();
        JaccService<?> service = deployer.deploy(deploymentUnit);
        if (service != null) {
            final DeploymentUnit parentDU = deploymentUnit.getParent();
            // Jakarta Enterprise Beans maybe included directly in war deployment
            ServiceName jaccServiceName = getJaccServiceName(deploymentUnit);
            final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
            ServiceBuilder<?> builder = serviceTarget.addService(jaccServiceName, service);
            if (parentDU != null) {
                // add dependency to parent policy
                builder.addDependency(parentDU.getServiceName().append(JaccService.SERVICE_NAME), PolicyConfiguration.class,
                        service.getParentPolicyInjector());
            }
            if (usesElytronJaccPolicy) {
                builder.requires(capabilitySupport.getCapabilityServiceName(ELYTRON_JACC_CAPABILITY));
            }
            builder.setInitialMode(Mode.ACTIVE).install();
        }
    }

    private static boolean deploymentContainsEjbs(final DeploymentUnit deploymentUnit) {
        final EEModuleConfiguration moduleConfiguration = deploymentUnit.getAttachment(EE_MODULE_CONFIGURATION);
        for (ComponentConfiguration current : moduleConfiguration.getComponentConfigurations()) {
            if (current.getComponentDescription() instanceof EJBComponentDescription) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        AbstractSecurityDeployer<?> deployer = new EjbSecurityDeployer();
        deployer.undeploy(deploymentUnit);

        // Jakarta Enterprise Beans maybe included directly in war deployment
        ServiceName jaccServiceName = getJaccServiceName(deploymentUnit);
        ServiceRegistry registry = deploymentUnit.getServiceRegistry();
        if(registry != null){
            ServiceController<?> serviceController = registry.getService(jaccServiceName);
            if (serviceController != null) {
                serviceController.setMode(ServiceController.Mode.REMOVE);
            }
        }
    }

    private ServiceName getJaccServiceName(DeploymentUnit deploymentUnit){
        final DeploymentUnit parentDU = deploymentUnit.getParent();
        // Jakarta Enterprise Beans maybe included directly in war deployment
        ServiceName jaccServiceName = deploymentUnit.getServiceName().append(JaccService.SERVICE_NAME).append("ejb");
        //Qualify the service name properly with parent DU
        if(parentDU != null) {
            jaccServiceName = jaccServiceName.append(parentDU.getName());
        }
        return jaccServiceName;
    }
}
