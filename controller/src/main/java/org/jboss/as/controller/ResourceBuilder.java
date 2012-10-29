/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2012, Red Hat Middleware LLC, and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.controller;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public interface ResourceBuilder {
    ResourceBuilder setAddOperation(AbstractAddStepHandler handler);

    ResourceBuilder setRemoveOperation(AbstractRemoveStepHandler handler);

    ResourceBuilder addReadWriteAttribute(AttributeDefinition attributeDefinition, OperationStepHandler reader, OperationStepHandler writer);

    ResourceBuilder addReadOnlyAttribute(AttributeDefinition attributeDefinition);

    ResourceBuilder setAttributeResolver(ResourceDescriptionResolver resolver);

    ResourceBuilder addOperation(OperationDefinition operationDefinition, OperationStepHandler handler);

    ResourceBuilder pushChild(final PathElement pathElement);

    ResourceBuilder pushChild(final PathElement pathElement, StandardResourceDescriptionResolver resolver);

    ResourceBuilder pushChild(PathElement pathElement, OperationStepHandler addHandler, OperationStepHandler removeHandler);

    ResourceBuilder pushChild(PathElement pathElement, StandardResourceDescriptionResolver resolver, OperationStepHandler addHandler, OperationStepHandler removeHandler);

    ResourceBuilder pushChild(ResourceBuilder child);

    ResourceBuilder pop();

    ResourceBuilder addReadWriteAttributes(OperationStepHandler reader, OperationStepHandler writer, AttributeDefinition... attributes);

    ResourceBuilder addMetric(AttributeDefinition attributeDefinition, OperationStepHandler handler);

    ResourceBuilder addMetrics(OperationStepHandler metricHandler, AttributeDefinition... attributes);

    ResourceBuilder addOperation(OperationDefinition operationDefinition, OperationStepHandler handler, boolean inherited);

    ResourceDefinition build();

    class Factory {
        public static ResourceBuilder create(PathElement pathElement, StandardResourceDescriptionResolver resourceDescriptionResolver) {
            return ResourceBuilderRoot.create(pathElement, resourceDescriptionResolver);
        }

        public static ResourceBuilder createSubsystemRoot(PathElement pathElement,
                                                          StandardResourceDescriptionResolver resolver,
                                                          OperationStepHandler addHandler,
                                                          OperationStepHandler removeHandler) {
            return createSubsystemRoot(pathElement, resolver, addHandler, removeHandler, GenericSubsystemDescribeHandler.INSTANCE);
        }

        public static ResourceBuilder createSubsystemRoot(PathElement pathElement,
                                                          StandardResourceDescriptionResolver resolver,
                                                          OperationStepHandler addHandler,
                                                          OperationStepHandler removeHandler,
                                                          OperationStepHandler describeHandler) {
            ResourceBuilder builder = ResourceBuilderRoot.create(pathElement, resolver, addHandler, removeHandler);
            builder.addOperation(GenericSubsystemDescribeHandler.DEFINITION, describeHandler); //operation description is always the same
            return builder;
        }
    }
}
