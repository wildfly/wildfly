/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.FILTER;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Helper class to define operations.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class OperationDefinitionHelper {
    public static AttributeDefinition createNonEmptyStringAttribute(String attributeName) {
        return SimpleAttributeDefinitionBuilder.create(attributeName, ModelType.STRING)
                .setRequired(true)
                .setValidator(new StringLengthValidator(1))
                .build();
    }

    /**
     * Create a {@code SimpleOperationDefinitionBuilder} for the given operationName and flag it as
     * <strong>read only and runtime only</strong>.
     */
    public static SimpleOperationDefinitionBuilder runtimeReadOnlyOperation(String operationName, ResourceDescriptionResolver resolver) {
        return new SimpleOperationDefinitionBuilder(operationName, resolver)
                .setRuntimeOnly()
                .setReadOnly();
    }

    /**
     * Create a {@code SimpleOperationDefinitionBuilder} for the given operationName and flag it as
     * <strong>runtime only</strong>.
    */
    public static SimpleOperationDefinitionBuilder runtimeOnlyOperation(String operationName, ResourceDescriptionResolver resolver) {
        return new SimpleOperationDefinitionBuilder(operationName, resolver)
                .setRuntimeOnly();
    }

    public static String resolveFilter(OperationContext context, ModelNode operation) throws OperationFailedException {
        return FILTER.resolveModelAttribute(context, operation).asStringOrNull();
    }
}
