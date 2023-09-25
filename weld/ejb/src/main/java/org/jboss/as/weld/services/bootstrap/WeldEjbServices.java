/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.services.bootstrap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.weld.ejb.EjbDescriptorImpl;
import org.jboss.as.weld.ejb.SessionObjectReferenceImpl;
import org.jboss.as.weld.ejb.StatefulSessionObjectReferenceImpl;
import org.jboss.weld.ejb.api.SessionObjectReference;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.ejb.spi.InterceptorBindings;

/**
 * EjbServices implementation
 */
public class WeldEjbServices implements EjbServices {

    private volatile Map<String, InterceptorBindings> bindings = Collections.emptyMap();

    @Override
    public synchronized void registerInterceptors(EjbDescriptor<?> ejbDescriptor, InterceptorBindings interceptorBindings) {
        final Map<String, InterceptorBindings> bindings = new HashMap<String, InterceptorBindings>(this.bindings);
        bindings.put(ejbDescriptor.getEjbName(), interceptorBindings);
        this.bindings = bindings;
    }

    @Override
    public SessionObjectReference resolveEjb(EjbDescriptor<?> ejbDescriptor) {
        if (ejbDescriptor.isStateful()) {
            return new StatefulSessionObjectReferenceImpl((EjbDescriptorImpl<?>) ejbDescriptor);
        } else {
            return new SessionObjectReferenceImpl((EjbDescriptorImpl<?>) ejbDescriptor);
        }
    }

    @Override
    public void cleanup() {
        bindings.clear();
    }

    public InterceptorBindings getBindings(String ejbName) {
        return bindings.get(ejbName);
    }
}
