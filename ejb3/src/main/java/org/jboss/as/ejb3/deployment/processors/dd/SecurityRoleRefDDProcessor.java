/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
 * Sets up the {@link EJBComponentDescription} with the &lt;security-role-ref&gt;s declared for an EJB
 *
 * User: Jaikiran Pai
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
