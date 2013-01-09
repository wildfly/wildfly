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
public abstract class TransformationDescriptionBuilder {

    /**
     * Register an alias, inheriting all transformation rules from this resource.
     *
     * @param element the path element
     * @return the transformation
     */
    // TODO allow some sort of parent? probably only scoped to a given context like subsystem (or host, server, domain)
    public abstract TransformationDescriptionBuilder redirectTo(PathElement element);

    /**
     * Build the transformation description.
     *
     * @return the transformation description
     */
    public abstract TransformationDescription build();

    /**
     * Path transformation for this current level.
     *
     * @return the path transformer
     */
    protected PathTransformation getPathTransformation() {
        return PathTransformation.DEFAULT;
    }

    public static class Factory {

        /**
         * Create a builder instance.
         *
         * @return the transformation builder
         * @deprecated experimental
         */
        @Deprecated
        public static ResourceTransformationDescriptionBuilder createInstance(final PathElement pathElement) {
            return new ResourceTransformationDescriptionBuilderImpl(pathElement);
        }

        /**
         * Create a builder instance.
         *
         * @return the transformation builder
         * @deprecated experimental
         */
        @Deprecated
        public static TransformationDescriptionBuilder createDiscardInstance(final PathElement pathElement) {
            return new TransformationDescriptionBuilder() {
                @Override
                public TransformationDescriptionBuilder redirectTo(PathElement element) {
                    return this;
                }

                @Override
                public TransformationDescription build() {
                    return new DiscardDefinition(pathElement);
                }
            };
        }

    }

}
