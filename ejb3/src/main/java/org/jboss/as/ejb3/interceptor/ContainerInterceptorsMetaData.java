/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.interceptor;

import org.jboss.metadata.ejb.spec.InterceptorBindingMetaData;
import org.jboss.metadata.ejb.spec.InterceptorBindingsMetaData;

/**
 * Holds the interceptor bindings information configured within a <code>container-interceptors</code>
 * element of jboss-ejb3.xml
 *
 * @author Jaikiran Pai
 */
public class ContainerInterceptorsMetaData {

    private final InterceptorBindingsMetaData interceptorBindings = new InterceptorBindingsMetaData();

    public InterceptorBindingsMetaData getInterceptorBindings() {
        return this.interceptorBindings;
    }

    void addInterceptorBinding(final InterceptorBindingMetaData interceptorBinding) {
        this.interceptorBindings.add(interceptorBinding);
    }
}
