/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;

/**
 * @author Jaikiran Pai
 */
public class EJBBusinessMethod implements Serializable {

    private final String methodName;

    private final Class<?>[] methodParamTypes;

    private final MethodInterfaceType viewType;

    private final int hashCode;


    public EJBBusinessMethod(Method method) {
        this(method.getName(), method.getParameterTypes());
    }

    public EJBBusinessMethod(String methodName, Class<?>... methodParamTypes) {
        this(MethodInterfaceType.Bean, methodName, methodParamTypes);
    }

    public EJBBusinessMethod(MethodInterfaceType view, String methodName, Class<?>... paramTypes) {
        if (methodName == null) {
            throw EjbLogger.ROOT_LOGGER.methodNameIsNull();
        }
        this.methodName = methodName;
        this.methodParamTypes = paramTypes == null ? new Class<?>[0] : paramTypes;
        this.viewType = view == null ? MethodInterfaceType.Bean : view;

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
