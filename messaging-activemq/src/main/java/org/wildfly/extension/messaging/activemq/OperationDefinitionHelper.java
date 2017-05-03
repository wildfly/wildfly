/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
        ModelNode filter = FILTER.resolveModelAttribute(context, operation);
        return filter.isDefined() ? filter.asString() : null;
    }
}
