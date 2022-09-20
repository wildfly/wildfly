/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.infinispan.timer;

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

    String getDeclaringClassName() {
        return this.declaringClassName;
    }

    int getIndex() {
        return this.index;
    }
}
