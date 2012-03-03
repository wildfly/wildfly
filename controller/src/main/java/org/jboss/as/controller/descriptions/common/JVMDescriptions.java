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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.controller.operations.common.JVMHandlers.*;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 */
public final class JVMDescriptions {

    private static final String RESOURCE_NAME = InterfaceDescription.class.getPackage().getName() + ".LocalDescriptions";

    public static ModelNode getJVMDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();

        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString("jvm"));

        node.get(ATTRIBUTES, JVM_AGENT_LIB, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, JVM_AGENT_LIB, DESCRIPTION).set(bundle.getString("jvm.agent.lib"));
        node.get(ATTRIBUTES, JVM_AGENT_PATH, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, JVM_AGENT_PATH, DESCRIPTION).set(bundle.getString("jvm.agent.path"));
        node.get(ATTRIBUTES, JVM_ENV_CLASSPATH_IGNORED, TYPE).set(ModelType.BOOLEAN);
        node.get(ATTRIBUTES, JVM_ENV_CLASSPATH_IGNORED, DESCRIPTION).set(bundle.getString("jvm.env.classpath.ignored"));
        node.get(ATTRIBUTES, JVM_ENV_VARIABLES, TYPE).set(ModelType.LIST);
        node.get(ATTRIBUTES, JVM_ENV_VARIABLES, VALUE_TYPE).set(ModelType.PROPERTY);
        node.get(ATTRIBUTES, JVM_ENV_VARIABLES, DESCRIPTION).set(bundle.getString("jvm.env.variables"));

        node.get(ATTRIBUTES, JVM_JAVA_AGENT, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, JVM_JAVA_AGENT, DESCRIPTION).set(bundle.getString("jvm.javaagent"));
        node.get(ATTRIBUTES, JVM_JAVA_HOME, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, JVM_JAVA_HOME, DESCRIPTION).set(bundle.getString("jvm.java.home"));
        node.get(ATTRIBUTES, JVM_OPTIONS, TYPE).set(ModelType.LIST);
        node.get(ATTRIBUTES, JVM_OPTIONS, VALUE_TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, JVM_OPTIONS, DESCRIPTION).set(bundle.getString("jvm.options"));
        node.get(ATTRIBUTES, JVM_STACK, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, JVM_STACK, DESCRIPTION).set(bundle.getString("jvm.stack"));
        node.get(ATTRIBUTES, JVM_TYPE, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, JVM_TYPE, DESCRIPTION).set(bundle.getString("jvm.type"));

        node.get(ATTRIBUTES, JVM_HEAP, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, JVM_HEAP, DESCRIPTION).set(bundle.getString("jvm.heap"));
        node.get(ATTRIBUTES, JVM_MAX_HEAP, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, JVM_MAX_HEAP, DESCRIPTION).set(bundle.getString("jvm.heap.max"));

        node.get(ATTRIBUTES, JVM_PERMGEN, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, JVM_PERMGEN, DESCRIPTION).set(bundle.getString("jvm.permgen"));
        node.get(ATTRIBUTES, JVM_MAX_PERMGEN, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, JVM_MAX_PERMGEN, DESCRIPTION).set(bundle.getString("jvm.permgen.max"));

        return node;
    }

    public static ModelNode getServerJVMDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = getJVMDescription(locale);
        node.get(ATTRIBUTES, JVM_DEBUG_ENABLED, TYPE).set(ModelType.BOOLEAN);
        node.get(ATTRIBUTES, JVM_DEBUG_ENABLED, DESCRIPTION).set(bundle.getString("jvm.debug.enabled"));
        node.get(ATTRIBUTES, JVM_DEBUG_OPTIONS, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, JVM_DEBUG_OPTIONS, DESCRIPTION).set(bundle.getString("jvm.debug.options"));
        return node;
    }

    public static ModelNode getJVMAddDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(REMOVE);
        node.get(DESCRIPTION).set(bundle.getString("jvm.add"));
        node.get(REQUEST_PROPERTIES, TYPE, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, TYPE, DESCRIPTION).set(bundle.getString("jvm.type"));
        node.get(REQUEST_PROPERTIES, TYPE, REQUIRED).set(false);

        node.get(REQUEST_PROPERTIES, JVM_AGENT_LIB, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JVM_AGENT_LIB, DESCRIPTION).set(bundle.getString("jvm.agent.lib"));
        node.get(REQUEST_PROPERTIES, JVM_AGENT_LIB, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, JVM_AGENT_PATH, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JVM_AGENT_PATH, DESCRIPTION).set(bundle.getString("jvm.agent.path"));
        node.get(REQUEST_PROPERTIES, JVM_AGENT_PATH, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, JVM_ENV_CLASSPATH_IGNORED, TYPE).set(ModelType.BOOLEAN);
        node.get(REQUEST_PROPERTIES, JVM_ENV_CLASSPATH_IGNORED, DESCRIPTION).set(bundle.getString("jvm.env.classpath.ignored"));
        node.get(REQUEST_PROPERTIES, JVM_ENV_CLASSPATH_IGNORED, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, JVM_ENV_VARIABLES, TYPE).set(ModelType.LIST);
        node.get(REQUEST_PROPERTIES, JVM_ENV_VARIABLES, VALUE_TYPE).set(ModelType.PROPERTY);
        node.get(REQUEST_PROPERTIES, JVM_ENV_VARIABLES, DESCRIPTION).set(bundle.getString("jvm.env.variables"));
        node.get(REQUEST_PROPERTIES, JVM_ENV_VARIABLES, REQUIRED).set(false);

        node.get(REQUEST_PROPERTIES, JVM_JAVA_AGENT, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JVM_JAVA_AGENT, DESCRIPTION).set(bundle.getString("jvm.javaagent"));
        node.get(REQUEST_PROPERTIES, JVM_JAVA_AGENT, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, JVM_JAVA_HOME, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JVM_JAVA_HOME, DESCRIPTION).set(bundle.getString("jvm.java.home"));
        node.get(REQUEST_PROPERTIES, JVM_JAVA_HOME, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, JVM_OPTIONS, TYPE).set(ModelType.LIST);
        node.get(REQUEST_PROPERTIES, JVM_OPTIONS, VALUE_TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JVM_OPTIONS, DESCRIPTION).set(bundle.getString("jvm.options"));
        node.get(REQUEST_PROPERTIES, JVM_OPTIONS, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, JVM_STACK, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JVM_STACK, DESCRIPTION).set(bundle.getString("jvm.stack"));
        node.get(REQUEST_PROPERTIES, JVM_STACK, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, JVM_TYPE, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JVM_TYPE, DESCRIPTION).set(bundle.getString("jvm.type"));
        node.get(REQUEST_PROPERTIES, JVM_TYPE, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, JVM_HEAP, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JVM_HEAP, DESCRIPTION).set(bundle.getString("jvm.heap"));
        node.get(REQUEST_PROPERTIES, JVM_HEAP, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, JVM_MAX_HEAP, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JVM_MAX_HEAP, DESCRIPTION).set(bundle.getString("jvm.heap.max"));
        node.get(REQUEST_PROPERTIES, JVM_MAX_HEAP, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, JVM_PERMGEN, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JVM_PERMGEN, DESCRIPTION).set(bundle.getString("jvm.permgen"));
        node.get(REQUEST_PROPERTIES, JVM_PERMGEN, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, JVM_MAX_PERMGEN, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JVM_MAX_PERMGEN, DESCRIPTION).set(bundle.getString("jvm.permgen.max"));
        node.get(REQUEST_PROPERTIES, JVM_MAX_PERMGEN, REQUIRED).set(false);

        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    public static ModelNode getServerJVMAddDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = getJVMAddDescription(locale);
        node.get(REQUEST_PROPERTIES, JVM_DEBUG_ENABLED, TYPE).set(ModelType.BOOLEAN);
        node.get(REQUEST_PROPERTIES, JVM_DEBUG_ENABLED, DESCRIPTION).set(bundle.getString("jvm.debug.enabled"));
        node.get(REQUEST_PROPERTIES, JVM_DEBUG_ENABLED, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, JVM_DEBUG_OPTIONS, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JVM_DEBUG_OPTIONS, DESCRIPTION).set(bundle.getString("jvm.debug.options"));
        node.get(REQUEST_PROPERTIES, JVM_DEBUG_OPTIONS, REQUIRED).set(false);
        return node;
    }

    public static ModelNode getJVMRemoveDescription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(REMOVE);
        node.get(DESCRIPTION).set(bundle.getString("jvm.remove"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    public static ModelNode getOptionAddOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("add-jvm-option");
        node.get(DESCRIPTION).set(bundle.getString("jvm.option.add"));
        node.get(REQUEST_PROPERTIES, JVM_OPTION, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JVM_OPTION, DESCRIPTION).set(bundle.getString("jvm.option"));
        node.get(REQUEST_PROPERTIES, JVM_OPTION, REQUIRED).set(true);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    public static ModelNode getOptionRemoveOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set("remove-jvm-option");
        node.get(DESCRIPTION).set(bundle.getString("jvm.option.remove"));
        node.get(REQUEST_PROPERTIES, JVM_OPTION, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JVM_OPTION, DESCRIPTION).set(bundle.getString("jvm.option"));
        node.get(REQUEST_PROPERTIES, JVM_OPTION, REQUIRED).set(true);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}