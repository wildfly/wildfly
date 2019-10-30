/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors.merging;

import java.lang.reflect.Method;

import javax.ejb.SessionBean;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.interceptors.InterceptorClassDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.session.SessionBeanSetSessionContextMethodInvocationInterceptor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;

/**
 * Processor that handles the {@link javax.ejb.SessionBean} interface
 *
 * @author Stuart Douglas
 */
public class SessionBeanMergingProcessor extends AbstractMergingProcessor<SessionBeanComponentDescription> {

    public SessionBeanMergingProcessor() {
        super(SessionBeanComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SessionBeanComponentDescription description) throws DeploymentUnitProcessingException {

    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SessionBeanComponentDescription description) throws DeploymentUnitProcessingException {
        if (SessionBean.class.isAssignableFrom(componentClass)) {
            // add the setSessionContext(SessionContext) method invocation interceptor for session bean implementing the javax.ejb.SessionContext
            // interface
            description.getConfigurators().add(new ComponentConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                    if (SessionBean.class.isAssignableFrom(configuration.getComponentClass())) {
                        configuration.addPostConstructInterceptor(SessionBeanSetSessionContextMethodInvocationInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.EJB_SET_CONTEXT_METHOD_INVOCATION_INTERCEPTOR);
                    }
                }
            });

            //now lifecycle callbacks
            final MethodIdentifier ejbRemoveIdentifier = MethodIdentifier.getIdentifier(void.class, "ejbRemove");
            final MethodIdentifier ejbActivateIdentifier = MethodIdentifier.getIdentifier(void.class, "ejbActivate");
            final MethodIdentifier ejbPassivateIdentifier = MethodIdentifier.getIdentifier(void.class, "ejbPassivate");

            boolean ejbActivate = false, ejbPassivate = false, ejbRemove = false;
            Class<?> c  = componentClass;
            while (c != null && c != Object.class) {
                final ClassReflectionIndex index = deploymentReflectionIndex.getClassIndex(c);

                if(!ejbActivate) {
                    final Method method = index.getMethod(ejbActivateIdentifier);
                    if(method != null) {
                        final InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder();
                        builder.setPostActivate(ejbActivateIdentifier);
                        description.addInterceptorMethodOverride(c.getName(), builder.build());
                        ejbActivate = true;
                    }
                }

                if(!ejbPassivate) {
                    final Method method = index.getMethod(ejbPassivateIdentifier);
                    if(method != null) {
                        final InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder();
                        builder.setPrePassivate(ejbPassivateIdentifier);
                        description.addInterceptorMethodOverride(c.getName(), builder.build());
                        ejbPassivate = true;
                    }
                }

                if(!ejbRemove) {
                    final Method method = index.getMethod(ejbRemoveIdentifier);
                    if(method != null) {
                        final InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder();
                        builder.setPreDestroy(ejbRemoveIdentifier);
                        description.addInterceptorMethodOverride(c.getName(), builder.build());
                        ejbRemove = true;
                    }
                }

                c = c.getSuperclass();
            }


        }
    }
}
