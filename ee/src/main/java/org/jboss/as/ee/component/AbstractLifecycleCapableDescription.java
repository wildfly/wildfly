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

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class used to allow a description to support life-cycle (PostConstruct and PreDestroy) descriptions.
 *
 * @author John Bailey
 */
public class AbstractLifecycleCapableDescription extends AbstractInjectableDescription {
    private final List<InterceptorMethodDescription> postConstructs = new ArrayList<InterceptorMethodDescription>();
    private final List<InterceptorMethodDescription> preDestroys = new ArrayList<InterceptorMethodDescription>();

    /**
     * Adds a post construct method
     *
     * @param methodDescription The method
     */
    public void addPostConstructMethod(InterceptorMethodDescription methodDescription) {
        postConstructs.add(methodDescription);
    }

    /**
     * Get the post-construct lifecycle method configurations.
     *
     * @return the post-construct lifecycle method configurations
     */
    public List<InterceptorMethodDescription> getPostConstructs() {
        return postConstructs;
    }

    /**
     * Adds a pre destroy method
     *
     * @param methodDescription The method
     */
    public void addPreDestroyMethod(InterceptorMethodDescription methodDescription) {
        preDestroys.add(methodDescription);
    }

    /**
     * Get the pre-destroy lifecycle method configurations.
     *
     * @return the pre-destroy lifecycle method configurations
     */
    public List<InterceptorMethodDescription> getPreDestroys() {
        return preDestroys;
    }

}
