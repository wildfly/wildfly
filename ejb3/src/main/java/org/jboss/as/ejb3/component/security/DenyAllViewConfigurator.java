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

package org.jboss.as.ejb3.component.security;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.EJBMethodIdentifier;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.proxy.MethodIdentifier;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import static org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX;

/**
 * Configures the EJB view with a {@link DenyAllInterceptor} for methods which aren't supposed to be invoked on a EJB.
 * <p/>
 * User: Jaikiran Pai
 */
public class DenyAllViewConfigurator implements ViewConfigurator {

    @Override
    public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription viewDescription, ViewConfiguration viewConfiguration) throws DeploymentUnitProcessingException {
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        final Collection<EJBMethodIdentifier> denyAllMethodsForView = ejbComponentDescription.getDenyAllMethodsForView(viewDescription.getViewClassName());
        if (denyAllMethodsForView == null || denyAllMethodsForView.isEmpty()) {
            return;
        }
        final DeploymentReflectionIndex deploymentReflectionIndex = context.getDeploymentUnit().getAttachment(REFLECTION_INDEX);
        final ClassReflectionIndex<?> classReflectionIndex = deploymentReflectionIndex.getClassIndex(componentConfiguration.getComponentClass());
        final Method[] viewMethods = viewConfiguration.getProxyFactory().getCachedMethods();
        for (final Method viewMethod : viewMethods) {
            // find the component method corresponding to this view method
            final Method componentMethod = ClassReflectionIndexUtil.findRequiredMethod(deploymentReflectionIndex, classReflectionIndex, viewMethod);
            final EJBMethodIdentifier ejbMethodIdentifier = EJBMethodIdentifier.fromMethod(componentMethod);
            if (denyAllMethodsForView.contains(ejbMethodIdentifier)) {
                // setup the DenyAllInterceptor for this view method
                viewConfiguration.addViewInterceptor(viewMethod, new ImmediateInterceptorFactory(new DenyAllInterceptor()), InterceptorOrder.View.EJB_SECURITY_DENY_ALL_INTERCEPTOR);
                continue;
            }
            // check on class level
            final Class<?> declaringClass = componentMethod.getDeclaringClass();
            if (ejbComponentDescription.isDenyAllApplicableToClass(declaringClass.getName())) {
                // setup the DenyAllInterceptor for this view method
                viewConfiguration.addViewInterceptor(viewMethod, new ImmediateInterceptorFactory(new DenyAllInterceptor()), InterceptorOrder.View.EJB_SECURITY_DENY_ALL_INTERCEPTOR);
                continue;
            }

        }
    }
}
