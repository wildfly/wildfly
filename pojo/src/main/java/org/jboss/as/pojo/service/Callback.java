/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import org.jboss.as.pojo.BeanState;
import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.pojo.descriptor.CallbackConfig;
import org.jboss.as.pojo.descriptor.ValueConfig;

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
            if (m.getParameterCount() != 1)
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
        joinpoint.setTarget(() -> bean);
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
