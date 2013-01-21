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
 * A path address transformation step. This specific type of transformer get registered alongside a resource transformer
 * entry and can be used to transform the {@linkplain PathAddress} for operation and resource transformations.
 *
 * In general this transformer is only responsible for changing the current {@linkplain PathElement} and the delegate
 * to other address transformers in the chain using {@linkplain Builder#next(org.jboss.as.controller.PathElement...)}.
 *
 * @author Emanuel Muckenhuber
 */
public interface PathAddressTransformer {

    /**
     * Transform a address.
     *
     * @param current the current path element
     * @param builder the address builder
     * @return the path address
     */
    PathAddress transform(PathElement current, Builder builder);

    PathAddressTransformer DEFAULT = new PathAddressTransformer() {

        @Override
        public PathAddress transform(PathElement current, Builder builder) {
            return builder.next(current);
        }

    };

    public class BasicPathAddressTransformer implements PathAddressTransformer {

        private final PathElement swap;
        public BasicPathAddressTransformer(PathElement swap) {
            this.swap = swap;
        }

        @Override
        public PathAddress transform(PathElement current, Builder builder) {
            return builder.next(swap);
        }

    }

    public class ReplaceElementKey implements PathAddressTransformer {

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
         * Get the unmodified (original) address.
         *
         * @return the original address
         */
        PathAddress getOriginal();

        /**
         * Get the current address, from the builder.
         *
         * @return the current address
         */
        PathAddress getCurrent();

        /**
         * Get the remaining elements left for transformation.
         *
         * @return the remaining elements for this address
         */
        PathAddress getRemaining();

        /**
         * Append a element to the current address and continue to the next transformer in the chain.
         *
         * @param elements the elements to append
         * @return the transformed address
         */
        PathAddress next(final PathElement... elements);

    }

    class BuilderImpl implements Builder {

        private final Iterator<PathAddressTransformer> transformers;
        private final PathAddress original;

        private PathAddress current = PathAddress.EMPTY_ADDRESS;
        private int idx = 0;

        protected BuilderImpl(Iterator<PathAddressTransformer> transformers, PathAddress original) {
            this.transformers = transformers;
            this.original = original;
        }

        @Override
        public PathAddress getOriginal() {
            return original;
        }

        @Override
        public PathAddress getCurrent() {
            return current;
        }

        @Override
        public PathAddress getRemaining() {
            return original.subAddress(idx);
        }

        @Override
        public PathAddress next(final PathElement... elements) {
            current = current.append(elements);
            final int remaining = original.size() - idx;
            if(remaining == 0) {
                return current;
            }
            if(transformers.hasNext()) {
                final PathAddressTransformer transformer = transformers.next();
                final PathElement next = original.getElement(idx++);
                return transformer.transform(next, this);
            } else {
                // This may not be an error?
                // return current.append(getRemaining());
                throw new IllegalStateException();
            }
        }

        protected PathAddress start() {
            if(original == PathAddress.EMPTY_ADDRESS || original.size() == 0) {
                return PathAddress.EMPTY_ADDRESS;
            }
            if(transformers.hasNext()) {
                final PathAddressTransformer transformer = transformers.next();
                final PathElement next = original.getElement(idx++);
                return transformer.transform(next, this);
            } else {
                return original;
            }
        };

    }

}
