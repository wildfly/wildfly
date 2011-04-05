/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.invocation.InterceptorFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class used to allow a description to support life-cycle (PostConstruct and PreDestroy) descriptions.
 *
 * @author John Bailey
 */
public class AbstractLifecycleCapableDescription  {
    private final List<InterceptorMethodDescription> interceptorPostConstructs = new ArrayList<InterceptorMethodDescription>();
    private final List<InterceptorMethodDescription> interceptorPreDestroys = new ArrayList<InterceptorMethodDescription>();


    private final List<InterceptorMethodDescription> postConstructs = new ArrayList<InterceptorMethodDescription>();
    private final List<InterceptorMethodDescription> preDestroys = new ArrayList<InterceptorMethodDescription>();

    private final List<InterceptorFactory> postConstructInterceptorFactories = new ArrayList<InterceptorFactory>();
    private final List<InterceptorFactory> preDestroyInterceptorFactories = new ArrayList<InterceptorFactory>();

    /**
     * Get the post-construct lifecycle method configurations for interceptor classes attached to this component
     *
     * @return the post-construct lifecycle method configurations
     */
    public List<InterceptorMethodDescription> getInterceptorPostConstructs() {
        return interceptorPostConstructs;
    }

    /**
     * Get the pre-destroy lifecycle method configurations for interceptor classes attached to this component
     *
     * @return the pre-destroy lifecycle method configurations
     */
    public List<InterceptorMethodDescription> getInterceptorPreDestroys() {
        return interceptorPreDestroys;
    }

    /**
     * Adds a PostConstruct method
     * @param methodDescription The method to add
     */
    public void addPostConstruct(InterceptorMethodDescription methodDescription) {
        if(methodDescription.isDeclaredOnTargetClass()) {
            postConstructs.add(methodDescription);
        } else {
            interceptorPostConstructs.add(methodDescription);
        }
    }

    public void addPreDestroy(InterceptorMethodDescription methodDescription) {
        if(methodDescription.isDeclaredOnTargetClass()) {
            preDestroys.add(methodDescription);
        } else {
            interceptorPreDestroys.add(methodDescription);
        }
    }

    /**
     * Get pre-destroy lifecycle methods declared on the component itself
     *
     * @return The pre-destroy methods
     */
    public List<InterceptorMethodDescription> getPreDestroys() {
        return preDestroys;
    }

    /**
     * Get post-construct lifecycle methods declared on the component itself
     *
     * @return The post-construct methods
     */
    public List<InterceptorMethodDescription> getPostConstructs() {
        return postConstructs;
    }

    /**
     * Adds a InterceptorFactory that runs at the start of the post construct chain
     * @param factory The factory to add
     */
    public void addPostConstructInterceptorFactory(InterceptorFactory factory) {
        postConstructInterceptorFactories.add(factory);
    }

    /**
     * Adds a InterceptorFactory that runs at the start of the pre destroy chain
     * @param factory The factory to add
     */
    public void addPreDestroyInterceptorFactory(InterceptorFactory factory) {
        preDestroyInterceptorFactories.add(factory);
    }

    /**
     *
     * @return A list of interceptor factories that run at the start of the post construct chain
     */
    public List<InterceptorFactory> getPostConstructInterceptorFactories() {
        return postConstructInterceptorFactories;
    }

    /**
     *
     * @return A list of interceptor factories that run at the start of the pre-destry chain
     */
    public List<InterceptorFactory> getPreDestroyInterceptorFactories() {
        return preDestroyInterceptorFactories;
    }
}
