/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors.merging;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.javaee.spec.SecurityRoleRefMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefsMetaData;

import jakarta.annotation.security.DeclareRoles;

/**
 * Merging process for @DeclareRoles
 *
 * @author Stuart Douglas
 */
public class DeclareRolesMergingProcessor extends AbstractMergingProcessor<EJBComponentDescription> {

    public DeclareRolesMergingProcessor() {
        super(EJBComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription description) throws DeploymentUnitProcessingException {
        final EEModuleClassDescription clazz = applicationClasses.getClassByName(componentClass.getName());
        //we only care about annotations on the bean class itself
        if (clazz == null) {
            return;
        }
        final ClassAnnotationInformation<DeclareRoles, String[]> declareRoles = clazz.getAnnotationInformation(DeclareRoles.class);
        if (declareRoles == null) {
            return;
        }
        if (!declareRoles.getClassLevelAnnotations().isEmpty()) {
            description.addDeclaredRoles(declareRoles.getClassLevelAnnotations().get(0));
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription description) throws DeploymentUnitProcessingException {
        if (description.getDescriptorData() == null) {
            return;
        }
        final SecurityRoleRefsMetaData roleRefs = description.getDescriptorData().getSecurityRoleRefs();

        if (roleRefs != null) {
            for(SecurityRoleRefMetaData ref : roleRefs) {
                description.addDeclaredRoles(ref.getRoleName());
            }
        }
    }
}
