/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * Describes a timeout method.
 * @author Paul Ferraro
 */
public class TimeoutDescriptor implements Predicate<Method> {

    private final String methodName;
    private final int parameters;

    public TimeoutDescriptor(Method method) {
        this(method.getName(), method.getParameterCount());
    }

    public TimeoutDescriptor(String methodName, int parameters) {
        this.methodName = methodName;
        this.parameters = parameters;
    }

    String getMethodName() {
        return this.methodName;
    }

    int getParameters() {
        return this.parameters;
    }

    @Override
    public boolean test(Method method) {
        return method.getName().equals(this.methodName) && method.getParameterCount() == this.parameters;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TimeoutDescriptor)) return false;
        TimeoutDescriptor descriptor = (TimeoutDescriptor) object;
        return this.methodName.equals(descriptor.methodName) && this.parameters == descriptor.parameters;
    }

    @Override
    public int hashCode() {
        return this.methodName.hashCode() + this.parameters;
    }
}
