/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.ejb3.timerservice;

import static org.jboss.as.ejb3.util.MethodInfoHelper.EMPTY_STRING_ARRAY;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Timer;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.SimpleInterceptorFactoryContext;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RunResult;

/**
 * Timed object invoker for an enterprise bean. This is analogous to a view service for timer invocations
 *
 * @author Stuart Douglas
 */
public class TimedObjectInvokerImpl implements TimedObjectInvoker, Service<TimedObjectInvoker> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("TimedObjectInvoker");

    private final InjectedValue<EJBComponent> ejbComponent = new InjectedValue<EJBComponent>();
    private final Module module;

    /**
     * String that uniquely identifies a deployment
     */
    private final String deploymentString;

    private Map<Method, Interceptor> timeoutInterceptors;

    public TimedObjectInvokerImpl(final String deploymentString, final Module module) {
        this.deploymentString = deploymentString;
        this.module = module;
    }

    @Override
    public void callTimeout(Timer timer, Method method) throws Exception {
        ControlPoint controlPoint = this.ejbComponent.getValue().getControlPoint();
        if (controlPoint != null) {
            if (controlPoint.beginRequest() == RunResult.REJECTED) {
                throw EjbLogger.EJB3_TIMER_LOGGER.containerSuspended();
            }
            try {
                this.invoke(timer, method);
            } finally {
                controlPoint.requestComplete();
            }
        } else {
            this.invoke(timer, method);
        }
    }

    private void invoke(Timer timer, Method method) throws Exception {
        Interceptor interceptor = this.timeoutInterceptors.get(method);
        if (interceptor == null) {
            throw EjbLogger.EJB3_TIMER_LOGGER.failToInvokeTimeout(method);
        }
        InterceptorContext context = new InterceptorContext();
        context.setContextData(new HashMap<String, Object>());
        context.setMethod(method);
        context.setParameters(method.getParameterCount() == 0 ? EMPTY_STRING_ARRAY : new Object[] { timer });
        context.setTimer(timer);
        context.putPrivateData(Component.class, this.ejbComponent.getValue());
        context.putPrivateData(MethodInterfaceType.class, MethodInterfaceType.Timer);
        context.putPrivateData(InvocationType.class, InvocationType.TIMER);
        interceptor.processInvocation(context);
    }

    @Override
    public EJBComponent getComponent() {
        return this.ejbComponent.getValue();
    }

    @Override
    public String getTimedObjectId() {
        return deploymentString + "." + ejbComponent.getValue().getComponentName();
    }

    @Override
    public ClassLoader getClassLoader() {
        return module.getClassLoader();
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        SimpleInterceptorFactoryContext factoryContext = new SimpleInterceptorFactoryContext();
        factoryContext.getContextData().put(Component.class, ejbComponent.getValue());
        Map<Method, Interceptor> interceptors = new HashMap<Method, Interceptor>();
        for(Map.Entry<Method, InterceptorFactory> entry : ejbComponent.getValue().getTimeoutInterceptors().entrySet()) {
            interceptors.put(entry.getKey(), entry.getValue().create(factoryContext));
        }
        this.timeoutInterceptors = interceptors;
    }

    @Override
    public synchronized void stop(final StopContext context) {
        this.timeoutInterceptors = null;
    }

    @Override
    public synchronized TimedObjectInvoker getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<EJBComponent> getEjbComponent() {
        return ejbComponent;
    }
}
