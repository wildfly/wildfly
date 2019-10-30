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

package org.jboss.as.pojo.service;

import org.jboss.as.pojo.BeanState;
import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.pojo.descriptor.CallbackConfig;
import org.jboss.as.pojo.descriptor.ValueConfig;
import org.jboss.msc.value.ImmediateValue;

import java.lang.reflect.Method;

/**
 * Simple callback.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class Callback {
    private final BeanInfo beanInfo;
    private final Object bean;
    private final CallbackConfig config;
    private Method method;

    public Callback(BeanInfo beanInfo, Object bean, CallbackConfig config) {
        this.beanInfo = beanInfo;
        this.bean = bean;
        this.config = config;
    }

    protected Method getMethod() {
        if (method == null) {
            final Method m = beanInfo.findMethod(config.getMethodName(), config.getSignature());
            if (m.getParameterTypes().length != 1)
                throw PojoLogger.ROOT_LOGGER.illegalParameterLength(m);
            method = m;
        }
        return method;
    }

    public Class<?> getType() {
        return getMethod().getParameterTypes()[0];
    }

    public BeanState getState() {
        return config.getState();
    }

    public void dispatch() throws Throwable {
        for (Object bean : InstancesService.getBeans(getType(), getState()))
            dispatch(bean);
    }

    public void dispatch(final Object dependency) throws Throwable {
        MethodJoinpoint joinpoint = new MethodJoinpoint(getMethod());
        joinpoint.setTarget(new ImmediateValue<Object>(bean));
        ValueConfig param = new ValueConfig() {
            protected Object getClassValue(Class<?> type) {
                return dependency;
            }
        };
        joinpoint.setParameters(new ValueConfig[]{param});
        joinpoint.dispatch();
    }

    @Override
    public int hashCode() {
        return config.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Callback == false)
            return false;

        Callback callback = (Callback) obj;
        return config.equals(callback.config);
    }
}
