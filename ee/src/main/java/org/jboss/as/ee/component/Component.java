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

import javax.naming.Context;

/**
 * Common contract for an EE component.  Implementations of this will be available as a service and can be used as the
 * backing for a JNDI ObjectFactory reference.
 *
 * @author John Bailey
 */
public interface Component {

    /**
     * Start operation called when the Component is available.
     */
    void start();

    /**
     * Stop operation called when the Component is no longer available.
     */
    void stop();

    /**
     * Create a proxy instance for this component.
     *
     * @return The proxy
     */
    Object createProxy();

    /**
     * Return an instance of this component.  Implementations can use any mechanism to retrieve the
     * bean including pooling, singleton or construction on every call to this method.
     *
     * @return A bean instance
     */
    Object getInstance();

    /**
     * Return an instance to the component.  This should be called whenever a consumer of the bean is no longer
     * using the instance.  This can be used to run post-constructs, cleanup, or to return the instance to a pool.
     *
     * @param instance The bean instance
     */
    void returnInstance(Object instance);

    /**
     * Return the naming context for this components java:comp context.
     *
     * @return The java:comp naming context
     */
    Context getComponentContext();

    /**
     * Return the naming context for this components java:module context.
     *
     * @return The java:module naming context
     */
    Context getModuleContext();

    /**
     * Return the naming context for this components java:app context.
     *
     * @return The java:app naming context
     */
    Context getApplicationContext();
}
