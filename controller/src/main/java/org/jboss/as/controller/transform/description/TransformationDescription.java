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

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ModelVersionRange;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathTransformation;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;

import java.util.List;
import java.util.Map;

/**
 * The final tranformation description.
 *
 * @author Emanuel Muckenhuber
 */
public interface TransformationDescription {

    /**
     * Get the path for this transformtion description.
     *
     * @return the path element
     */
    PathElement getPath();

    /**
     * Get the path transformation for this level.
     *
     * @return the path transformation
     */
    PathTransformation getPathTransformation();

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

    @Deprecated
    void register(SubsystemRegistration subsytem, ModelVersion... versions);
    @Deprecated
    void register(SubsystemRegistration subsytem, ModelVersionRange range);
    @Deprecated
    void register(TransformersSubRegistration parent);

    public static final class Tools {

        private Tools() {
            //
        }

        public static void register(final TransformationDescription description, TransformersSubRegistration parent) {
            final TransformersSubRegistration registration = parent.registerSubResource(description.getPath(), description.getPathTransformation(), description.getResourceTransformer(), description.getOperationTransformer());
            for (final Map.Entry<String, OperationTransformer> entry : description.getOperationTransformers().entrySet()) {
                registration.registerOperationTransformer(entry.getKey(), entry.getValue());
            }
            for (final TransformationDescription child : description.getChildren()) {
                register(child, registration);
            }
        }

        public static void register(TransformationDescription description, SubsystemRegistration registration, ModelVersion... versions) {
            register(description, registration, ModelVersionRange.Versions.range(versions));
        }

        public static void register(TransformationDescription description, SubsystemRegistration registration, ModelVersionRange range) {
            throw new IllegalStateException("implement operation transformer registration for subsystems");
        }

    }
}
