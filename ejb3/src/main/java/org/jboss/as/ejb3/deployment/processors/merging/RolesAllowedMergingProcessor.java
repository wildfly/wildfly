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

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ejb3.EJBMethodIdentifier;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;
import org.jboss.metadata.ejb.spec.MethodMetaData;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.ejb.spec.MethodPermissionMetaData;
import org.jboss.metadata.ejb.spec.MethodPermissionsMetaData;
import org.jboss.metadata.ejb.spec.MethodsMetaData;

import javax.annotation.security.RolesAllowed;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles the {@link javax.annotation.security.RolesAllowed} annotation
 * <p/>
 * Also rocesses the &lt;method-permission&gt; elements of a EJB and sets up appropriate security permissions on the EJB.
 * <p/>
 * This processor should be run *after* all the views of the EJB have been identified and set in the {@link EJBComponentDescription}
 *
 * @author Stuart Douglas
 */
public class RolesAllowedMergingProcessor extends AbstractMergingProcessor<EJBComponentDescription> {

    private static final Logger logger = Logger.getLogger(RolesAllowedMergingProcessor.class);

    public RolesAllowedMergingProcessor() {
        super(EJBComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription description) throws DeploymentUnitProcessingException {
        final RuntimeAnnotationInformation<String[]> data = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, RolesAllowed.class);

        for (Map.Entry<String, List<String[]>> entry : data.getClassAnnotations().entrySet()) {
            description.setRolesAllowedOnAllViewsForClass(entry.getKey(), new HashSet<String>(Arrays.<String>asList(entry.getValue().get(0))));
        }

        for (Map.Entry<Method, List<String[]>> entry : data.getMethodAnnotations().entrySet()) {
            EJBMethodIdentifier identifier = EJBMethodIdentifier.fromMethod(entry.getKey());
            description.setRolesAllowedOnAllViewsForMethod(identifier, new HashSet<String>(Arrays.<String>asList(entry.getValue().get(0))));
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription description) throws DeploymentUnitProcessingException {
        final EnterpriseBeanMetaData beanMetaData = description.getDescriptorData();
        if (beanMetaData == null) {
            return;
        }
        final AssemblyDescriptorMetaData assemblyDescriptor = beanMetaData.getAssemblyDescriptor();
        if (assemblyDescriptor == null) {
            return;
        }
        final MethodPermissionsMetaData methodPermissions = assemblyDescriptor.getMethodPermissionsByEjbName(description.getEJBName());
        if (methodPermissions == null || methodPermissions.isEmpty()) {
            return;
        }

        final ClassReflectionIndex<?> classReflectionIndex = deploymentReflectionIndex.getClassIndex(componentClass);

        for (final MethodPermissionMetaData methodPermission : methodPermissions) {
            final MethodsMetaData methods = methodPermission.getMethods();
            if (methods == null || methods.isEmpty()) {
                continue;
            }
            // if "unchecked" then it means all roles are allowed access
            if (methodPermission.isNotChecked()) {
                continue;
            }
            final Set<String> securityRoles = methodPermission.getRoles();
            for (final MethodMetaData method : methods) {
                final String methodName = method.getMethodName();
                final MethodIntf methodIntf = this.getMethodIntf(method.getMethodIntf());
                // style 1
                //            <method>
                //                <ejb-name>EJBNAME</ejb-name>
                //                <method-name>*</method-name>
                //            </method>
                if (methodName.equals("*")) {
                    // if method name is * then it means all methods, which actually implies a class level @RolesAllowed
                    // now check if it specifies the optional method-inf. If it doesn't then it applies to all views
                    if (methodIntf == null) {
                        description.setRolesAllowedForAllMethodsOfAllViews(securityRoles);
                    } else {
                        description.setRolesAllowedForAllMethodsOnViewType(methodIntf, securityRoles);
                    }
                } else {
                    final MethodParametersMetaData methodParams = method.getMethodParams();
                    // style 2
                    //            <method>
                    //                <ejb-name>EJBNAME</ejb-name>
                    //                <method-name>METHOD</method-name>
                    //              </method>
                    if (methodParams == null || methodParams.isEmpty()) {
                        final Collection<Method> applicableMethods = ClassReflectionIndexUtil.findAllMethodsByName(deploymentReflectionIndex, classReflectionIndex, methodName);
                        // just log a WARN message and proceed, in case there was no method by that name
                        if (applicableMethods.isEmpty()) {
                            logger.warn("No method named: " + methodName + " found on EJB: " + description.getEJBName() + " while processing method-permission element in ejb-jar.xml");
                            continue;
                        }
                        // apply the @RolesAllowed/method-permission
                        this.setRolesAllowed(description, methodIntf, applicableMethods, securityRoles);

                    } else {
                        // style 3
                        //            <method>
                        //                <ejb-name>EJBNAME</ejb-name>
                        //                <method-name>METHOD</method-name>
                        //                <method-params>
                        //                <method-param>PARAMETER_1</method-param>
                        //                ...
                        //                <method-param>PARAMETER_N</method-param>
                        //                </method-params>
                        //
                        //              </method>
                        final String[] paramTypes = methodParams.toArray(new String[methodParams.size()]);
                        final Collection<Method> applicableMethods = ClassReflectionIndexUtil.findMethods(deploymentReflectionIndex, classReflectionIndex, methodName, paramTypes);
                        // just log a WARN message and proceed, in case there was no method by that name and param types
                        if (applicableMethods.isEmpty()) {
                            logger.warn("No method named: " + methodName + " with param types: " + paramTypes + " found on EJB: " + description.getEJBName() + " while processing method-permission element in ejb-jar.xml");
                            continue;
                        }
                        // apply the @RolesAllowed/method-permission
                        this.setRolesAllowed(description, methodIntf, applicableMethods, securityRoles);
                    }
                }
            }
        }
    }


    private void setRolesAllowed(final EJBComponentDescription ejbComponentDescription, final MethodIntf viewType, final Collection<Method> rolesAllowedApplicableMethods, Collection<String> roles) {
        for (final Method denyAllApplicableMethod : rolesAllowedApplicableMethods) {
            final EJBMethodIdentifier ejbMethodIdentifier = EJBMethodIdentifier.fromMethod(denyAllApplicableMethod);
            if (viewType == null) {
                ejbComponentDescription.setRolesAllowedOnAllViewsForMethod(ejbMethodIdentifier, new HashSet(roles));
            } else {
                ejbComponentDescription.setRolesAllowedForMethodOnViewType(viewType, ejbMethodIdentifier, new HashSet(roles));
            }
        }
    }

    protected MethodIntf getMethodIntf(MethodInterfaceType viewType) {
        if (viewType == null) {
            return null;
        }
        return super.getMethodIntf(viewType);
    }
}
