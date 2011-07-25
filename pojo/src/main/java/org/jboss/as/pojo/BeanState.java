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

package org.jboss.as.pojo;

/**
 * A MC bean state.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public enum BeanState {
    NOT_INSTALLED,
    DESCRIBED,
    INSTANTIATED,
    CONFIGURED,
    CREATE,
    START,
    INSTALLED;

    /**
     * Get the next state.
     *
     * @return the next state
     */
    public BeanState next() {
        return values()[ordinal() + 1];
    }

    /**
     * Is this instance before state @param.
     *
     * @param state the state to check
     * @return true if before, false otherwise
     */
    public boolean isBefore(BeanState state) {
        return state.ordinal() > ordinal();
    }

    /**
     * Is this instance after state @param.
     *
     * @param state the state to check
     * @return true if after, false otherwise
     */
    public boolean isAfter(BeanState state) {
        return state.ordinal() < ordinal();
    }
}
