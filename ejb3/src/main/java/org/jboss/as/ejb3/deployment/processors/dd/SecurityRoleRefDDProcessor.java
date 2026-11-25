/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors.dd;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefsMetaData;

/**
 * Sets up the {@link EJBComponentDescription} with the &lt;security-role-ref&gt;s declared for a Jakarta Enterprise Beans bean.
 *
 * @author Jaikiran Pai
 */
public class SecurityRoleRefDDProcessor extends AbstractEjbXmlDescriptorProcessor<EnterpriseBeanMetaData> {

    @Override
    protected Class<EnterpriseBeanMetaData> getMetaDataType() {
        return EnterpriseBeanMetaData.class;
    }

    @Override
    protected void processBeanMetaData(final EnterpriseBeanMetaData beanMetaData, final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final SecurityRoleRefsMetaData securityRoleRefs = beanMetaData.getSecurityRoleRefs();
        if (securityRoleRefs == null) {
            return;
        }
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) moduleDescription.getComponentByName(beanMetaData.getEjbName());
        for (final SecurityRoleRefMetaData securityRoleRef : securityRoleRefs) {
            final String fromRole = securityRoleRef.getRoleName();
            String toRole = securityRoleRef.getRoleLink();
            if (fromRole == null || fromRole.trim().isEmpty()) {
                throw EjbLogger.ROOT_LOGGER.roleNamesIsNull(ejbComponentDescription.getEJBName());
            }
            // if role-link hasn't been specified, then it links to the same role name as the one specified
            // in the role-name
            if (toRole == null) {
                toRole = fromRole;
            }
            ejbComponentDescription.linkSecurityRoles(fromRole, toRole);
        }

    }
}
