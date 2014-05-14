/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.jboss.as.ejb3.logging.EjbLogger;

/**
 * @author Jaikiran Pai
 */
public class EJBBusinessMethod implements Serializable {

    private final String methodName;

    private final Class<?>[] methodParamTypes;

    private final MethodIntf viewType;

    private final int hashCode;


    public EJBBusinessMethod(Method method) {
        this(method.getName(), method.getParameterTypes());
    }

    public EJBBusinessMethod(String methodName, Class<?>... methodParamTypes) {
        this(MethodIntf.BEAN, methodName, methodParamTypes);
    }

    public EJBBusinessMethod(MethodIntf view, String methodName, Class<?>... paramTypes) {
        if (methodName == null) {
            throw EjbLogger.ROOT_LOGGER.methodNameIsNull();
        }
        this.methodName = methodName;
        this.methodParamTypes = paramTypes == null ? new Class<?>[0] : paramTypes;
        this.viewType = view == null ? MethodIntf.BEAN : view;

        this.hashCode = this.generateHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EJBBusinessMethod that = (EJBBusinessMethod) o;

        if (!methodName.equals(that.methodName)) {
            return false;
        }
        if (!Arrays.equals(methodParamTypes, that.methodParamTypes)) {
            return false;
        }
        if (viewType != that.viewType) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    private int generateHashCode() {
        int result = methodName.hashCode();
        result = 31 * result + Arrays.hashCode(methodParamTypes);
        result = 31 * result + viewType.hashCode();
        return result;
    }


}
