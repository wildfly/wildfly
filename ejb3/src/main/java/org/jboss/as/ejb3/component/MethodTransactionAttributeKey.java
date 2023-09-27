/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component;

import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;

/**
 * @author Stuart Douglas
 */
public class MethodTransactionAttributeKey {
    private final MethodInterfaceType methodIntf;
    private final MethodIdentifier methodIdentifier;

    public MethodTransactionAttributeKey(final MethodInterfaceType methodIntf, final MethodIdentifier methodIdentifier) {
        this.methodIntf = methodIntf;
        this.methodIdentifier = methodIdentifier;
    }

    public MethodIdentifier getMethodIdentifier() {
        return methodIdentifier;
    }

    public MethodInterfaceType getMethodIntf() {
        return methodIntf;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final MethodTransactionAttributeKey that = (MethodTransactionAttributeKey) o;

        if (!methodIdentifier.equals(that.methodIdentifier)) return false;
        if (methodIntf != that.methodIntf) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = methodIntf.hashCode();
        result = 31 * result + methodIdentifier.hashCode();
        return result;
    }
}
