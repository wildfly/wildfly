/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import java.lang.reflect.Method;

/**
 * Reflection joinpoint.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ReflectionJoinpoint extends TargetJoinpoint {
    private final BeanInfo beanInfo;
    private final String methodName;
    private final String[] types;

    public ReflectionJoinpoint(BeanInfo beanInfo, String methodName) {
        this(beanInfo, methodName, null);
    }

    public ReflectionJoinpoint(BeanInfo beanInfo, String methodName, String[] types) {
        this.beanInfo = beanInfo;
        this.methodName = methodName;
        this.types = types;
    }

    @Override
    public Object dispatch() throws Throwable {
        String[] pts = types;
        if (pts == null)
            pts = Configurator.getTypes(getParameters());

        Object target = getTarget().getValue();
        Method method = beanInfo.findMethod(methodName, pts);
        return method.invoke(target, toObjects(method.getGenericParameterTypes()));
    }
}
