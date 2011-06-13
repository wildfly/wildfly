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
package org.jboss.as.test.spec.ejb3.interceptor.lifecycle.chains;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

/**
 * @author Stuart Douglas
 */
@Stateless
@Interceptors(LifecycleInterceptorWithProceed.class)
public class InterceptedWithProceedSFSB {

    boolean postConstructCalled = false;

    public void doStuff() {

    }

    /**
     * This method should be called, after proceed is called from the interceptor, in the same call stack as the interceptors
     * post construct method. (See 'Multiple Callback Interceptor Methods for a Life Cycle Callback Event' in the interceptors
     * specification.
     */
    @PostConstruct
    public void postConstruct() {
        if (!LifecycleInterceptorWithProceed.postConstruct) {
            throw new AssertionError("Postconstruct call not interceptored as started");
        }
        if (LifecycleInterceptorWithProceed.postConstructFinished) {
            throw new AssertionError("Postconstruct call intercepted as done before completed");
        }
        postConstructCalled = true;
    }

    public boolean isPostConstructCalled() {
        return postConstructCalled;
    }
}
