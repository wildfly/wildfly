/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors.dd;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.javaee.spec.MessageDestinationMetaData;
import org.jboss.metadata.javaee.spec.MessageDestinationsMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;

/**
 * Processes the assembly-descriptor section of a ejb-jar.xml of a Jakarta Enterprise Beans deployment and updates the {@link EjbJarDescription}
 * appropriately with this info.
 *
 * @author Jaikiran Pai
 * @author Stuart Douglas
 */
public class AssemblyDescriptorProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // get the deployment unit
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        // find the Jakarta Enterprise Beans jar metadata and start processing it
        final EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetaData == null) {
            return;
        }
        // process assembly-descriptor stuff
        final AssemblyDescriptorMetaData assemblyDescriptor = ejbJarMetaData.getAssemblyDescriptor();
        if (assemblyDescriptor != null) {
            // get hold of the ejb jar description (to which we'll be adding this assembly description metadata)
            final EjbJarDescription ejbJarDescription = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION);

            final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);

            // process security-role(s)
            this.processSecurityRoles(assemblyDescriptor.getSecurityRoles(), ejbJarDescription);

            final MessageDestinationsMetaData destinations = assemblyDescriptor.getMessageDestinations();
            if(destinations != null) {
                processMessageDestinations(destinations, eeModuleDescription);
            }
        }

    }

    private void processMessageDestinations(final MessageDestinationsMetaData destinations, final EEModuleDescription eeModuleDescription) {
        for(final MessageDestinationMetaData destination : destinations) {
            //TODO: should these be two separate metadata attributes?
            if(destination.getJndiName() != null) {
                eeModuleDescription.addMessageDestination(destination.getName(), destination.getJndiName());
            } else if(destination.getLookupName() != null) {
                eeModuleDescription.addMessageDestination(destination.getName(), destination.getLookupName());
            }
        }
    }

    private void processSecurityRoles(final SecurityRolesMetaData securityRoles, final EjbJarDescription ejbJarDescription) {
        if (securityRoles == null || securityRoles.isEmpty()) {
            return;
        }
        for (final SecurityRoleMetaData securityRole : securityRoles) {
            final String roleName = securityRole.getRoleName();
            if (roleName != null && !roleName.trim().isEmpty()) {
                // Augment the security roles
                // Enterprise Beans 3.1 spec, section 17.3.1:
                // The Bean Provider may augment the set of security roles defined for the application by annotations in
                // this way by means of the security-role deployment descriptor element.
                ejbJarDescription.addSecurityRole(roleName);
            }
        }
    }
}
