/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Index for auto-timers.  Ensures auto-timers are only created once across cluster.
 * @author Paul Ferraro
 */
public class TimerIndex extends TimeoutDescriptor {
    private final String declaringClassName;
    private final int index;

    public TimerIndex(Method method, int index) {
        super(method);
        this.declaringClassName = method.getDeclaringClass().getName();
        this.index = index;
    }

    public TimerIndex(String declaringClassName, String methodName, int parameters, int index) {
        super(methodName, parameters);
        this.declaringClassName = declaringClassName;
        this.index = index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.declaringClassName, this.getMethodName()) + this.getParameters() + this.index;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TimerIndex)) return false;
        TimerIndex index = (TimerIndex) object;
        // We only need to compare method name and parameter count for method equality.
        return this.declaringClassName.equals(index.declaringClassName) && super.equals(index) && this.index == index.index;
    }

    @Override
    public String toString() {
        return String.format("%s.%s(%s)[%s]", this.declaringClassName, this.getMethodName(), this.getParameters() > 0 ? "Timer" : "", this.index);
    }

    public String getDeclaringClassName() {
        return this.declaringClassName;
    }

    public int getIndex() {
        return this.index;
    }
}
