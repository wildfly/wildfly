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

package org.jboss.as.controller.transform;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;

import java.util.Iterator;

/**
 * This transformer is specific to address transformation. It will get called prior to any operation or resource
 * transformation and is mostly only response for transformation the current path element.
 *
 * @author Emanuel Muckenhuber
 */
public interface PathTransformation {

    /**
     * Transform a address.
     *
     * @param current the current path element
     * @param builder the address builder
     * @return the path address
     */
    PathAddress transform(PathElement current, Builder builder);

    PathTransformation DEFAULT = new PathTransformation() {

        @Override
        public PathAddress transform(PathElement current, Builder builder) {
            return builder.next(current);
        }

    };

    public class BasicPathTransformation implements PathTransformation {

        private final PathElement swap;
        public BasicPathTransformation(PathElement swap) {
            this.swap = swap;
        }

        @Override
        public PathAddress transform(PathElement current, Builder builder) {
            return builder.next(swap);
        }

    }

    public class ReplaceElementKey implements PathTransformation {

        private final String newKey;
        public ReplaceElementKey(String newKey) {
            this.newKey = newKey;
        }

        @Override
        public PathAddress transform(PathElement current, Builder builder) {
            final PathElement newElement = PathElement.pathElement(newKey, current.getValue());
            return builder.next(newElement);
        }

    }

    public interface Builder {

        /**
         * Get the original address.
         *
         * @return the origial address
         */
        PathAddress getOriginal();

        /**
         * Get the current address.
         *
         * @return the current address
         */
        PathAddress getCurrent();

        /**
         * Get the remaining elments for this address.
         *
         * @return the remmaining elements for this address
         */
        PathAddress getRemaining();

        /**
         * Append a element to the current address and continue.
         *
         * @param elements the elements to append
         * @return the transformed address
         */
        PathAddress next(final PathElement... elements);

    }

    public class BuilderImpl implements Builder {

        private final Iterator<PathTransformation> transformers;
        private final PathAddress original;

        private PathAddress current = PathAddress.EMPTY_ADDRESS;
        private int idx = 0;

        protected BuilderImpl(Iterator<PathTransformation> transformers, PathAddress original) {
            this.transformers = transformers;
            this.original = original;
        }

        /**
         * Get the original address.
         *
         * @return the origial address
         */
        public PathAddress getOriginal() {
            return original;
        }

        /**
         * Get the current address.
         *
         * @return the current address
         */
        public PathAddress getCurrent() {
            return current;
        }

        /**
         * Get the remaining elments for this address.
         *
         * @return the remmaining elements for this address
         */
        public PathAddress getRemaining() {
            return original.subAddress(idx);
        }

        /**
         * Append a element to the current address and continue.
         *
         * @param elements the elements to append
         * @return the transformed address
         */
        public PathAddress next(final PathElement... elements) {
            current = current.append(elements);
            final int remaining = original.size() - idx;
            if(remaining == 0) {
                return current;
            }
            if(transformers.hasNext()) {
                final PathTransformation transformer = transformers.next();
                final PathElement next = original.getElement(idx++);
                return transformer.transform(next, this);
            } else {
                // This may not be an error?
                // return current.append(getRemaining());
                throw new IllegalStateException();
            }
        }

        protected PathAddress start() {
            if(transformers.hasNext()) {
                final PathTransformation transformer = transformers.next();
                final PathElement next = original.getElement(idx++);
                return transformer.transform(next, this);
            } else {
                return original;
            }
        };

    }

}
