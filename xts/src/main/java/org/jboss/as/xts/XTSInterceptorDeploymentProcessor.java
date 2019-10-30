/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.xts;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.webservices.injection.WSComponentDescription;
import org.jboss.narayana.txframework.api.annotation.service.ServiceRequest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


public class XTSInterceptorDeploymentProcessor implements DeploymentUnitProcessor {


    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit unit = phaseContext.getDeploymentUnit();

        final EEModuleDescription moduleDescription = unit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        for (ComponentDescription component : moduleDescription.getComponentDescriptions()) {

            if (component instanceof SessionBeanComponentDescription) {
                registerSessionBeanInterceptors((SessionBeanComponentDescription) component);
            }
            if (component instanceof WSComponentDescription) {
                registerWSPOJOInterceptors((WSComponentDescription) component);
            }
        }
    }

    private void registerSessionBeanInterceptors(SessionBeanComponentDescription componentDescription) {
        if (componentDescription.isStateless()) {

            componentDescription.getConfigurators().addFirst(new ComponentConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws
                        DeploymentUnitProcessingException {
                    for (Method method : configuration.getDefinedComponentMethods()) {
                        if (methodHasServiceRequestAnnotation(method)) {
                            configuration.addComponentInterceptor(method, XTSEJBInterceptor.FACTORY, InterceptorOrder.Component.XTS_INTERCEPTOR);
                            configuration.getInterceptorContextKeys().add(XTSEJBInterceptor.CONTEXT_KEY);
                        }
                    }
                }
            });
        }

    }

    private void registerWSPOJOInterceptors(WSComponentDescription componentDescription) {
        for (ViewDescription view : componentDescription.getViews()) {
            view.getConfigurators().add(new ViewConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                    for (final Method method : configuration.getProxyFactory().getCachedMethods()) {
                        if (methodHasServiceRequestAnnotation(method)) {
                            configuration.addViewInterceptor(method, XTSPOJOInterceptor.FACTORY, InterceptorOrder.View.XTS_INTERCEPTOR);
                        }
                    }
                }
            });
        }
    }

    public void undeploy(final DeploymentUnit unit) {

        // does nothing
    }

    private boolean methodHasServiceRequestAnnotation(Method method) {
        for (Annotation a : method.getDeclaredAnnotations()) {
            if (a instanceof ServiceRequest) {
                return true;
            }
        }
        return false;
    }

}
