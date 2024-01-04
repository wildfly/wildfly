/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

/**
 * A description of an {@link jakarta.interceptor.Interceptor @Interceptor} annotation value or its equivalent
 * deployment descriptor construct.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class InterceptorDescription {
    private final String interceptorClassName;

    public InterceptorDescription(final String interceptorClassName) {
        this.interceptorClassName = interceptorClassName;
    }

    public String getInterceptorClassName() {
        return interceptorClassName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final InterceptorDescription that = (InterceptorDescription) o;

        if (interceptorClassName != null ? !interceptorClassName.equals(that.interceptorClassName) : that.interceptorClassName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return interceptorClassName != null ? interceptorClassName.hashCode() : 0;
    }
}
