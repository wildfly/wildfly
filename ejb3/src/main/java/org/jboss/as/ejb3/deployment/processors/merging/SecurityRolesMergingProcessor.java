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

package org.jboss.as.ejb3.deployment.processors.merging;

import java.util.List;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.structure.Attachments;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.metadata.merge.javaee.spec.SecurityRolesMetaDataMerger;

/**
 * A processor which sets the {@link EJBComponentDescription#setSecurityRoles(org.jboss.metadata.javaee.spec.SecurityRolesMetaData)}
 * with the principal to role mapping defined in the assembly descriptor section of the jboss-ejb3.xml via elements from
 * urn:security-roles namespace.
 * <p/>
 * Additionally, we also merge the security roles metadata from the ear.
 *
 * @author Jaikiran Pai
 * @see {@link org.jboss.as.ejb3.security.parser.SecurityRoleMetaDataParser}
 */
public class SecurityRolesMergingProcessor extends AbstractMergingProcessor<EJBComponentDescription> {

    public SecurityRolesMergingProcessor() {
        super(EJBComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(DeploymentUnit deploymentUnit, EEApplicationClasses applicationClasses, DeploymentReflectionIndex deploymentReflectionIndex, Class<?> componentClass, EJBComponentDescription description) throws DeploymentUnitProcessingException {
        // no-op
    }

    @Override
    protected void handleDeploymentDescriptor(DeploymentUnit deploymentUnit, DeploymentReflectionIndex deploymentReflectionIndex, Class<?> componentClass, EJBComponentDescription ejbComponentDescription) throws DeploymentUnitProcessingException {
        final SecurityRolesMetaData roleMappings = new SecurityRolesMetaData();
        final EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetaData != null) {
            final AssemblyDescriptorMetaData assemblyDescriptorMetaData = ejbJarMetaData.getAssemblyDescriptor();
            if (assemblyDescriptorMetaData != null) {
                // get the mapping between principal to rolename, defined in the assembly descriptor
                final List<SecurityRoleMetaData> securityRoleMetaDatas = assemblyDescriptorMetaData.getAny(SecurityRoleMetaData.class);
                if (securityRoleMetaDatas != null) {
                    for (SecurityRoleMetaData securityRoleMetaData : securityRoleMetaDatas) {
                        roleMappings.add(securityRoleMetaData);
                    }
                }
            }
        }
        //Let us look at the ear metadata also
        DeploymentUnit parent = deploymentUnit.getParent();
        if (parent != null) {
            final EarMetaData earMetaData = parent.getAttachment(Attachments.EAR_METADATA);
            if (earMetaData != null) {
                SecurityRolesMetaData earSecurityRolesMetaData = earMetaData.getSecurityRoles();
                SecurityRolesMetaDataMerger.merge(roleMappings, roleMappings, earSecurityRolesMetaData);
            }
        }
        // add it to the EJB component description
        ejbComponentDescription.setSecurityRoles(roleMappings);
    }
}