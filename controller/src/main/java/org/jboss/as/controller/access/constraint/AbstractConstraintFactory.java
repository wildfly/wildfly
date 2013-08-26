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

package org.jboss.as.controller.access.constraint;

/**
 * Abstract superclass for {@link ConstraintFactory} implementations.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
abstract class AbstractConstraintFactory implements ConstraintFactory {


    @Override
    public int compareTo(ConstraintFactory o) {
        if (o instanceof  AbstractConstraintFactory) {
            // We have no particular preference, so defer to other
            int otherPref = ((AbstractConstraintFactory) o).internalCompare(this);
            if (otherPref == 0) {
                // Other also has no preference. We came first, so we stay first
                return this.equals(o) ? 0 : -1;
            }
            // Defer to other
            return otherPref * -1;
        }

        return -1;
    }

    /**
     * Compare this {@link AbstractConstraintFactory} to another. Similar contract to {@link Comparable#compareTo(Object)}
     * except that a return value of {@code 0} does not imply equality; rather it implies indifference with respect
     * to order. The intended use for this method is in {@link Comparable#compareTo(Object)} implementations where
     * the class implementing the method has no preference with respect to order and is willing to go with the
     * preference of the passed in object if it has one.
     *
     * @param other  the other constraint factory
     */
    protected abstract int internalCompare(AbstractConstraintFactory other);
}
