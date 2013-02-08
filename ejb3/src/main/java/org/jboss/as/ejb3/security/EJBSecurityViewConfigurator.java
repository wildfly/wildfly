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

package org.jboss.as.ejb3.security;

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.component.serialization.WriteReplaceInterface;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.deployment.ApplicableMethodInformation;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.proxy.MethodIdentifier;

/**
 * {@link ViewConfigurator} responsible for setting up necessary security interceptors on a EJB view.
 * <p/>
 * User: Jaikiran Pai
 */
public class EJBSecurityViewConfigurator implements ViewConfigurator {

    @Override
    public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription viewDescription, ViewConfiguration viewConfiguration) throws DeploymentUnitProcessingException {
        if (componentConfiguration.getComponentDescription() instanceof EJBComponentDescription == false) {
            throw MESSAGES.invalidEjbComponent(componentConfiguration.getComponentName(), componentConfiguration.getComponentClass());
        }
        final DeploymentReflectionIndex deploymentReflectionIndex = context.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        // The getSecurityDomain() will return a null value if neither an explicit security domain is configured
        // for the bean nor there's any default security domain that's configured at EJB3 subsystem level.
        // In such cases, we do *not* apply any security interceptors
        if (ejbComponentDescription.getSecurityDomain() == null) {
            ROOT_LOGGER.debug("Security is *not* enabled on EJB: " + ejbComponentDescription.getEJBName() +
                    ", since no explicit security domain is configured for the bean, nor is there any default security domain configured in the EJB3 subsystem");
            return;
        }

        final String viewClassName = viewDescription.getViewClassName();
        final EJBViewDescription ejbViewDescription = (EJBViewDescription) viewDescription;

        // setup the JACC contextID.
        DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        String contextID = deploymentUnit.getName();
        if (deploymentUnit.getParent() != null) {
            contextID = deploymentUnit.getParent().getName() + "!" + contextID;
        }

        // setup the method specific security interceptor(s)
        boolean beanHasMethodLevelSecurityMetadata = false;
        final List<Method> viewMethods = viewConfiguration.getProxyFactory().getCachedMethods();
        final List<Method> methodsWithoutExplicitSecurityConfiguration = new ArrayList<Method>();
        for (final Method viewMethod : viewMethods) {
            // TODO: proxy factory exposes non-public methods, is this a bug in the no-interface view?
            if (!Modifier.isPublic(viewMethod.getModifiers())) {
                continue;
            }
            if (viewMethod.getDeclaringClass() == WriteReplaceInterface.class) {
                continue;
            }
            // setup the authorization interceptor
            final ApplicableMethodInformation<EJBMethodSecurityAttribute> permissions = ejbComponentDescription.getDescriptorMethodPermissions();
            boolean methodHasSecurityMetadata = handlePermissions(contextID, componentConfiguration, viewConfiguration, deploymentReflectionIndex, viewClassName, ejbViewDescription, viewMethod, permissions, false);
            if (!methodHasSecurityMetadata) {
                //if it was not handled by the descriptor processor we look for annotation basic info
                methodHasSecurityMetadata = handlePermissions(contextID, componentConfiguration, viewConfiguration, deploymentReflectionIndex, viewClassName, ejbViewDescription, viewMethod, ejbComponentDescription.getAnnotationMethodPermissions(), true);
            }
            // if any method has security metadata then the bean has method level security metadata
            if (methodHasSecurityMetadata) {
                beanHasMethodLevelSecurityMetadata = true;
            } else {
                // make a note that this method didn't have any explicit method permissions configured
                methodsWithoutExplicitSecurityConfiguration.add(viewMethod);
            }
        }

        // now add the security interceptor if the bean has *any* security metadata applicable
        if (beanHasMethodLevelSecurityMetadata || this.hasSecurityMetaData(ejbComponentDescription)) {
            // setup the security context interceptor
            viewConfiguration.addViewInterceptor(new SecurityContextInterceptorFactory(), InterceptorOrder.View.SECURITY_CONTEXT);
            // also check the missing-method-permissions-deny-access configuration and add the authorization interceptor
            // to methods which don't have explicit method permissions.
            // (@see http://anil-identity.blogspot.in/2010/02/tip-interpretation-of-missing-ejb.html for details)
            final Boolean denyAccessToMethodsMissingPermissions = ((EJBComponentDescription) componentConfiguration.getComponentDescription()).isMissingMethodPermissionsDeniedAccess();
            // default to "deny access"
            if (denyAccessToMethodsMissingPermissions == null || denyAccessToMethodsMissingPermissions == true) {
                for (final Method viewMethod : methodsWithoutExplicitSecurityConfiguration) {
                    // "deny access" implies we need the authorization interceptor to be added so that it can nuke the invocation
                    final Interceptor authorizationInterceptor = new AuthorizationInterceptor(EJBMethodSecurityAttribute.denyAll(), viewClassName, viewMethod, contextID);
                    viewConfiguration.addViewInterceptor(viewMethod, new ImmediateInterceptorFactory(authorizationInterceptor), InterceptorOrder.View.EJB_SECURITY_AUTHORIZATION_INTERCEPTOR);
                }
            }
        } else {
            // if security is not applicable for the EJB, then do *not* add the security related interceptors
            ROOT_LOGGER.debug("Security is *not* enabled on EJB: " + ejbComponentDescription.getEJBName() + ", no security interceptors will apply");
            return;
        }

    }

    private boolean handlePermissions(String contextID, ComponentConfiguration componentConfiguration, ViewConfiguration viewConfiguration, DeploymentReflectionIndex deploymentReflectionIndex, String viewClassName, EJBViewDescription ejbViewDescription, Method viewMethod, ApplicableMethodInformation<EJBMethodSecurityAttribute> permissions, boolean annotations) {
        EJBMethodSecurityAttribute ejbMethodSecurityMetaData = permissions.getViewAttribute(ejbViewDescription.getMethodIntf(), viewMethod.getName(), MethodIdentifier.getIdentifierForMethod(viewMethod).getParameterTypes());
        final List<EJBMethodSecurityAttribute> allAttributes = new ArrayList<EJBMethodSecurityAttribute>();
        allAttributes.addAll(permissions.getAllAttributes(ejbViewDescription.getMethodIntf(), viewMethod.getDeclaringClass().getName(), viewMethod.getName(), MethodIdentifier.getIdentifierForMethod(viewMethod).getParameterTypes()));

        if (ejbMethodSecurityMetaData == null) {
            ejbMethodSecurityMetaData = permissions.getViewAttribute(MethodIntf.BEAN, viewMethod.getName(), MethodIdentifier.getIdentifierForMethod(viewMethod).getParameterTypes());
        }
        allAttributes.addAll(permissions.getAllAttributes(MethodIntf.BEAN, viewMethod.getDeclaringClass().getName(), viewMethod.getName(), MethodIdentifier.getIdentifierForMethod(viewMethod).getParameterTypes()));

        final Method classMethod = ClassReflectionIndexUtil.findMethod(deploymentReflectionIndex, componentConfiguration.getComponentClass(), viewMethod);
        if (ejbMethodSecurityMetaData == null) {
            //if this is null we try with the corresponding bean method
            if (classMethod != null) {
                ejbMethodSecurityMetaData = permissions.getAttribute(ejbViewDescription.getMethodIntf(), classMethod.getDeclaringClass().getName(), classMethod.getName(), MethodIdentifier.getIdentifierForMethod(classMethod).getParameterTypes());
                if (ejbMethodSecurityMetaData == null) {
                    ejbMethodSecurityMetaData = permissions.getAttribute(MethodIntf.BEAN, classMethod.getDeclaringClass().getName(), classMethod.getName(), MethodIdentifier.getIdentifierForMethod(classMethod).getParameterTypes());

                }
            }
        }
        if (classMethod != null) {
            allAttributes.addAll(permissions.getAllAttributes(ejbViewDescription.getMethodIntf(), classMethod.getDeclaringClass().getName(), classMethod.getName(), MethodIdentifier.getIdentifierForMethod(classMethod).getParameterTypes()));
            allAttributes.addAll(permissions.getAllAttributes(MethodIntf.BEAN, classMethod.getDeclaringClass().getName(), classMethod.getName(), MethodIdentifier.getIdentifierForMethod(classMethod).getParameterTypes()));
        }


        //we do not add the security interceptor if there is no security information
        if (ejbMethodSecurityMetaData != null) {

            if (!annotations &&
                    !ejbMethodSecurityMetaData.isDenyAll() &&
                    !ejbMethodSecurityMetaData.isPermitAll()) {
                //roles are additive when defined in the deployment descriptor
                final Set<String> rolesAllowed = new HashSet<String>();
                for (EJBMethodSecurityAttribute attr : allAttributes) {
                    rolesAllowed.addAll(attr.getRolesAllowed());
                }
                ejbMethodSecurityMetaData = EJBMethodSecurityAttribute.rolesAllowed(rolesAllowed);
            }

            final Interceptor authorizationInterceptor = new AuthorizationInterceptor(ejbMethodSecurityMetaData, viewClassName, viewMethod, contextID);
            viewConfiguration.addViewInterceptor(viewMethod, new ImmediateInterceptorFactory(authorizationInterceptor), InterceptorOrder.View.EJB_SECURITY_AUTHORIZATION_INTERCEPTOR);
            return true;
        }
        return false;
    }

    /**
     * Returns true if the passed EJB component description has any security metadata configured at the EJB level.
     * Else returns false. Note that this method does *not* consider method level security metadata.
     *
     * @param ejbComponentDescription The EJB component description
     * @return
     */
    private boolean hasSecurityMetaData(final EJBComponentDescription ejbComponentDescription) {
        // if an explicit security-domain is present, then we consider it the bean to be processed by security interceptors
        if (ejbComponentDescription.isExplicitSecurityDomainConfigured()) {
            return true;
        }
        // if a run-as is present, then we consider it the bean to be processed by security interceptors
        if (ejbComponentDescription.getRunAs() != null) {
            return true;
        }
        // if a run-as-principal is present, then we consider it the bean to be processed by security interceptors
        if (ejbComponentDescription.getRunAsPrincipal() != null) {
            return true;
        }
        // if security roles are configured then we consider the bean to be processed by security interceptors
        final Collection securityRoles = ejbComponentDescription.getSecurityRoles();
        if (securityRoles != null && !securityRoles.isEmpty()) {
            return true;
        }
        // if security role links are configured then we consider the bean to be processed by security interceptors
        final Map<String, Collection<String>> securityRoleLinks = ejbComponentDescription.getSecurityRoleLinks();
        if (securityRoleLinks != null && !securityRoleLinks.isEmpty()) {
            return true;
        }
        // if declared roles are configured then we consider the bean to be processed by security interceptors
        final Set<String> declaredRoles = ejbComponentDescription.getDeclaredRoles();
        if (declaredRoles != null && !declaredRoles.isEmpty()) {
            return true;
        }
        // no security metadata at bean level
        return false;
    }
}
