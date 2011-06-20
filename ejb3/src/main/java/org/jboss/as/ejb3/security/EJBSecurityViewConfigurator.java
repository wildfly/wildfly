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

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * {@link ViewConfigurator} responsible for setting up necessary security interceptors on a EJB view.
 * <p/>
 * User: Jaikiran Pai
 */
public class EJBSecurityViewConfigurator implements ViewConfigurator {

    private static final Logger logger = Logger.getLogger(EJBSecurityViewConfigurator.class);

    @Override
    public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription viewDescription, ViewConfiguration viewConfiguration) throws DeploymentUnitProcessingException {
        if (componentConfiguration.getComponentDescription() instanceof EJBComponentDescription == false) {
            throw new IllegalArgumentException("Component named " + componentConfiguration.getComponentName() +
                    " with component class " + componentConfiguration.getComponentClass() + " is not a EJB component");
        }
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        // if security is not enabled on the EJB, then do *not* add the security related interceptors
        if (!ejbComponentDescription.isSecurityEnabled()) {
            logger.debug("Security is *not* enabled on EJB: " + ejbComponentDescription.getEJBName() + ", no security interceptors will apply");
            return;
        }
        final String viewClassName = viewDescription.getViewClassName();
        // setup the security context interceptor
        viewConfiguration.addViewInterceptor(new SecurityContextInterceptorFactory(), InterceptorOrder.View.SECURITY_CONTEXT);

        // now setup the rest of the method specific security interceptor(s)
        final Method[] viewMethods = viewConfiguration.getProxyFactory().getCachedMethods();
        for (final Method viewMethod : viewMethods) {
            // TODO: proxy factory exposes non-public methods, is this a bug in the no-interface view?
            if (!Modifier.isPublic(viewMethod.getModifiers())) {
                continue;
            }
            final EJBMethodSecurityMetaData ejbMethodSecurityMetaData = new EJBMethodSecurityMetaData(componentConfiguration, viewClassName, viewMethod);
            // setup the authorization interceptor
            final Interceptor authorizationInterceptor = new AuthorizationInterceptor(ejbMethodSecurityMetaData, viewClassName, viewMethod);
            viewConfiguration.addViewInterceptor(viewMethod, new ImmediateInterceptorFactory(authorizationInterceptor), InterceptorOrder.View.EJB_SECURITY_AUTHORIZATION_INTERCEPTOR);
        }

    }
}
