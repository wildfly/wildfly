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
package org.jboss.as.controller.descriptions.common;

import java.util.Locale;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 * {@link org.jboss.as.controller.descriptions.DescriptionProvider} implementations for sub-models that occur across different
 * types of models.
 *
 * @author Brian Stansberry
 *
 */
@Deprecated
public final class CommonProviders {

    // Prevent instantiation
    private CommonProviders() {
    }


    public static final DescriptionProvider READ_RESOURCE_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return GlobalDescriptions.getReadResourceOperationDescription(locale);
        }
    };

    public static final DescriptionProvider READ_ATTRIBUTE_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return GlobalDescriptions.getReadAttributeOperationDescription(locale);
        }
    };

    public static final DescriptionProvider UNDEFINE_ATTRIBUTE_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return GlobalDescriptions.getUndefineAttributeOperationDescription(locale);
        }
    };

    public static final DescriptionProvider WRITE_ATTRIBUTE_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return GlobalDescriptions.getWriteAttributeOperationDescription(locale);
        }
    };

    public static final DescriptionProvider READ_CHILDREN_NAMES_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return GlobalDescriptions.getReadChildrenNamesOperationDescription(locale);
        }
    };

    public static final DescriptionProvider READ_CHILDREN_TYPES_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return GlobalDescriptions.getReadChildrenTypesOperationDescription(locale);
        }
    };

    public static final DescriptionProvider READ_CHILDREN_RESOURCES_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return GlobalDescriptions.getReadChildrenResourcesOperationDescription(locale);
        }
    };

    public static final DescriptionProvider READ_OPERATION_NAMES_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return GlobalDescriptions.getReadOperationNamesOperation(locale);
        }
    };

    public static final DescriptionProvider READ_OPERATION_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return GlobalDescriptions.getReadOperationOperation(locale);
        }
    };

    public static final DescriptionProvider READ_RESOURCE_DESCRIPTION_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return GlobalDescriptions.getReadResourceDescriptionOperationDescription(locale);
        }
    };

    /**
     * Provider for a sub-resource that exposes the MSC ServiceContainer.
     */
    public static final DescriptionProvider SERVICE_CONTAINER_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return CommonDescriptions.getServiceContainerDescription(locale);
        }
    };
}
