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
import java.util.List;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.component.serialization.WriteReplaceInterface;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.proxy.MethodIdentifier;

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

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
        // if security is not enabled on the EJB, then do *not* add the security related interceptors
        if (!ejbComponentDescription.isSecurityEnabled()) {
            ROOT_LOGGER.debug("Security is *not* enabled on EJB: " + ejbComponentDescription.getEJBName() + ", no security interceptors will apply");
            return;
        }
        final String viewClassName = viewDescription.getViewClassName();
        // setup the security context interceptor
        viewConfiguration.addViewInterceptor(new SecurityContextInterceptorFactory(), InterceptorOrder.View.SECURITY_CONTEXT);

        final EJBViewDescription ejbViewDescription = (EJBViewDescription) viewDescription;

        // now setup the rest of the method specific security interceptor(s)
        final List<Method> viewMethods = viewConfiguration.getProxyFactory().getCachedMethods();
        for (final Method viewMethod : viewMethods) {
            // TODO: proxy factory exposes non-public methods, is this a bug in the no-interface view?
            if (!Modifier.isPublic(viewMethod.getModifiers())) {
                continue;
            }
            if (viewMethod.getDeclaringClass() == WriteReplaceInterface.class) {
                continue;
            }
            // setup the authorization interceptor
            EJBMethodSecurityAttribute ejbMethodSecurityMetaData = ejbComponentDescription.getMethodPermissions().getViewAttribute(ejbViewDescription.getMethodIntf(), viewMethod.getName(), MethodIdentifier.getIdentifierForMethod(viewMethod).getParameterTypes());
            if (ejbMethodSecurityMetaData == null) {
                ejbMethodSecurityMetaData = ejbComponentDescription.getMethodPermissions().getViewAttribute(MethodIntf.BEAN, viewMethod.getName(), MethodIdentifier.getIdentifierForMethod(viewMethod).getParameterTypes());
            }

            if (ejbMethodSecurityMetaData == null) {
                //if this is null we try with the corresponding bean method
                final Method classMethod = ClassReflectionIndexUtil.findMethod(deploymentReflectionIndex, componentConfiguration.getComponentClass(), viewMethod);
                if (classMethod != null) {
                    ejbMethodSecurityMetaData = ejbComponentDescription.getMethodPermissions().getAttribute(ejbViewDescription.getMethodIntf(), classMethod.getDeclaringClass().getName(), classMethod.getName(), MethodIdentifier.getIdentifierForMethod(classMethod).getParameterTypes());
                    if (ejbMethodSecurityMetaData == null) {
                        ejbMethodSecurityMetaData = ejbComponentDescription.getMethodPermissions().getAttribute(MethodIntf.BEAN, classMethod.getDeclaringClass().getName(), classMethod.getName(), MethodIdentifier.getIdentifierForMethod(classMethod).getParameterTypes());

                    }
                }
            }
            //we do not add the security interceptor if there is no security information
            if (ejbMethodSecurityMetaData != null) {
                final Interceptor authorizationInterceptor = new AuthorizationInterceptor(ejbMethodSecurityMetaData, viewClassName, viewMethod);
                viewConfiguration.addViewInterceptor(viewMethod, new ImmediateInterceptorFactory(authorizationInterceptor), InterceptorOrder.View.EJB_SECURITY_AUTHORIZATION_INTERCEPTOR);
            }
        }

    }
}
