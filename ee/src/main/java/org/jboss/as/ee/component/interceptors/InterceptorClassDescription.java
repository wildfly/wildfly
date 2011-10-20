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
package org.jboss.as.ee.component.interceptors;

import org.jboss.invocation.proxy.MethodIdentifier;

/**
 * Post class loading description of an interceptor class.
 *
 * @author Stuart Douglas
 */
public class InterceptorClassDescription {

    public static final InterceptorClassDescription EMPTY_INSTANCE = new Builder().build();
    private final MethodIdentifier aroundInvoke;
    private final MethodIdentifier aroundTimeout;
    private final MethodIdentifier preDestroy;
    private final MethodIdentifier postConstruct;

    public InterceptorClassDescription(final MethodIdentifier aroundInvoke, final MethodIdentifier aroundTimeout, final MethodIdentifier preDestroy, final MethodIdentifier postConstruct) {
        this.aroundInvoke = aroundInvoke;
        this.aroundTimeout = aroundTimeout;
        this.preDestroy = preDestroy;
        this.postConstruct = postConstruct;
    }

    /**
     * Merges two descriptors, either of the paraemters will be null.
     *
     * this method will never return null;
     * @param existing
     * @param override
     * @return
     */
    public static InterceptorClassDescription merge(InterceptorClassDescription existing, InterceptorClassDescription override) {
        if(existing == null && override ==null) {
            return EMPTY_INSTANCE;
        }
        if(override == null) {
            return existing;
        }
        if(existing == null) {
            return override;
        }
        final Builder builder = builder(existing);
        if(override.getAroundInvoke() != null) {
            builder.setAroundInvoke(override.getAroundInvoke());
        }
        if(override.getAroundTimeout() != null) {
            builder.setAroundTimeout(override.getAroundTimeout());
        }
        if(override.getPostConstruct() != null) {
            builder.setPostConstruct(override.getPostConstruct());
        }
        if(override.getPreDestroy() != null) {
            builder.setPreDestroy(override.getPreDestroy());
        }
        return builder.build();
    }

    /**
     * Constructs a new empty builder
     * @return An empty builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     *
     * @param base The existing description, or null for an empty builder
     * @return A builder based on the existing description
     */
    public static Builder builder(InterceptorClassDescription base) {
        if(base == null) {
            return new Builder();
        }
        return new Builder(base);
    }


    public static class Builder {

        private MethodIdentifier aroundInvoke;
        private MethodIdentifier aroundTimeout;
        private MethodIdentifier preDestroy;
        private MethodIdentifier postConstruct;

        Builder() {

        }

        Builder(InterceptorClassDescription existing) {
            this.aroundInvoke = existing.aroundInvoke;
            this.aroundTimeout = existing.aroundTimeout;
            this.preDestroy = existing.preDestroy;
            this.postConstruct = existing.postConstruct;
        }

        public InterceptorClassDescription build() {
            return new InterceptorClassDescription(aroundInvoke, aroundTimeout, preDestroy, postConstruct);
        }


        public MethodIdentifier getAroundInvoke() {
            return aroundInvoke;
        }

        public void setAroundInvoke(final MethodIdentifier aroundInvoke) {
            this.aroundInvoke = aroundInvoke;
        }

        public MethodIdentifier getAroundTimeout() {
            return aroundTimeout;
        }

        public void setAroundTimeout(final MethodIdentifier aroundTimeout) {
            this.aroundTimeout = aroundTimeout;
        }

        public MethodIdentifier getPostConstruct() {
            return postConstruct;
        }

        public void setPostConstruct(final MethodIdentifier postConstruct) {
            this.postConstruct = postConstruct;
        }

        public MethodIdentifier getPreDestroy() {
            return preDestroy;
        }

        public void setPreDestroy(final MethodIdentifier preDestroy) {
            this.preDestroy = preDestroy;
        }
    }

    public MethodIdentifier getAroundInvoke() {
        return aroundInvoke;
    }

    public MethodIdentifier getAroundTimeout() {
        return aroundTimeout;
    }

    public MethodIdentifier getPostConstruct() {
        return postConstruct;
    }

    public MethodIdentifier getPreDestroy() {
        return preDestroy;
    }
}
