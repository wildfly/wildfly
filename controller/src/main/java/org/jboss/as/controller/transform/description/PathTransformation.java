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

package org.jboss.as.controller.transform.description;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;

/**
 * @author Emanuel Muckenhuber
 */
abstract class PathTransformation {

    abstract PathAddress transform(PathAddress current, PathElement element);

    static PathTransformation DEFAULT = new PathTransformation() {

        @Override
        PathAddress transform(final PathAddress current, final PathElement element) {
            return current.append(element);
        }

    };

    static class BasicPathTransformation extends PathTransformation {

        private final PathElement swap;
        BasicPathTransformation(PathElement swap) {
            this.swap = swap;
        }

        @Override
        PathAddress transform(PathAddress current, PathElement element) {
            return current.append(swap);
        }

    }

    static class ReplaceElementKey extends PathTransformation {

        private final String newKey;
        ReplaceElementKey(String newKey) {
            this.newKey = newKey;
        }

        @Override
        PathAddress transform(PathAddress current, PathElement element) {
            final PathElement newElement = PathElement.pathElement(newKey, element.getValue());
            return current.append(newElement);
        }
    }

}
