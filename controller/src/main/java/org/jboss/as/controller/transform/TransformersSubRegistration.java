/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ModelVersionRange;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import org.jboss.as.controller.registry.GlobalTransformerRegistry;

/**
 * Registration for subsystem specific operation transformers.
 *
 * @author Emanuel Muckenhuber
 */
public interface TransformersSubRegistration {

    String[] COMMON_OPERATIONS = { ADD, REMOVE };

    /**
     * Register a sub resource.
     *
     * @param element the path element
     * @return the sub registration
     */
    TransformersSubRegistration registerSubResource(PathElement element);

    /**
     * Register a sub resource.
     *
     * @param element the path element
     * @param discardByDefault don't forward operations by default
     * @return the sub registration
     */
    TransformersSubRegistration registerSubResource(PathElement element, boolean discardByDefault);

    /**
     * Register a sub resource.
     *
     * @param element the path element
     * @param operationTransformer the default operation transformer
     * @return the sub registration
     */
    TransformersSubRegistration registerSubResource(PathElement element, OperationTransformer operationTransformer);

    /**
     * Don't forward and just discard the operation.
     *
     * @param operationNames the operation names
     */
    void discardOperations(String... operationNames);

    /**
     * Register an operation transformer.
     *
     * @param operationName the operation name
     * @param transformer the operation transformer
     */
    void registerOperationTransformer(String operationName, OperationTransformer transformer);

    public class TransformersSubRegistrationImpl implements TransformersSubRegistration {

        private final PathAddress current;
        private final ModelVersionRange range;
        private final GlobalTransformerRegistry registry;

        public TransformersSubRegistrationImpl(ModelVersionRange range, GlobalTransformerRegistry registry, PathAddress parent) {
            this.range = range;
            this.registry = registry;
            this.current = parent;
        }

        @Override
        public TransformersSubRegistration registerSubResource(PathElement element) {
            return registerSubResource(element, false);
        }

        @Override
        public TransformersSubRegistration registerSubResource(PathElement element, boolean discard) {
            return registerSubResource(element, null);
        }

        @Override
        public TransformersSubRegistration registerSubResource(PathElement element, OperationTransformer operationTransformer) {
            final PathAddress address = current.append(element);
            for(final ModelVersion version : range.getVersions()) {
                registry.createChildRegistry(address, version, operationTransformer);
            }
            return new TransformersSubRegistrationImpl(range, registry, address);
        }

        @Override
        public void discardOperations(String... operationNames) {
            for(final ModelVersion version : range.getVersions()) {
                for(final String operationName : operationNames) {
                    registry.discardOperation(current, version, operationName);
                }
            }
        }

        @Override
        public void registerOperationTransformer(String operationName, OperationTransformer transformer) {
            for(final ModelVersion version : range.getVersions()) {
                registry.registerTransformer(current, version, operationName, transformer);
            }
        }
    }

}
