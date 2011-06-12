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
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.MessageDrivenBeanMetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefsMetaData;

/**
 * Sets up the {@link EJBComponentDescription} with the &lt;security-role-ref&gt;s declared for a EJB
 *
 * User: Jaikiran Pai
 */
public class SecurityRoleRefDDProcessor extends AbstractEjbXmlDescriptorProcessor<EnterpriseBeanMetaData> {

    private static final Logger logger = Logger.getLogger(SecurityRoleRefDDProcessor.class);

    @Override
    protected Class<EnterpriseBeanMetaData> getMetaDataType() {
        return EnterpriseBeanMetaData.class;
    }

    @Override
    protected void processBeanMetaData(final EnterpriseBeanMetaData beanMetaData, final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        SecurityRoleRefsMetaData securityRoles = null;
        if (beanMetaData instanceof SessionBeanMetaData) {
            securityRoles = ((SessionBeanMetaData) beanMetaData).getSecurityRoleRefs();
        } else if (beanMetaData instanceof MessageDrivenBeanMetaData) {
            // TODO: Why doesn't MessageDrivenBeanMetaData have security role refs metadata
            logger.warn("security-role-ref for message driven beans isn't yet implemented");
        }
        if (securityRoles == null) {
            return;
        }
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) moduleDescription.getComponentByName(beanMetaData.getEjbName());
        ejbComponentDescription.setDeclaredRoles(securityRoles.keySet());

    }
}
