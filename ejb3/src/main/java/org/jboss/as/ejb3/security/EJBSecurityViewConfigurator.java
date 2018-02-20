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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.component.serialization.WriteReplaceInterface;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.deployment.ApplicableMethodInformation;
import org.jboss.as.ejb3.security.service.EJBViewMethodSecurityAttributesService;
import org.jboss.as.security.deployment.SecurityAttachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

/**
 * {@link ViewConfigurator} responsible for setting up necessary security interceptors on an EJB view.
 * <p/>
 * User: Jaikiran Pai
 */
public class EJBSecurityViewConfigurator implements ViewConfigurator {

    @Override
    public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription viewDescription, ViewConfiguration viewConfiguration) throws DeploymentUnitProcessingException {
        if (componentConfiguration.getComponentDescription() instanceof EJBComponentDescription == false) {
            throw EjbLogger.ROOT_LOGGER.invalidEjbComponent(componentConfiguration.getComponentName(), componentConfiguration.getComponentClass());
        }

        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        final boolean isSecurityDomainKnown = ejbComponentDescription.isSecurityDomainKnown();
        final String viewClassName = viewDescription.getViewClassName();
        final EJBViewDescription ejbViewDescription = (EJBViewDescription) viewDescription;

        final EJBViewMethodSecurityAttributesService.Builder viewMethodSecurityAttributesServiceBuilder;
        final ServiceName viewMethodSecurityAttributesServiceName;
        // The way @WebService view integrates with EJBs is tricky. It marks the fully qualified bean class name as the view name of the service endpoint. Now, if that bean also has a @LocalBean (i.e. no-interface view)
        // then we now have 2 views with the same view name. In such cases, it's fine to skip one of those views and register this service only once, since essentially, the service is expected to return the same data
        // for both these views. So here we skip the @WebService view if the bean also has a @LocalBean (no-interface) view and let the EJBViewMethodSecurityAttributesService be built when the no-interface view is processed
        // note that we always install this service for SERVICE_ENDPOINT views, even if security is not enabled
        if (MethodIntf.SERVICE_ENDPOINT == ejbViewDescription.getMethodIntf()) {
            viewMethodSecurityAttributesServiceBuilder = new EJBViewMethodSecurityAttributesService.Builder();
            viewMethodSecurityAttributesServiceName =  EJBViewMethodSecurityAttributesService.getServiceName(ejbComponentDescription.getApplicationName(), ejbComponentDescription.getModuleName(), ejbComponentDescription.getEJBName(), viewClassName);
        } else {
            viewMethodSecurityAttributesServiceBuilder = null;
            viewMethodSecurityAttributesServiceName = null;
        }


        if ((! deploymentUnit.hasAttachment(SecurityAttachments.SECURITY_ENABLED)) && (! isSecurityDomainKnown)) {
            // the security subsystem is not present and Elytron is not being used for security, we don't apply any security settings
            installAttributeServiceIfRequired(context, viewMethodSecurityAttributesServiceBuilder, viewMethodSecurityAttributesServiceName);
            return;
        }
        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        // The getSecurityDomain() will return a null value if neither an explicit security domain is configured
        // for the bean nor there's any default security domain that's configured at EJB3 subsystem level.
        // In such cases, we do *not* apply any security interceptors
        if (ejbComponentDescription.getSecurityDomain() == null || ejbComponentDescription.getSecurityDomain().isEmpty()) {
            if (ROOT_LOGGER.isDebugEnabled()) {
                ROOT_LOGGER
                        .debug("Security is *not* enabled on EJB: "
                                + ejbComponentDescription.getEJBName()
                                + ", since no explicit security domain is configured for the bean, nor is there any default security domain configured in the EJB3 subsystem");
            }
            installAttributeServiceIfRequired(context, viewMethodSecurityAttributesServiceBuilder, viewMethodSecurityAttributesServiceName);
            return;
        }

        // setup the JACC contextID.
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
            boolean methodHasSecurityMetadata = handlePermissions(contextID, componentConfiguration, viewConfiguration, deploymentReflectionIndex, viewClassName, ejbViewDescription, viewMethod, permissions, false, viewMethodSecurityAttributesServiceBuilder, ejbComponentDescription);
            if (!methodHasSecurityMetadata) {
                //if it was not handled by the descriptor processor we look for annotation basic info
                methodHasSecurityMetadata = handlePermissions(contextID, componentConfiguration, viewConfiguration, deploymentReflectionIndex, viewClassName, ejbViewDescription, viewMethod, ejbComponentDescription.getAnnotationMethodPermissions(), true, viewMethodSecurityAttributesServiceBuilder, ejbComponentDescription);
            }
            // if any method has security metadata then the bean has method level security metadata
            if (methodHasSecurityMetadata) {
                beanHasMethodLevelSecurityMetadata = true;
            } else {
                // make a note that this method didn't have any explicit method permissions configured
                methodsWithoutExplicitSecurityConfiguration.add(viewMethod);
            }
        }

        final boolean securityRequired = beanHasMethodLevelSecurityMetadata || ejbComponentDescription.hasBeanLevelSecurityMetadata();
        ejbComponentDescription.setSecurityRequired(securityRequired);
        // setup the security context interceptor
        if (isSecurityDomainKnown) {
            final HashMap<Integer, InterceptorFactory> elytronInterceptorFactories = ejbComponentDescription.getElytronInterceptorFactories(contextID, ejbComponentDescription.isEnableJacc(), true);
            elytronInterceptorFactories.forEach((priority, elytronInterceptorFactory) -> viewConfiguration.addViewInterceptor(elytronInterceptorFactory, priority));
        } else {
            viewConfiguration.addViewInterceptor(new SecurityContextInterceptorFactory(securityRequired, true, contextID), InterceptorOrder.View.SECURITY_CONTEXT);
        }
        // now add the authorization interceptor if the bean has *any* security metadata applicable
        if (securityRequired) {
            // check the missing-method-permissions-deny-access configuration and add the authorization interceptor
            // to methods which don't have explicit method permissions.
            // (@see http://anil-identity.blogspot.in/2010/02/tip-interpretation-of-missing-ejb.html for details)
            final Boolean denyAccessToMethodsMissingPermissions = ((EJBComponentDescription) componentConfiguration.getComponentDescription()).isMissingMethodPermissionsDeniedAccess();
            // default to "deny access"
            if (denyAccessToMethodsMissingPermissions != Boolean.FALSE) {
                for (final Method viewMethod : methodsWithoutExplicitSecurityConfiguration) {
                    if (viewMethodSecurityAttributesServiceBuilder != null) {
                        // build the EJBViewMethodSecurityAttributesService to expose these security attributes to other components like WS (@see https://issues.jboss.org/browse/WFLY-308)
                        viewMethodSecurityAttributesServiceBuilder.addMethodSecurityMetadata(viewMethod, EJBMethodSecurityAttribute.denyAll());
                    }
                    // "deny access" implies we need the authorization interceptor to be added so that it can nuke the invocation
                    if (isSecurityDomainKnown) {
                        viewConfiguration.addViewInterceptor(viewMethod, new ImmediateInterceptorFactory(RolesAllowedInterceptor.DENY_ALL), InterceptorOrder.View.EJB_SECURITY_AUTHORIZATION_INTERCEPTOR);
                    } else {
                        final Interceptor authorizationInterceptor = new AuthorizationInterceptor(EJBMethodSecurityAttribute.denyAll(), viewClassName, viewMethod, contextID);
                        viewConfiguration.addViewInterceptor(viewMethod, new ImmediateInterceptorFactory(authorizationInterceptor), InterceptorOrder.View.EJB_SECURITY_AUTHORIZATION_INTERCEPTOR);
                    }
                }
            }
        }
        installAttributeServiceIfRequired(context, viewMethodSecurityAttributesServiceBuilder, viewMethodSecurityAttributesServiceName);
    }

    private void installAttributeServiceIfRequired(DeploymentPhaseContext context, EJBViewMethodSecurityAttributesService.Builder viewMethodSecurityAttributesServiceBuilder, ServiceName viewMethodSecurityAttributesServiceName) {
        if (viewMethodSecurityAttributesServiceBuilder != null) {
            final EJBViewMethodSecurityAttributesService viewMethodSecurityAttributesService = viewMethodSecurityAttributesServiceBuilder.build();
            context.getServiceTarget().addService(viewMethodSecurityAttributesServiceName, viewMethodSecurityAttributesService).install();
        }
    }

    private boolean handlePermissions(String contextID, ComponentConfiguration componentConfiguration, ViewConfiguration viewConfiguration, DeploymentReflectionIndex deploymentReflectionIndex, String viewClassName, EJBViewDescription ejbViewDescription, Method viewMethod, ApplicableMethodInformation<EJBMethodSecurityAttribute> permissions, boolean annotations,
                                      final EJBViewMethodSecurityAttributesService.Builder viewMethodSecurityAttributesServiceBuilder, EJBComponentDescription componentDescription) {
        EJBMethodSecurityAttribute ejbMethodSecurityMetaData = permissions.getViewAttribute(ejbViewDescription.getMethodIntf(), viewMethod);
        final List<EJBMethodSecurityAttribute> allAttributes = new ArrayList<EJBMethodSecurityAttribute>();
        allAttributes.addAll(permissions.getAllAttributes(ejbViewDescription.getMethodIntf(), viewMethod));

        if (ejbMethodSecurityMetaData == null) {
            ejbMethodSecurityMetaData = permissions.getViewAttribute(MethodIntf.BEAN, viewMethod);
        }
        allAttributes.addAll(permissions.getAllAttributes(MethodIntf.BEAN, viewMethod));

        final Method classMethod = ClassReflectionIndexUtil.findMethod(deploymentReflectionIndex, componentConfiguration.getComponentClass(), viewMethod);
        if (ejbMethodSecurityMetaData == null) {
            //if this is null we try with the corresponding bean method
            if (classMethod != null) {
                ejbMethodSecurityMetaData = permissions.getAttribute(ejbViewDescription.getMethodIntf(), classMethod);
                if (ejbMethodSecurityMetaData == null) {
                    ejbMethodSecurityMetaData = permissions.getAttribute(MethodIntf.BEAN, classMethod);

                }
            }
        }
        if (classMethod != null) {
            allAttributes.addAll(permissions.getAllAttributes(ejbViewDescription.getMethodIntf(), classMethod));
            allAttributes.addAll(permissions.getAllAttributes(MethodIntf.BEAN, classMethod));
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
            // build the EJBViewMethodSecurityAttributesService to expose these security attributes to other components like WS (@see https://issues.jboss.org/browse/WFLY-308)
            if (viewMethodSecurityAttributesServiceBuilder != null) {
                viewMethodSecurityAttributesServiceBuilder.addMethodSecurityMetadata(viewMethod, ejbMethodSecurityMetaData);
            }

            if (ejbMethodSecurityMetaData.isPermitAll()) {
                // no need to add authorizing interceptor
                return true;
            }
            // add the interceptor
            final Interceptor authorizationInterceptor;
            if (componentDescription.isSecurityDomainKnown()) {
                if (ejbMethodSecurityMetaData.isDenyAll()) {
                    authorizationInterceptor = RolesAllowedInterceptor.DENY_ALL;
                } else {
                    if (componentDescription.isEnableJacc()) {
                        authorizationInterceptor = new JaccInterceptor(viewClassName, viewMethod);
                    } else {
                        authorizationInterceptor = new RolesAllowedInterceptor(ejbMethodSecurityMetaData.getRolesAllowed());
                    }
                }
            } else {
                authorizationInterceptor = new AuthorizationInterceptor(ejbMethodSecurityMetaData, viewClassName, viewMethod, contextID);
            }
            viewConfiguration.addViewInterceptor(viewMethod, new ImmediateInterceptorFactory(authorizationInterceptor), InterceptorOrder.View.EJB_SECURITY_AUTHORIZATION_INTERCEPTOR);

            return true;
        }
        return false;
    }
}
