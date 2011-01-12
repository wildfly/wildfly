/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCALE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_SUB_MODEL_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class GlobalDescriptions {
    private static final String RESOURCE_NAME = PathDescription.class.getPackage().getName() + ".LocalDescriptions";

    public static DescriptionProvider getReadSubModelOperationDescription() {
        return new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                ResourceBundle bundle = getResourceBundle(locale);

                ModelNode node = new ModelNode();
                node.get(OPERATION_NAME).set(READ_SUB_MODEL_OPERATION);
                node.get(DESCRIPTION).set(bundle.getString("global.read-sub-model"));
                node.get(REQUEST_PROPERTIES, RECURSIVE, TYPE).set(ModelType.BOOLEAN);
                node.get(REQUEST_PROPERTIES, RECURSIVE, DESCRIPTION).set(bundle.getString("global.read-sub-model.recursive"));
                node.get(REQUEST_PROPERTIES, RECURSIVE, NILLABLE).set(true);
                node.get(REPLY_PROPERTIES, TYPE).set(ModelType.OBJECT);
                //TODO value type
                node.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("global.read-sub-model.reply"));
                node.protect();

                return node;
            }
        };
    }

    public static DescriptionProvider getReadAttributeOperationDescription() {
        return new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                ResourceBundle bundle = getResourceBundle(locale);

                ModelNode node = new ModelNode();
                node.get(OPERATION_NAME).set(READ_ATTRIBUTE_OPERATION);
                node.get(DESCRIPTION).set(bundle.getString("global.read-attribute"));

                node.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
                node.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("global.read-attribute.type"));
                node.get(REQUEST_PROPERTIES, NAME, NILLABLE).set(false);
                node.get(REPLY_PROPERTIES, TYPE).set(ModelType.OBJECT);
                node.get(REPLY_PROPERTIES, VALUE_TYPE).set(bundle.getString("global.read-attribute.reply.type"));
                node.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("global.read-attribute.reply"));
                node.protect();

                return node;
            }
        };
    }

    public static DescriptionProvider getReadChildrenNamesOperationDescription() {
        return new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                ResourceBundle bundle = getResourceBundle(locale);

                ModelNode node = new ModelNode();
                node.get(OPERATION_NAME).set(READ_CHILDREN_NAMES_OPERATION);
                node.get(DESCRIPTION).set(bundle.getString("global.read-children-names"));

                node.get(REQUEST_PROPERTIES, CHILD_TYPE, TYPE).set(ModelType.STRING);
                node.get(REQUEST_PROPERTIES, CHILD_TYPE, DESCRIPTION).set(bundle.getString("global.read-children-names.child-type"));
                node.get(REQUEST_PROPERTIES, CHILD_TYPE, NILLABLE).set(false);
                node.get(REPLY_PROPERTIES, TYPE).set(ModelType.LIST);
                node.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("global.read-children-names.reply"));
                node.get(REPLY_PROPERTIES, VALUE_TYPE).set(ModelType.STRING);

                node.protect();
                return node;
            }
        };
    }

    public static DescriptionProvider getReadOperationNamesOperation() {
        return new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                ResourceBundle bundle = getResourceBundle(locale);

                ModelNode node = new ModelNode();
                node.get(OPERATION_NAME).set(READ_OPERATION_NAMES_OPERATION);
                node.get(DESCRIPTION).set(bundle.getString("global.read-operation-names"));

                node.get(REPLY_PROPERTIES, TYPE).set(ModelType.LIST);
                node.get(REPLY_PROPERTIES, VALUE_TYPE).set(ModelType.STRING);
                node.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("global.read-operation-names.reply"));

                node.protect();
                return node;
            }
        };
    }

    public static DescriptionProvider getReadOperationOperation() {
        return new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                ResourceBundle bundle = getResourceBundle(locale);

                ModelNode node = new ModelNode();
                node.get(OPERATION_NAME).set(READ_OPERATION_DESCRIPTION_OPERATION);
                node.get(DESCRIPTION).set(bundle.getString("global.read-operation"));
                node.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
                node.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("global.read-operation.type"));
                node.get(REQUEST_PROPERTIES, TYPE, NILLABLE).set(false);
                node.get(REQUEST_PROPERTIES, LOCALE, TYPE).set(ModelType.STRING);
                node.get(REQUEST_PROPERTIES, LOCALE, NILLABLE).set(true);
                node.get(REQUEST_PROPERTIES, LOCALE, DESCRIPTION).set(bundle.getString("global.read-operation.locale"));

                node.get(REPLY_PROPERTIES, TYPE).set(ModelType.OBJECT);
                //TODO value type?
                node.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("global.read-operation.type"));

                node.protect();
                return node;
            }
        };
    }

    public static DescriptionProvider getReadResourceDescriptionOperationDescription() {
        return new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                ResourceBundle bundle = getResourceBundle(locale);

                ModelNode node = new ModelNode();
                node.get(OPERATION_NAME).set(READ_RESOURCE_DESCRIPTION_OPERATION);
                node.get(DESCRIPTION).set(bundle.getString("global.read-resource-description"));
                node.get(REQUEST_PROPERTIES, OPERATIONS, TYPE).set(ModelType.BOOLEAN);
                node.get(REQUEST_PROPERTIES, OPERATIONS, DESCRIPTION).set(bundle.getString("global.read-resource-description.operations"));
                node.get(REQUEST_PROPERTIES, OPERATIONS, NILLABLE).set(true);
                node.get(REQUEST_PROPERTIES, LOCALE, TYPE).set(ModelType.STRING);
                node.get(REQUEST_PROPERTIES, LOCALE, NILLABLE).set(true);
                node.get(REQUEST_PROPERTIES, LOCALE, DESCRIPTION).set(bundle.getString("global.read-resource-description.locale"));

                node.get(REPLY_PROPERTIES, TYPE).set(ModelType.OBJECT);
                node.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("global.read-resource-description.reply"));
                node.protect();

                return node;
            }
        };
    }


    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
