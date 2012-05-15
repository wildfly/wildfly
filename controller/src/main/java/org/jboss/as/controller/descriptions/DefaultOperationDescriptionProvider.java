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

package org.jboss.as.controller.descriptions;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Provides a default description of an operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DefaultOperationDescriptionProvider implements DescriptionProvider {

    private final String operationName;
    private final ResourceDescriptionResolver descriptionResolver;
    private final ModelType replyType;
    private final ModelType replyValueType;
    private final AttributeDefinition[] parameters;

    public DefaultOperationDescriptionProvider(final String operationName,
                                               final ResourceDescriptionResolver descriptionResolver,
                                               final AttributeDefinition... parameters) {
        this(operationName, descriptionResolver, null, null, parameters);
    }

    public DefaultOperationDescriptionProvider(final String operationName,
                                               final ResourceDescriptionResolver descriptionResolver,
                                               final ModelType replyType,
                                               final AttributeDefinition... parameters) {
        this(operationName, descriptionResolver, replyType, null, parameters);
    }

    public DefaultOperationDescriptionProvider(final String operationName,
                                               final ResourceDescriptionResolver descriptionResolver,
                                               final ModelType replyType,
                                               final ModelType replyValueType,
                                               final AttributeDefinition... parameters) {
        this.operationName = operationName;
        this.descriptionResolver = descriptionResolver;
        this.replyType = replyType;
        this.replyValueType = replyValueType;
        this.parameters = parameters;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        ModelNode result = new ModelNode();

        final ResourceBundle bundle = descriptionResolver.getResourceBundle(locale);
        result.get(OPERATION_NAME).set(operationName);
        result.get(DESCRIPTION).set(descriptionResolver.getOperationDescription(operationName, locale, bundle));

        result.get(REQUEST_PROPERTIES).setEmptyObject();

        if (parameters != null) {
            for (AttributeDefinition definition : parameters) {
                definition.addOperationParameterDescription(result, operationName, descriptionResolver, locale, bundle);
            }
        }

        final ModelNode reply = result.get(REPLY_PROPERTIES).setEmptyObject();

        final String replyDesc = descriptionResolver.getOperationReplyDescription(operationName, locale, bundle);
        if (replyDesc != null) {
            reply.get(DESCRIPTION).set(replyDesc);
        }
        if (replyType != null && replyType != ModelType.UNDEFINED) {
            reply.get(TYPE).set(replyType);
            if (replyType == ModelType.LIST || replyType == ModelType.OBJECT) {
                if (replyValueType != null && replyValueType != ModelType.OBJECT) {
                    reply.get(VALUE_TYPE).set(replyValueType);
                } else {
                    reply.get(VALUE_TYPE).set(getReplyValueTypeDescription(descriptionResolver, locale, bundle));
                }
            }
        }

        return result;
    }

    /**
     * Hook for subclasses to provide a description object for any complex "value-type" description of the operation reply.
     * <p>This default implementation throws an {@code IllegalStateException}; it is the responsibility of
     * subclasses to override this method if a complex "value-type" description is required.</p>
     *
     * @param descriptionResolver  resolver for localizing any text in the description
     * @param locale locale for any text description
     * @param bundle resource bundle previously {@link ResourceDescriptionResolver#getResourceBundle(Locale) obtained from the description resolver}
     *
     * @return a node describing the reply's "value-type"
     *
     * @throws IllegalStateException if not overridden by an implementation that does not
     */
    protected ModelNode getReplyValueTypeDescription(ResourceDescriptionResolver descriptionResolver, Locale locale, ResourceBundle bundle) {
        // bug -- user specifies a complex reply type but does not override this method to describe it
        throw MESSAGES.operationReplyValueTypeRequired(operationName);
    }
}
