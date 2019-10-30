/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.weld.interceptors;

import java.util.List;

import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;

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
