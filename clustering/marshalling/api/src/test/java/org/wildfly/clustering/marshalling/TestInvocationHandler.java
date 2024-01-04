/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author Paul Ferraro
 */
public class TestInvocationHandler implements InvocationHandler, Serializable {
    private static final long serialVersionUID = 1903022476673232647L;

    private final Object value;

    public TestInvocationHandler(Object value) {
        this.value = value;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }

    public Object getValue() {
        return this.value;
    }
}
