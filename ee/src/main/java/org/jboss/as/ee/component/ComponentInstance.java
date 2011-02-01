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

import java.io.Serializable;
import org.jboss.invocation.Interceptor;

/**
 * An instance of a Java EE component.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ComponentInstance extends Serializable {

    /**
     * Get the component associated with this instance.
     *
     * @return the component
     */
    Component getComponent();

    /**
     * Get the interceptor entry point for this instance.  This interceptor will be wired with the interceptor instances
     * associated with this component instance.
     *
     * @return the interceptor entry point
     */
    Interceptor getInterceptor();

    /**
     * Get the actual object instance.  The object instance has all injections filled.
     *
     * @return the instance
     */
    Object getInstance();

    /**
     * Create a new local client proxy for this component instance.
     *
     * @return the proxy instance
     */
    Object createLocalClientProxy();
}
