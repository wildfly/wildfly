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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.security.EJBMethodSecurityAttribute;
import org.jboss.as.ejb3.security.EjbJaccConfigurator;
import org.jboss.as.ejb3.util.MethodInfoHelper;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.ExcludeListMetaData;
import org.jboss.metadata.ejb.spec.MethodMetaData;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.ejb.spec.MethodPermissionMetaData;
import org.jboss.metadata.ejb.spec.MethodPermissionsMetaData;
import org.jboss.metadata.ejb.spec.MethodsMetaData;

/**
 * Handles the {@link javax.annotation.security.RolesAllowed} {@link DenyAll} {@link javax.annotation.security.PermitAll} annotations
 * <p/>
 * Also processes the &lt;method-permission&gt; elements of an EJB and sets up appropriate security permissions on the EJB.
 * <p/>
 * This processor should be run *after* all the views of the EJB have been identified and set in the {@link EJBComponentDescription}
 *
 * @author Stuart Douglas
 */
public class MethodPermissionsMergingProcessor extends AbstractMergingProcessor<EJBComponentDescription> {

    public MethodPermissionsMergingProcessor() {
        super(EJBComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription description) throws DeploymentUnitProcessingException {


        final RuntimeAnnotationInformation<Boolean> permitData = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, PermitAll.class);

        for (Map.Entry<String, List<Boolean>> entry : permitData.getClassAnnotations().entrySet()) {
            description.getAnnotationMethodPermissions().setAttribute(null, entry.getKey(), EJBMethodSecurityAttribute.permitAll());
        }

        for (Map.Entry<Method, List<Boolean>> entry : permitData.getMethodAnnotations().entrySet()) {
            final Method method = entry.getKey();
            description.getAnnotationMethodPermissions().setAttribute(null, EJBMethodSecurityAttribute.permitAll(), method.getDeclaringClass().getName(), method.getName(), MethodInfoHelper.getCanonicalParameterTypes(method));
        }

        final RuntimeAnnotationInformation<String[]> data = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, RolesAllowed.class);

        for (Map.Entry<String, List<String[]>> entry : data.getClassAnnotations().entrySet()) {
            description.getAnnotationMethodPermissions().setAttribute(null, entry.getKey(), EJBMethodSecurityAttribute.rolesAllowed(new HashSet<String>(Arrays.<String>asList(entry.getValue().get(0)))));
        }

        for (Map.Entry<Method, List<String[]>> entry : data.getMethodAnnotations().entrySet()) {
            final Method method = entry.getKey();
            description.getAnnotationMethodPermissions().setAttribute(null, EJBMethodSecurityAttribute.rolesAllowed(new HashSet<String>(Arrays.<String>asList(entry.getValue().get(0)))), method.getDeclaringClass().getName(), method.getName(), MethodInfoHelper.getCanonicalParameterTypes(method));
        }

