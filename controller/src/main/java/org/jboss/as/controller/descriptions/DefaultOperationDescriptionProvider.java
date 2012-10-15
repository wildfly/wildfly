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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.DeprecationData;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Provides a default description of an operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author Tomaz Cerar (c) 2012 Red Hat Inc.
 */
public class DefaultOperationDescriptionProvider implements DescriptionProvider {

    private final String operationName;
    private final ResourceDescriptionResolver descriptionResolver;
    private final ResourceDescriptionResolver attributeDescriptionResolver;
    private final ModelType replyType;
    private final ModelType replyValueType;
    private DeprecationData deprecationData;
    private final AttributeDefinition[] replyParameters;
    private final AttributeDefinition[] parameters;

    public DefaultOperationDescriptionProvider(final String operationName,
                                               final ResourceDescriptionResolver descriptionResolver,
                                               final AttributeDefinition... parameters) {
        this(operationName, descriptionResolver, null, null, parameters);
    }

    public DefaultOperationDescriptionProvider(final String operationName,
                                               final ResourceDescriptionResolver descriptionResolver,
                                               final DeprecationData deprecationData,
                                               final AttributeDefinition... parameters) {
        this(operationName, descriptionResolver, null, null, parameters);
        this.deprecationData = deprecationData;
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
        this(operationName, descriptionResolver, replyType, replyValueType, null, parameters);
    }

    public DefaultOperationDescriptionProvider(final String operationName,
                                               final ResourceDescriptionResolver descriptionResolver,
                                               final ModelType replyType,
                                               final ModelType replyValueType,
                                               final DeprecationData deprecationData,
                                               final AttributeDefinition... parameters) {
        this(operationName, descriptionResolver, descriptionResolver, replyType, replyValueType, deprecationData, null, parameters);
    }

    public DefaultOperationDescriptionProvider(final String operationName,
                                               final ResourceDescriptionResolver descriptionResolver,
                                               final ResourceDescriptionResolver attributeDescriptionResolver,
                                               final ModelType replyType,
                                               final ModelType replyValueType,
                                               final DeprecationData deprecationData,
                                               final AttributeDefinition[] replyParameters,
                                               final AttributeDefinition... parameters) {
        this.operationName = operationName;
        this.descriptionResolver = descriptionResolver;
        this.attributeDescriptionResolver = attributeDescriptionResolver;
        this.replyType = replyType;
        this.replyValueType = replyValueType;
        this.parameters = parameters;
        this.deprecationData = deprecationData;
        this.replyParameters = replyParameters;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        ModelNode result = new ModelNode();

        final ResourceBundle bundle = descriptionResolver.getResourceBundle(locale);
        final ResourceBundle attributeBundle = attributeDescriptionResolver.getResourceBundle(locale);
        result.get(OPERATION_NAME).set(operationName);
        result.get(DESCRIPTION).set(descriptionResolver.getOperationDescription(operationName, locale, bundle));

        result.get(REQUEST_PROPERTIES).setEmptyObject();

        if (parameters != null) {
            for (AttributeDefinition definition : parameters) {
                definition.addOperationParameterDescription(result, operationName, attributeDescriptionResolver, locale, attributeBundle);
            }
        }

        final ModelNode reply = result.get(REPLY_PROPERTIES).setEmptyObject();

        if (replyType != null && replyType != ModelType.UNDEFINED) {
            reply.get(TYPE).set(replyType);
            if (replyType == ModelType.LIST || replyType == ModelType.OBJECT) {
                if (replyValueType != null && replyValueType != ModelType.OBJECT) {
                    reply.get(VALUE_TYPE).set(replyValueType);
                } else if (replyValueType != null) {
                    reply.get(VALUE_TYPE).set(getReplyValueTypeDescription(descriptionResolver, locale, bundle));
                }
            }
        }
        if (replyParameters != null && replyParameters.length > 0) {
            if (replyParameters.length == 1) {
                AttributeDefinition ad = replyParameters[0];
                ModelNode param = ad.getNoTextDescription(true);
                final String description = attributeDescriptionResolver.getOperationParameterDescription(operationName, ad.getName(), locale, attributeBundle);
                param.get(ModelDescriptionConstants.DESCRIPTION).set(description);
                reply.set(param);
                ModelNode deprecated = ad.addDeprecatedInfo(result);
                if (deprecated != null) {
                    deprecated.get(ModelDescriptionConstants.REASON).set(attributeDescriptionResolver.getOperationParameterDeprecatedDescription(operationName, ad.getName(), locale, attributeBundle));
                }
            } else {
                reply.get(TYPE).set(ModelType.OBJECT);
                for (AttributeDefinition ad : replyParameters) {
                    final ModelNode param = ad.addOperationParameterDescription(new ModelNode(), operationName, attributeDescriptionResolver, locale, bundle);
                    reply.get(VALUE_TYPE, ad.getName()).set(param);
                }
            }
        }
        if (!reply.asList().isEmpty()) {
            final String replyDesc = descriptionResolver.getOperationReplyDescription(operationName, locale, bundle);
            if (replyDesc != null) {
                reply.get(DESCRIPTION).set(replyDesc);
            }
        }
        if (deprecationData != null) {
            ModelNode deprecated = result.get(ModelDescriptionConstants.DEPRECATED);
            deprecated.get(ModelDescriptionConstants.SINCE).set(deprecationData.getSince().toString());
            deprecated.get(ModelDescriptionConstants.REASON).set(descriptionResolver.getOperationDeprecatedDescription(operationName, locale, bundle));
        }

        return result;
    }

    /**
     * Hook for subclasses to provide a description object for any complex "value-type" description of the operation reply.
     * <p>This default implementation throws an {@code IllegalStateException}; it is the responsibility of
     * subclasses to override this method if a complex "value-type" description is required.</p>
     *
     * @param descriptionResolver resolver for localizing any text in the description
     * @param locale              locale for any text description
     * @param bundle              resource bundle previously {@link ResourceDescriptionResolver#getResourceBundle(Locale) obtained from the description resolver}
     * @return a node describing the reply's "value-type"
     * @throws IllegalStateException if not overridden by an implementation that does not
     */
    protected ModelNode getReplyValueTypeDescription(ResourceDescriptionResolver descriptionResolver, Locale locale, ResourceBundle bundle) {
        // bug -- user specifies a complex reply type but does not override this method to describe it
        return new ModelNode(ModelType.OBJECT); //todo rethink this
        //throw MESSAGES.operationReplyValueTypeRequired(operationName);
    }
}
