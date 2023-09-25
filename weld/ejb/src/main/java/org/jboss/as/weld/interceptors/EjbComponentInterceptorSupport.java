/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.interceptors;

import java.util.List;

import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.stateful.SerializedCdiInterceptorsKey;
import org.jboss.as.weld.ejb.DelegatingInterceptorInvocationContext;
import org.jboss.as.weld.services.bootstrap.WeldEjbServices;
import org.jboss.as.weld.spi.InterceptorInstances;
import org.jboss.as.weld.spi.ComponentInterceptorSupport;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.ejb.spi.helpers.ForwardingEjbServices;
import org.jboss.weld.manager.api.WeldManager;

/**
 *
 * @author Martin Kouba
 */
public class EjbComponentInterceptorSupport implements ComponentInterceptorSupport {

    @Override
    public InterceptorInstances getInterceptorInstances(ComponentInstance componentInstance) {
        return (WeldInterceptorInstances) componentInstance.getInstanceData(SerializedCdiInterceptorsKey.class);
    }

    @Override
    public void setInterceptorInstances(ComponentInstance componentInstance, InterceptorInstances interceptorInstances) {
        componentInstance.setInstanceData(SerializedCdiInterceptorsKey.class, interceptorInstances);
    }

    @Override
    public Object delegateInterception(InvocationContext invocationContext, InterceptionType interceptionType, List<Interceptor<?>> currentInterceptors,
            List<Object> currentInterceptorInstances) throws Exception {
        return new DelegatingInterceptorInvocationContext(invocationContext, currentInterceptors, currentInterceptorInstances, interceptionType).proceed();
    }

    @Override
    public InterceptorBindings getInterceptorBindings(String ejbName, WeldManager manager) {
        EjbServices ejbServices = manager.getServices().get(EjbServices.class);
        if (ejbServices instanceof ForwardingEjbServices) {
            ejbServices = ((ForwardingEjbServices) ejbServices).delegate();
        }
        if (ejbServices instanceof WeldEjbServices) {
            return ((WeldEjbServices) ejbServices).getBindings(ejbName);
        }
        return null;
    }

}
