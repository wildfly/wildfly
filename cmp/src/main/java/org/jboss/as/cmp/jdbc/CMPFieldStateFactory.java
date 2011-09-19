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
package org.jboss.as.cmp.jdbc;

/**
 * Implementations of this interface are used to create and compare field states
 * for equality.
 *
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 */
public interface CMPFieldStateFactory {
    /**
     * Calculates and returns an object that represents the state of the field value.
     * The states produced by this method will be used to check whether the field
     * is dirty at synchronization time.
     *
     * @param fieldValue field's value.
     * @return an object representing the field's state.
     */
    Object getFieldState(Object fieldValue);

    /**
     * Checks whether the field's state <code>state</code>
     * is equal to the field value's state (possibly, calculated with
     * the <code>getFieldState()</code> method).
     *
     * @param state      the state to compare with field value's state.
     * @param fieldValue field's value, the state of which will be compared
     *                   with <code>state</code>.
     * @return true if <code>state</code> equals to <code>fieldValue</code>'s state.
     */
    boolean isStateValid(Object state, Object fieldValue);
}