        final RuntimeAnnotationInformation<Boolean> denyData = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, DenyAll.class);

        for (Map.Entry<String, List<Boolean>> entry : denyData.getClassAnnotations().entrySet()) {
            description.getAnnotationMethodPermissions().setAttribute(null, entry.getKey(), EJBMethodSecurityAttribute.denyAll());
        }

        for (Map.Entry<Method, List<Boolean>> entry : denyData.getMethodAnnotations().entrySet()) {
            final Method method = entry.getKey();
            description.getAnnotationMethodPermissions().setAttribute(null, EJBMethodSecurityAttribute.denyAll(), method.getDeclaringClass().getName(), method.getName(), MethodInfoHelper.getCanonicalParameterTypes(method));
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription componentDescription) throws DeploymentUnitProcessingException {

        //Add the configurator that calculates JACC permissions
        //TODO: should this be elsewhere?
        componentDescription.getConfigurators().add(new EjbJaccConfigurator());

        //DO NOT USE componentConfiguration.getDescriptorData()
        //It will return null if there is no <enterprise-beans/> declaration even if there is an assembly descriptor entry

        EjbJarMetaData ejbJarMetadata = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);

        if (ejbJarMetadata != null) {
            final AssemblyDescriptorMetaData assemblyDescriptor = ejbJarMetadata.getAssemblyDescriptor();
            if (assemblyDescriptor != null) {
                //handle wildcard exclude-list
                final ExcludeListMetaData wildCardExcludeList = assemblyDescriptor.getExcludeListByEjbName("*");
                if(wildCardExcludeList != null && wildCardExcludeList.getMethods() != null) {
                    handleExcludeMethods(componentDescription, wildCardExcludeList);
                }

                //handle ejb-specific exclude-list
                final ExcludeListMetaData excludeList = assemblyDescriptor.getExcludeListByEjbName(componentDescription.getEJBName());
                if (excludeList != null && excludeList.getMethods() != null) {
                    handleExcludeMethods(componentDescription, excludeList);
                }

                //handle wildcard method permissions
                final MethodPermissionsMetaData wildCardMethodPermissions = assemblyDescriptor.getMethodPermissionsByEjbName("*");
                if (wildCardMethodPermissions != null) {
                    handleMethodPermissions(componentDescription, wildCardMethodPermissions);
                }

                //handle ejb-specific method permissions
                final MethodPermissionsMetaData methodPermissions = assemblyDescriptor.getMethodPermissionsByEjbName(componentDescription.getEJBName());
                if (methodPermissions != null) {
                    handleMethodPermissions(componentDescription, methodPermissions);
                }
            }
        }
    }

    private void handleMethodPermissions(final EJBComponentDescription componentDescription, final MethodPermissionsMetaData methodPermissions) {
        for (final MethodPermissionMetaData methodPermissionMetaData : methodPermissions) {

            final MethodsMetaData methods = methodPermissionMetaData.getMethods();
            for (final MethodMetaData method : methods) {
                EJBMethodSecurityAttribute ejbMethodSecurityMetaData;
                // EJB 3.1 FR 17.3.2.2 The unchecked element is used instead of a role name in the method-permission element to indicate that all roles are permitted.
                if (methodPermissionMetaData.isNotChecked()) {
                    ejbMethodSecurityMetaData = EJBMethodSecurityAttribute.permitAll();
                } else {
                    ejbMethodSecurityMetaData = EJBMethodSecurityAttribute.rolesAllowed(methodPermissionMetaData.getRoles());
                }
                final String methodName = method.getMethodName();
                final MethodIntf defaultMethodIntf = (componentDescription instanceof MessageDrivenComponentDescription) ? MethodIntf.MESSAGE_ENDPOINT : MethodIntf.BEAN;
                final MethodIntf methodIntf = this.getMethodIntf(method.getMethodIntf(), defaultMethodIntf);
                if (methodName.equals("*")) {
                    final EJBMethodSecurityAttribute existingRoles = componentDescription.getDescriptorMethodPermissions().getAttributeStyle1(methodIntf, null);
                    ejbMethodSecurityMetaData = mergeExistingRoles(ejbMethodSecurityMetaData, existingRoles);
                    componentDescription.getDescriptorMethodPermissions().setAttribute(methodIntf, null, ejbMethodSecurityMetaData);
                } else {

                    final MethodParametersMetaData methodParams = method.getMethodParams();
                    // update the session bean description with the tx attribute info
                    if (methodParams == null) {

                        final EJBMethodSecurityAttribute existingRoles = componentDescription.getDescriptorMethodPermissions().getAttributeStyle2(methodIntf, methodName);
                        ejbMethodSecurityMetaData = mergeExistingRoles(ejbMethodSecurityMetaData, existingRoles);
                        componentDescription.getDescriptorMethodPermissions().setAttribute(methodIntf, ejbMethodSecurityMetaData, methodName);
                    } else {
                        final EJBMethodSecurityAttribute existingRoles = componentDescription.getDescriptorMethodPermissions().getAttributeStyle3(methodIntf, null, methodName, this.getMethodParams(methodParams));
                        ejbMethodSecurityMetaData = mergeExistingRoles(ejbMethodSecurityMetaData, existingRoles);
                        componentDescription.getDescriptorMethodPermissions().setAttribute(methodIntf, ejbMethodSecurityMetaData, null, methodName, this.getMethodParams(methodParams));
                    }
                }
            }
        }
    }

    private void handleExcludeMethods(final EJBComponentDescription componentDescription, final ExcludeListMetaData excludeList) {
        for (final MethodMetaData method : excludeList.getMethods()) {
            final String methodName = method.getMethodName();
            final MethodIntf defaultMethodIntf = (componentDescription instanceof MessageDrivenComponentDescription) ? MethodIntf.MESSAGE_ENDPOINT : MethodIntf.BEAN;
            final MethodIntf methodIntf = this.getMethodIntf(method.getMethodIntf(), defaultMethodIntf);
            if (methodName.equals("*")) {
                componentDescription.getDescriptorMethodPermissions().setAttribute(methodIntf, null, EJBMethodSecurityAttribute.denyAll());
            } else {

                final MethodParametersMetaData methodParams = method.getMethodParams();
                // update the session bean description with the tx attribute info
                if (methodParams == null) {
                    componentDescription.getDescriptorMethodPermissions().setAttribute(methodIntf, EJBMethodSecurityAttribute.denyAll(), methodName);
                } else {

                    componentDescription.getDescriptorMethodPermissions().setAttribute(methodIntf, EJBMethodSecurityAttribute.denyAll(), null, methodName, this.getMethodParams(methodParams));
                }
            }
        }
    }

    private EJBMethodSecurityAttribute mergeExistingRoles(EJBMethodSecurityAttribute ejbMethodSecurityMetaData, final EJBMethodSecurityAttribute existingRoles) {
        if (existingRoles != null && !existingRoles.getRolesAllowed().isEmpty()) {
            final Set<String> roles = new HashSet<String>(existingRoles.getRolesAllowed());
            roles.addAll(ejbMethodSecurityMetaData.getRolesAllowed());
            ejbMethodSecurityMetaData = EJBMethodSecurityAttribute.rolesAllowed(roles);
        }
        return ejbMethodSecurityMetaData;
    }
}
