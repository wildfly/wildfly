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

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALTERNATIVES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STORAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.resource.InterfaceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model descriptions for interface elements.
 *
 * @author Brian Stansberry
 * @author Emanuel Muckenhuber
 */
@Deprecated
public class InterfaceDescription {

    private static final String RESOURCE_NAME = InterfaceDescription.class.getPackage().getName() + ".LocalDescriptions";

    public static ModelNode getNamedInterfaceDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("named_interface"));
        InterfaceDefinition.NAME.addResourceAttributeDescription(bundle, "interface", root);
        populateInterface(root, bundle, false);
        return root;
    }

    public static ModelNode getSpecifiedInterfaceDescription(final Locale locale, boolean server) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("specified_interface"));
        InterfaceDefinition.NAME.addResourceAttributeDescription(bundle, "interface", root);

        if (server) {
            //Quick and dirty hack to get this passing the tests, the whole thing shouldbe converted to resource definition
            //NetworkInterfaceRuntimeHandler.RESOLVED_ADDRESS lives in the server module and so is invisible from here
            root.get(ATTRIBUTES, "resolved-address", DESCRIPTION).set(bundle.getString("interface.resolved-address"));
            root.get(ATTRIBUTES, "resolved-address", STORAGE).set(Storage.RUNTIME.toString());
            root.get(ATTRIBUTES, "resolved-address", TYPE).set(ModelType.STRING);
        }

        populateInterface(root, bundle, true);
        return root;
    }

    private static void populateInterface(ModelNode root, ResourceBundle bundle, boolean specified) {
        root.get(HEAD_COMMENT_ALLOWED).set(true);
        root.get(TAIL_COMMENT_ALLOWED).set(false);
        // Add the interface criteria operation params
        for(final AttributeDefinition def : InterfaceDefinition.ROOT_ATTRIBUTES) {
            def.addResourceAttributeDescription(bundle, "interface", root);
        }
    }

    public static ModelNode getNamedInterfaceAddOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(ADD);
        root.get(DESCRIPTION).set(bundle.getString("interface.add"));
        // Add the interface criteria attributes
        for(final AttributeDefinition def : InterfaceDefinition.ROOT_ATTRIBUTES) {
            def.addOperationParameterDescription(bundle, "interface", root);
        }
        return root;
    }

    public static ModelNode getSpecifiedInterfaceAddOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(ADD);
        root.get(DESCRIPTION).set(bundle.getString("interface.add"));
        // Add the interface criteria attributes
        for(final AttributeDefinition def : InterfaceDefinition.ROOT_ATTRIBUTES) {
            def.addOperationParameterDescription(bundle, "interface", root);
        }
        return root;
    }

    public static ModelNode getInterfaceRemoveOperation(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode root = new ModelNode();
        root.get(OPERATION_NAME).set(REMOVE);
        root.get(DESCRIPTION).set(bundle.getString("interface.remove"));
        root.get(REQUEST_PROPERTIES).setEmptyObject();
        root.get(REPLY_PROPERTIES).setEmptyObject();
        return root;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    /**
     * Test whether the operation has a defined criteria attribute.
     *
     * @param operation the operation
     * @return
     */
    public static boolean isOperationDefined(final ModelNode operation) {
        for(final AttributeDefinition def : InterfaceDefinition.ROOT_ATTRIBUTES) {
            if(operation.hasDefined(def.getName())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Create the AttributeDefinition for the nested 'not' / 'any' type.
     *
     * @param name the name
     * @return the attribute definition
     */
    public static AttributeDefinition createNestedComplexType(final String name) {
        return new AttributeDefinition(name, name, null, ModelType.OBJECT, true, false, MeasurementUnit.NONE, createNestedParamValidator(), InterfaceDefinition.ALTERNATIVES_ANY, null, AttributeAccess.Flag.RESTART_ALL_SERVICES) {
            @Override
            public void marshallAsElement(ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                throw new UnsupportedOperationException();
            }

            @Override
            public ModelNode addResourceAttributeDescription(final ResourceBundle bundle, final String prefix, final ModelNode resourceDescription) {
                final ModelNode result = super.addResourceAttributeDescription(bundle, prefix, resourceDescription);
                addNestedDescriptions(result, prefix, bundle);
                return result;
            }

            @Override
            public ModelNode addOperationParameterDescription(ResourceBundle bundle, String prefix, ModelNode operationDescription) {
                final ModelNode result = super.addOperationParameterDescription(bundle, prefix, operationDescription);    //To change body of overridden methods use File | Settings | File Templates.
                addNestedDescriptions(result, prefix, bundle);
                return result;
            }

            void addNestedDescriptions(final ModelNode result, final String prefix, final ResourceBundle bundle) {
                for(final AttributeDefinition def : InterfaceDefinition.NESTED_ATTRIBUTES) {
                    final String bundleKey = prefix == null ? def.getName() : (prefix + "." + def.getName());
                    result.get(VALUE_TYPE, def.getName(), DESCRIPTION).set(bundle.getString(bundleKey));
                }
            }

            @Override
            public ModelNode getNoTextDescription(boolean forOperation) {
                final ModelNode model = super.getNoTextDescription(forOperation);
                final ModelNode valueType = model.get(VALUE_TYPE);
                for(final AttributeDefinition def : InterfaceDefinition.NESTED_ATTRIBUTES) {
                    final AttributeDefinition current;
                    if(InterfaceDefinition.NESTED_LIST_ATTRIBUTES.contains(def)) {
                        current = wrapAsList(def);
                    } else {
                        current = def;
                    }
                    final ModelNode m = current.getNoTextDescription(forOperation);
                    m.remove(ALTERNATIVES);
                    valueType.get(current.getName()).set(m);
                }
                return model;
            }
        };

    }

    /**
     * Wrap a simple attribute def as list.
     *
     * @param def the attribute definition
     * @return the list attribute def
     */
    private static ListAttributeDefinition wrapAsList(final AttributeDefinition def) {
        final ListAttributeDefinition list = new ListAttributeDefinition(def.getName(), true, def.getValidator()) {

            @Override
            public ModelNode getNoTextDescription(boolean forOperation) {
                final ModelNode model = super.getNoTextDescription(forOperation);
                setValueType(model);
                return model;
            }

            @Override
            protected void addValueTypeDescription(final ModelNode node, final ResourceBundle bundle) {
                setValueType(node);
            }
            @Override
            public void marshallAsElement(final ModelNode resourceModel, final boolean marshalDefault, final XMLStreamWriter writer) throws XMLStreamException {
                throw new RuntimeException();
            }

            @Override
            protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
                setValueType(node);
            }

            @Override
            protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
                setValueType(node);
            }
            private void setValueType(ModelNode node) {
                node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
            }
        };
        return list;
    }

    /**
     * Create the nested complex attribute parameter validator.
     *
     * @return the parameter validator
     */
    static ParameterValidator createNestedParamValidator() {
        return new ModelTypeValidator(ModelType.OBJECT, true, false, true) {
            @Override
            public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {
                super.validateParameter(parameterName, value);

                for(final AttributeDefinition def : InterfaceDefinition.NESTED_ATTRIBUTES) {
                    final String name = def.getName();
                    if(value.hasDefined(name)) {
                        final ModelNode v = value.get(name);
                        if(InterfaceDefinition.NESTED_LIST_ATTRIBUTES.contains(def)) {
                            if (ModelType.LIST != v.getType()) {
                                throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidType(v.getType())));
                            }
                        } else {
                            def.getValidator().validateParameter(name, v);
                        }
                    }
                }
            }
        };
    }

}
