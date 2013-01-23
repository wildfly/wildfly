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

import org.jboss.as.controller.PathElement;

/**
 * A transformation description builder.
 *
 * @author Emanuel Muckenhuber
 */
public interface TransformationDescriptionBuilder {

    /**
     * Build the transformation description. Modifications to the builder won't affect the built description after this
     * method was called.
     *
     * @return the transformation description
     */
    TransformationDescription build();

    public static class Factory {

        /**
         * Create a resource builder instance.
         *
         * @return the transformation builder
         * @deprecated experimental
         */
        public static ResourceTransformationDescriptionBuilder createInstance(final PathElement pathElement) {
            return new ResourceTransformationDescriptionBuilderImpl(pathElement);
        }

        /**
         * Create a builder instance.
         *
         * @return the transformation builder
         * @deprecated experimental
         */
        public static DiscardTransformationDescriptionBuilder createDiscardInstance(PathElement pathElement) {
            return new DiscardTransformationDescriptionBuilder(pathElement);
        }

    }

}
