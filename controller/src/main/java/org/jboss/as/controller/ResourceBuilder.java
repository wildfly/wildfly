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

    ResourceBuilder addChild(final PathElement pathElement);

    ResourceBuilder addChild(final PathElement pathElement, StandardResourceDescriptionResolver resolver);

    ResourceDefinition build();

    ResourceBuilder addChild(PathElement pathElement, OperationStepHandler addHandler, OperationStepHandler removeHandler);

    ResourceBuilder addChild(PathElement pathElement, StandardResourceDescriptionResolver resolver, OperationStepHandler addHandler, OperationStepHandler removeHandler);

    ResourceBuilder end();

    ResourceBuilder addChild(ResourceBuilder child);

    ResourceBuilder addReadWriteAttributes(OperationStepHandler reader, OperationStepHandler writer, AttributeDefinition... attributes);

    ResourceBuilder addMetric(AttributeDefinition attributeDefinition, OperationStepHandler handler);

    ResourceBuilder addMetrics(OperationStepHandler metricHandler, AttributeDefinition... attributes);

    ResourceBuilder addOperation(OperationDefinition operationDefinition, OperationStepHandler handler, boolean inherited);

    class Factory {
        public static ResourceBuilder create(PathElement pathElement, StandardResourceDescriptionResolver resourceDescriptionResolver) {
            return ResourceBuilderRoot.create(pathElement, resourceDescriptionResolver);
        }

        public static ResourceBuilder createSubsystemRoot(PathElement pathElement,
                                                          StandardResourceDescriptionResolver resolver,
                                                          OperationStepHandler addHandler,
                                                          OperationStepHandler removeHandler) {
            ResourceBuilder builder = ResourceBuilderRoot.create(pathElement, resolver, addHandler, removeHandler);
            builder.addOperation(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
            return builder;
        }
    }
}
