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

import java.util.List;
import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ModelVersionRange;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathAddressTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;

/**
 * The final transformation description including child resources.
 *
 * @author Emanuel Muckenhuber
 */
public interface TransformationDescription {

    /**
     * Get the path for this transformation description.
     *
     * @return the path element
     */
    PathElement getPath();

    /**
     * Get the path transformation for this level.
     *
     * @return the path transformation
     */
    PathAddressTransformer getPathAddressTransformer();

    /**
     * Get the default operation transformer.
     *
     * @return the operation transformer
     */
    OperationTransformer getOperationTransformer();

    /**
     * Get the resource transformer.
     *
     * @return the resource transformer
     */
    ResourceTransformer getResourceTransformer();

    /**
     * Get the operation transformers for specific operations.
     *
     * @return the operation transformer overrides
     */
    Map<String, OperationTransformer> getOperationTransformers();

    /**
     * Get the children descriptions.
     *
     * @return the children
     */
    List<TransformationDescription> getChildren();

    /**
     * If this is a discarded or rejected resource it returns {@code true}
     *
     * @return {@code true} if this is a discarded or rejected resource
     */
    boolean isInherited();

    public static final class Tools {

        private Tools() {
            //
        }

        /**
         * Register a transformation description as a sub-resource at a given {@linkplain TransformersSubRegistration}.
         *
         * @param description the transformation description.
         * @param parent the parent registration
         * @return the created sub registration
         */
        public static TransformersSubRegistration register(final TransformationDescription description, TransformersSubRegistration parent) {
            final TransformersSubRegistration registration =
                    parent.registerSubResource(
                            description.getPath(),
                            description.getPathAddressTransformer(),
                            description.getResourceTransformer(),
                            description.getOperationTransformer(),
                            description.isInherited());
            for (final Map.Entry<String, OperationTransformer> entry : description.getOperationTransformers().entrySet()) {
                registration.registerOperationTransformer(entry.getKey(), entry.getValue());
            }
            for (final TransformationDescription child : description.getChildren()) {
                register(child, registration);
            }
            return registration;
        }

        /**
         * Register a transformation description as a sub-resource at a given {@linkplain SubsystemRegistration}.
         *
         * @param description the subsystem transformation description
         * @param registration the subsystem registrations
         * @param versions the model versions the transformation description applies to
         * @return the created sub registration
         */
        public static TransformersSubRegistration register(TransformationDescription description, SubsystemRegistration registration, ModelVersion... versions) {
            return register(description, registration, ModelVersionRange.Versions.range(versions));
        }

        /**
         * Register a transformation description as a sub-resource at a given {@linkplain SubsystemRegistration}.
         *
         * @param description the subsystem transformation description
         * @param registration the subsystem registrations
         * @param range the model version range the transformation applies to
         * @return the create sub registration
         */
        public static TransformersSubRegistration register(TransformationDescription description, SubsystemRegistration registration, ModelVersionRange range) {
            final TransformersSubRegistration subRegistration = registration.registerModelTransformers(range, description.getResourceTransformer(), description.getOperationTransformer());
            for (final Map.Entry<String, OperationTransformer> entry : description.getOperationTransformers().entrySet()) {
                subRegistration.registerOperationTransformer(entry.getKey(), entry.getValue());
            }
            for (final TransformationDescription child : description.getChildren()) {
                register(child, subRegistration);
            }
            return subRegistration;
        }

    }
}
