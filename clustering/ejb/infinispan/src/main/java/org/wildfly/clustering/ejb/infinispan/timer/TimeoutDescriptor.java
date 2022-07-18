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
