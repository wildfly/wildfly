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
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class LifecycleCapableDescription {

    private final List<InterceptorMethodDescription> postConstructs = new ArrayList<InterceptorMethodDescription>();
    private final List<InterceptorMethodDescription> preDestroys = new ArrayList<InterceptorMethodDescription>();

    /**
     * Add a post-construct method description.
     *
     * @param methodDescription the method to add
     */
    public void addPostConstruct(InterceptorMethodDescription methodDescription) {
        postConstructs.add(methodDescription);
    }

    /**
     * Add a pre-destroy method description.
     *
     * @param methodDescription the method to add
     */
    public void addPreDestroy(InterceptorMethodDescription methodDescription) {
        preDestroys.add(methodDescription);
    }

    /**
     * Get pre-destroy lifecycle methods declared on the component itself.
     *
     * @return the pre-destroy methods
     */
    public List<InterceptorMethodDescription> getPreDestroys() {
        return preDestroys;
    }

    /**
     * Get post-construct lifecycle methods declared on the component itself.
     *
     * @return the post-construct methods
     */
    public List<InterceptorMethodDescription> getPostConstructs() {
        return postConstructs;
    }
}
