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
 */package org.jboss.as.controller.descriptions.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROBLEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URI;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALID;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.common.ValidateAddressOperationHandler;
import org.jboss.as.controller.operations.common.ValidateOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Some bits and pieces of model attribute description that are used across models.
 *
 * @author Brian Stansberry
 */
@Deprecated
public class CommonDescriptions {

    public static ModelNode getValidateAddressOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode descr = getSingleParamSimpleReplyOperation(bundle, ValidateAddressOperationHandler.OPERATION_NAME, "global", VALUE, ModelType.OBJECT, false, ModelType.OBJECT, true);
        final ModelNode valueType = descr.get(REPLY_PROPERTIES, VALUE_TYPE);
        valueType.get(VALID, DESCRIPTION).set(bundle.getString("global." + ValidateAddressOperationHandler.OPERATION_NAME + ".reply." + VALID));
        valueType.get(VALID, TYPE).set(ModelType.BOOLEAN);
        valueType.get(PROBLEM, DESCRIPTION).set(bundle.getString("global." + ValidateAddressOperationHandler.OPERATION_NAME + ".reply." + PROBLEM));
        valueType.get(PROBLEM, TYPE).set(ModelType.STRING);
        return descr;
    }

    public static ModelNode getCompositeOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode descr = getSingleParamSimpleReplyOperation(bundle, COMPOSITE, "root", STEPS, ModelType.LIST, false, ModelType.OBJECT, true);
        final ModelNode steps = descr.get(REQUEST_PROPERTIES, STEPS);
        steps.get(VALUE_TYPE).set(ModelType.OBJECT);
        // TODO could probably describe better the response structure
        // and make it different for domain and standalone mode
        return descr;
    }

    /**
     * default describe operation description,
     * here just for  binary compatibility with external extensions
     * @param locale
     * @return
     */
    @Deprecated
    public static ModelNode getSubsystemDescribeOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(ADD);
        root.get(DESCRIPTION).set(bundle.getString("subsystem.describe"));
        root.get(REPLY_PROPERTIES, TYPE).set(ModelType.LIST);
        root.get(REPLY_PROPERTIES, VALUE_TYPE).set(ModelType.OBJECT);
        return root;
    }


    public static ModelNode getDescriptionOnlyOperation(final ResourceBundle bundle, final String operationName, final String descriptionPrefix) {

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(operationName);
        String descriptionKey = descriptionPrefix == null ? operationName : descriptionPrefix + "." + operationName;
        node.get(DESCRIPTION).set(bundle.getString(descriptionKey));

        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    public static ModelNode getSingleParamOnlyOperation(final ResourceBundle bundle, final String operationName,
                                                         final String descriptionPrefix, final String paramName,
                                                        final ModelType paramType, final boolean nillable) {

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(operationName);
        String descriptionKey = descriptionPrefix == null ? operationName : descriptionPrefix + "." + operationName;
        node.get(DESCRIPTION).set(bundle.getString(descriptionKey));

        final ModelNode param = node.get(REQUEST_PROPERTIES, paramName);
        param.get(DESCRIPTION).set(bundle.getString(descriptionKey + "." + paramName));
        param.get(TYPE).set(paramType);
        param.get(REQUIRED).set(!nillable);
        param.get(NILLABLE).set(nillable);

        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    public static ModelNode getNoArgSimpleReplyOperation(final ResourceBundle bundle, final String operationName,
                                                         final String descriptionPrefix, final ModelType replyType,
                                                         final boolean describeReply) {
        final ModelNode result = getDescriptionOnlyOperation(bundle, operationName, descriptionPrefix);
        if (describeReply) {
            String replyKey = descriptionPrefix == null ? operationName + ".reply" : descriptionPrefix + "." + operationName + ".reply";
            result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString(replyKey));
        }
        result.get(REPLY_PROPERTIES, TYPE).set(replyType);

        return result;
    }

    public static ModelNode getSingleParamSimpleReplyOperation(final ResourceBundle bundle, final String operationName,
                                                         final String descriptionPrefix, final String paramName,
                                                         final ModelType paramType, final boolean paramNillable,
                                                         final ModelType replyType, final boolean describeReply) {
        final ModelNode result = getSingleParamOnlyOperation(bundle, operationName, descriptionPrefix, paramName, paramType, paramNillable);
        if (describeReply) {
            String replyKey = descriptionPrefix == null ? operationName + ".reply" : descriptionPrefix + "." + operationName + ".reply";
            result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString(replyKey));
        }
        result.get(REPLY_PROPERTIES, TYPE).set(replyType);

        return result;
    }

    public static ModelNode getNoArgSimpleListReplyOperation(final ResourceBundle bundle, final String operationName,
                                                         final String descriptionPrefix, final ModelType listValueType,
                                                         final boolean describeReply) {
        ModelNode result = getNoArgSimpleReplyOperation(bundle, operationName, descriptionPrefix, ModelType.LIST, describeReply);
        result.get(REPLY_PROPERTIES, VALUE_TYPE).set(listValueType);
        return result;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(ControllerResolver.RESOURCE_NAME, locale);
    }
}
