/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.weld.interceptors;

import java.io.Serializable;
import java.util.Map;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Interceptor;

import org.jboss.as.weld.spi.InterceptorInstances;
import org.jboss.weld.serialization.spi.helpers.SerializableContextualInstance;

/**
* @author Stuart Douglas
*/
public class WeldInterceptorInstances implements InterceptorInstances, Serializable {
    private static final long serialVersionUID = 1L;
    private final CreationalContext<Object> creationalContext;
    private final Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> interceptorInstances;

    public WeldInterceptorInstances(final CreationalContext<Object> creationalContext, final Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> interceptorInstances) {
        this.creationalContext = creationalContext;
        this.interceptorInstances = interceptorInstances;
    }

    @Override
    public CreationalContext<Object> getCreationalContext() {
        return creationalContext;
    }

    @Override
    public Map<String, SerializableContextualInstance<Interceptor<Object>, Object>> getInstances() {
        return interceptorInstances;
    }
}
