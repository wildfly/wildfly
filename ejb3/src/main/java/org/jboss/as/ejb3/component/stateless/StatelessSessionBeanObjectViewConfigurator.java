/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.stateless;

import java.lang.reflect.Method;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.interceptors.ComponentTypeIdentityInterceptorFactory;
import org.jboss.as.ejb3.component.session.SessionBeanObjectViewConfigurator;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.Interceptors;

/**
 * @author Stuart Douglas
 */
public class StatelessSessionBeanObjectViewConfigurator extends SessionBeanObjectViewConfigurator {

    public static final StatelessSessionBeanObjectViewConfigurator INSTANCE = new StatelessSessionBeanObjectViewConfigurator();

    @Override
    protected void handleIsIdenticalMethod(final ComponentConfiguration componentConfiguration, final ViewConfiguration configuration, final DeploymentReflectionIndex index, final Method method) {
        configuration.addClientInterceptor(method, ComponentTypeIdentityInterceptorFactory.INSTANCE, InterceptorOrder.Client.EJB_EQUALS_HASHCODE);
    }

    @Override
    protected void handleRemoveMethod(final ComponentConfiguration componentConfiguration, final ViewConfiguration configuration, final DeploymentReflectionIndex index, final Method method) throws DeploymentUnitProcessingException {
        configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
        configuration.addViewInterceptor(method, Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.View.COMPONENT_DISPATCHER);
    }

}
