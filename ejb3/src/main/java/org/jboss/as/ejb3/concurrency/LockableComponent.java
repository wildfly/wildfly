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

package org.jboss.as.ejb3.concurrency;

import javax.ejb.LockType;
import java.lang.reflect.Method;

/**
 * A {@link LockableComponent} represents the runtime component of an EJB. It is meant to provide access to EJB
 * metadata for {@link javax.ejb.Lock} management interceptors.
 * <p/>
 *
 * @author Jaikiran Pai
 */
public interface LockableComponent {
    /**
     * Returns the {@link javax.ejb.LockType} applicable to the passed <code>method</code>.
     * <p/>
     * If there is no explicit {@link javax.ejb.LockType} specified for the passed <code>method</code> then this method
     * must return the {@link javax.ejb.LockType} applicable at the component level or the default applicable {@link javax.ejb.LockType}.
     * This method must *not* return a null value.
     *
     * @param method The method for which the {@link javax.ejb.LockType} is being queried. Cannot be null.
     * @return
     * @throws IllegalArgumentException If the passed <code>method</code> is null
     */
    LockType getLockType(Method method);

    /**
     * Returns the {@link AccessTimeoutDetails} applicable for the passed <code>method</code>.
     *
     * @param method
     * @return
     */
    AccessTimeoutDetails getAccessTimeout(Method method);

    /**
     * Returns the default applicable {@link AccessTimeoutDetails} for a component. This value will be used if a method doesn't
     * explicitly specify an {@link AccessTimeoutDetails}
     *
     * @return
     */
    AccessTimeoutDetails getDefaultAccessTimeout();

    /**
     *
     * @return The name of this component
     */
    String getComponentName();
}
