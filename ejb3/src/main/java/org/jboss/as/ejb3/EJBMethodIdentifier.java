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

package org.jboss.as.ejb3;

import java.lang.reflect.Method;

import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 * Identifier for a method on a EJB and is classloader agnostic.
 * <p/>
 * Unlike the {@link MethodIdentifier} this {@link EJBMethodIdentifier} takes into the account the declaring class of the
 * method.
 * <p/>
 * User: Jaikiran Pai
 */
public class EJBMethodIdentifier {

    private final MethodIdentifier methodIdentifier;

    private final String methodDeclaringClass;

    private final int cachedHashCode;

    public EJBMethodIdentifier(final MethodIdentifier methodIdentifier, final String methodDeclaringClass) {
        this.methodIdentifier = methodIdentifier;
        this.methodDeclaringClass = methodDeclaringClass;

        this.cachedHashCode = this.generateHashCode();
    }

    public static EJBMethodIdentifier fromMethodInfo(final MethodInfo methodInfo) {
        final String returnType = methodInfo.returnType().name().toString();
        final String[] argTypes = new String[methodInfo.args().length];
        int i = 0;
        for (Type argType : methodInfo.args()) {
            argTypes[i++] = argType.name().toString();
        }
        final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(returnType, methodInfo.name(), argTypes);
        return new EJBMethodIdentifier(methodIdentifier, methodInfo.declaringClass().name().toString());
    }

    public static EJBMethodIdentifier fromMethod(final Method method) {
        final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifierForMethod(method);
        return new EJBMethodIdentifier(methodIdentifier, method.getDeclaringClass().getName());
    }

    private int generateHashCode() {
        int result = methodIdentifier.hashCode();
        result = 31 * result + methodDeclaringClass.hashCode();
        return result;
    }

    public MethodIdentifier getMethodIdentifier() {
        return methodIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EJBMethodIdentifier that = (EJBMethodIdentifier) o;

        if (!methodDeclaringClass.equals(that.methodDeclaringClass)) return false;
        if (!methodIdentifier.equals(that.methodIdentifier)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return this.cachedHashCode;
    }

}
