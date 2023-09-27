/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} which processes EJB deployments and configures the
 * <code>missing-method-permissions-deny-access</code> on the {@link EJBComponentDescription}s
 *
 * @author Jaikiran Pai
 */
public class MissingMethodPermissionsDenyAccessMergingProcessor extends AbstractMergingProcessor<EJBComponentDescription> {

    private final AtomicBoolean denyAccessByDefault;

    public MissingMethodPermissionsDenyAccessMergingProcessor(AtomicBoolean denyAccessByDefault) {
        super(EJBComponentDescription.class);
        this.denyAccessByDefault = denyAccessByDefault;
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
            description.setMissingMethodPermissionsDenyAccess(this.denyAccessByDefault.get());
        }
    }
}
