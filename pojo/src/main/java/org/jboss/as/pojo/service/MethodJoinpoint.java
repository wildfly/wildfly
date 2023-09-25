/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import java.lang.reflect.Method;

/**
 * Method joinpoint.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class MethodJoinpoint extends TargetJoinpoint {
    private final Method method;

    public MethodJoinpoint(Method method) {
        this.method = method;
    }

    @Override
    public Object dispatch() throws Throwable {
        return method.invoke(getTarget().getValue(), toObjects(method.getGenericParameterTypes()));
    }
}
