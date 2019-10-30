/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import javax.annotation.security.RunAs;
import java.util.List;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.security.metadata.EJBBoundSecurityMetaData;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.ejb3.annotation.RunAsPrincipal;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.SecurityIdentityMetaData;
import org.jboss.metadata.javaee.spec.RunAsMetaData;

/**
 * Handles the {@link javax.annotation.security.RunAs} annotation merging
 *
 * @author Stuart Douglas
 */
public class RunAsMergingProcessor extends AbstractMergingProcessor<EJBComponentDescription> {

    private static final String DEFAULT_RUN_AS_PRINCIPAL = "anonymous";

    public RunAsMergingProcessor() {
        super(EJBComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses,
                                     final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass,
                                     final EJBComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        final EEModuleClassDescription clazz = applicationClasses.getClassByName(componentClass.getName());
        // we only care about annotations on the bean class itself
        if (clazz == null) {
            return;
        }
        final ClassAnnotationInformation<RunAs, String> runAs = clazz.getAnnotationInformation(RunAs.class);
        if (runAs == null) {
            return;
        }
        if (!runAs.getClassLevelAnnotations().isEmpty()) {
            componentConfiguration.setRunAs(runAs.getClassLevelAnnotations().get(0));
        }
        final ClassAnnotationInformation<RunAsPrincipal, String> runAsPrincipal = clazz
                .getAnnotationInformation(RunAsPrincipal.class);
        String principal = DEFAULT_RUN_AS_PRINCIPAL;
        if (runAsPrincipal != null) {
            if (!runAsPrincipal.getClassLevelAnnotations().isEmpty()) {
                principal = runAsPrincipal.getClassLevelAnnotations().get(0);
            }
        }
        componentConfiguration.setRunAsPrincipal(principal);
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit,
                                              final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass,
                                              final EJBComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        if (componentConfiguration.getDescriptorData() != null) {
            final SecurityIdentityMetaData identity = componentConfiguration.getDescriptorData().getSecurityIdentity();

            if (identity != null) {
                final RunAsMetaData runAs = identity.getRunAs();
                if (runAs != null) {
                    final String role = runAs.getRoleName();
                    if (role != null && !role.trim().isEmpty()) {
                        componentConfiguration.setRunAs(role.trim());
                    }
                }
            }
        }
        if (componentConfiguration.getRunAs() != null) {
            String principal = null;
            String globalRunAsPrincipal = null;
            EjbJarMetaData jbossMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
            if (jbossMetaData != null && jbossMetaData.getAssemblyDescriptor() != null) {
                List<EJBBoundSecurityMetaData> securityMetaDatas = jbossMetaData.getAssemblyDescriptor().getAny(EJBBoundSecurityMetaData.class);
                if (securityMetaDatas != null) {
                    for (EJBBoundSecurityMetaData securityMetaData : securityMetaDatas) {
                        if (securityMetaData.getEjbName().equals(componentConfiguration.getComponentName())) {
                            principal = securityMetaData.getRunAsPrincipal();
                            break;
                        }
                        // check global run-as principal
                        if (securityMetaData.getEjbName().equals("*")) {
                            globalRunAsPrincipal = securityMetaData.getRunAsPrincipal();
                            continue;
                        }
                    }
                }

                if (principal != null)
                    componentConfiguration.setRunAsPrincipal(principal);
                else if (globalRunAsPrincipal != null)
                    componentConfiguration.setRunAsPrincipal(globalRunAsPrincipal);
                else {
                    // we only set the run-as-principal to default, if it's not already set (via annotation) on the component
                    if (componentConfiguration.getRunAsPrincipal() == null) {
                        componentConfiguration.setRunAsPrincipal(DEFAULT_RUN_AS_PRINCIPAL);
                    }
                }

            }
        }
    }
}