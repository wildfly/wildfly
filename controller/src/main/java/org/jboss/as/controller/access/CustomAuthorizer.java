/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.access;

/**
 * Interface for non-standard implementations of the {@link Authorizer} super-interface.
 * Custom authorizers will use this interface to allow integration with the WildFly management
 * layer.
 * <p>
 * Implementations of this interface must be loadable via the {@code java.util.ServiceLoader} mechanism.
 * </p>
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public interface CustomAuthorizer extends Authorizer {

    /**
     * Provides a reference to the {@link AuthorizerConfiguration} to the authorizer. This allows
     * the authorizer to take advantage of access control configuration information provided via
     * the WildFly management API. How, or even if, this information is used is up to the authorizer
     * implementation.
     * <p>
     * This method will be called before any methods in the {@code Authorizer} interface are called.
     * </p>
     *
     * @param configuration the configuration. Will not be {@code null}
     *
     * @return a description of this authorizer
     * @throws IllegalStateException if the configuration has already been set
     */
    AuthorizerDescription setAuthorizerConfiguration(AuthorizerConfiguration configuration);

    /**
     * Notification that the authorizer is no longer in use and should shut down.
     */
    void shutdown();

}
