/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * Describes a timeout method.
 * @author Paul Ferraro
 */
public class TimeoutDescriptor implements Predicate<Method> {
    static final String DEFAULT_METHOD_NAME = "ejbTimeout";
    static final int DEFAULT_PARAMETERS = 0;
    static final TimeoutDescriptor DEFAULT = new TimeoutDescriptor(DEFAULT_METHOD_NAME, DEFAULT_PARAMETERS);

    private final String methodName;
    private final int parameters;

    public TimeoutDescriptor(Method method) {
        this(method.getName(), method.getParameterCount());
    }

    public TimeoutDescriptor(String methodName, int parameters) {
        this.methodName = methodName;
        this.parameters = parameters;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public int getParameters() {
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.methodName).append('(');
        for (int i = 1; i <= this.parameters; ++i) {
            builder.append("arg").append(i);
            if (i < this.parameters) {
                builder.append(", ");
            }
        }
        return builder.append(')').toString();
    }
}
