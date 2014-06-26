/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
