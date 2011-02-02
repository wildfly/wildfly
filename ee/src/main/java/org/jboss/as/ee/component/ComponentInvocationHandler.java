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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * An invocation handler for a component.  Used to send method invocations to a component.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ComponentInvocationHandler extends InvocationHandler {

    /**
     * Get the component associated with this invocation handler.
     *
     * @return the component
     */
    Component getComponent();

    /**
     * Get the view class for this invocation handler.
     *
     * @return the view class
     */
    Class<?> getViewClass();

    /**
     * Get the list of allowed methods for this invocation handler.  The handler will only accept invocations on
     * these exact {@code Method} objects.
     *
     * @return the list of methods
     */
    Collection<Method> allowedMethods();

    /**
     * Destroy this handler.  This method should be called when the client proxy is no longer
     * in use; this may destroy the instance, or do nothing at all.
     */
    void destroy();
}
