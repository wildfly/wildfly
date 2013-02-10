/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.security.metadata.EJBBoundSecurityMetaData;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;

import java.util.List;

/**
 * A {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} which processes EJB deployments and configures the
 * <code>missing-method-permissions-deny-access</code> on the {@link EJBComponentDescription}s
 *
 * @author Jaikiran Pai
 */
public class MissingMethodPermissionsDenyAccessMergingProcessor extends AbstractMergingProcessor<EJBComponentDescription> {

    private volatile boolean denyAccessByDefault = false;


    public MissingMethodPermissionsDenyAccessMergingProcessor() {
        super(EJBComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(DeploymentUnit deploymentUnit, EEApplicationClasses applicationClasses, DeploymentReflectionIndex deploymentReflectionIndex, Class<?> componentClass, EJBComponentDescription description) throws DeploymentUnitProcessingException {
        // we don't support annotations (for now).
    }

    @Override
    protected void handleDeploymentDescriptor(DeploymentUnit deploymentUnit, DeploymentReflectionIndex deploymentReflectionIndex, Class<?> componentClass, EJBComponentDescription description) throws DeploymentUnitProcessingException {
        Boolean missingMethodPermissionsDenyAccess = null;
        Boolean missingMethodPermissionsDenyAccessApplicableForAllBeans = null;
        final EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetaData != null) {
            final AssemblyDescriptorMetaData assemblyMetadata = ejbJarMetaData.getAssemblyDescriptor();
            if (assemblyMetadata != null) {
                final List<EJBBoundSecurityMetaData> securityMetaDatas = assemblyMetadata.getAny(EJBBoundSecurityMetaData.class);
                if (securityMetaDatas != null) {
                    for (final EJBBoundSecurityMetaData securityMetaData : securityMetaDatas) {
                        if (securityMetaData.getEjbName().equals(description.getComponentName())) {
                            missingMethodPermissionsDenyAccess = securityMetaData.getMissingMethodPermissionsDenyAccess();
                            break;
                        }
                        // check missing-method-permissions-excluded-mode that's applicable for all EJBs.
                        if (securityMetaData.getEjbName().equals("*")) {
                            missingMethodPermissionsDenyAccessApplicableForAllBeans = securityMetaData.getMissingMethodPermissionsDenyAccess();
                            continue;
                        }
                    }
                }
            }
        }
        if (missingMethodPermissionsDenyAccess != null) {
            description.setMissingMethodPermissionsDenyAccess(missingMethodPermissionsDenyAccess);
        } else if (missingMethodPermissionsDenyAccessApplicableForAllBeans != null) {
            description.setMissingMethodPermissionsDenyAccess(missingMethodPermissionsDenyAccessApplicableForAllBeans);
        } else {
            description.setMissingMethodPermissionsDenyAccess(denyAccessByDefault);
        }

    }

    public boolean isDenyAccessByDefault() {
        return denyAccessByDefault;
    }

    public void setDenyAccessByDefault(final boolean denyAccessByDefault) {
        this.denyAccessByDefault = denyAccessByDefault;
    }
}
