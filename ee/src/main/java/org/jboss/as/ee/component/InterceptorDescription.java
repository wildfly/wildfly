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

package org.jboss.as.ee.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for an interceptor bound to an EE component.
 *
 * @author John E. Bailey
 */
public class InterceptorDescription extends AbstractLifecycleCapableDescription {
    private final String interceptorClassName;

    private final List<InterceptorMethodDescription> aroundInvokeMethods;

    /**
     * Create an instance with the interceptor class and the resource configurations.
     *
     * @param interceptorClassName the interceptor class name
     */
    public InterceptorDescription(final String interceptorClassName) {
        this.interceptorClassName = interceptorClassName;
        this.aroundInvokeMethods = new ArrayList<InterceptorMethodDescription>();
    }

    /**
     * Get the interceptor class.
     *
     * @return The interceptor class
     */
    public String getInterceptorClassName() {
        return interceptorClassName;
    }

    /**
     * Adds an AroundInvoke method to the interceptor. Superclass AroundInvoke methods
     * should be added first to maintain the correct ordering as per the interceptor spec
     *
     * @param aroundInvokeMethod The method to add
     */
    public void addAroundInvokeMethod(InterceptorMethodDescription aroundInvokeMethod) {
        aroundInvokeMethods.add(aroundInvokeMethod);
    }

    public List<InterceptorMethodDescription> getAroundInvokeMethods() {
        return Collections.unmodifiableList(aroundInvokeMethods);
    }
}
