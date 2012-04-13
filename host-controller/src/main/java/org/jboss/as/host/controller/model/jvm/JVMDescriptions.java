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

package org.jboss.as.host.controller.model.jvm;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.host.controller.descriptions.HostDescriptionProviders;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 */
final class JVMDescriptions {

    private static final String RESOURCE_NAME = HostDescriptionProviders.class.getPackage().getName() + ".LocalDescriptions";

    static ModelNode getOptionAddOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(JVMOptionAddHandler.OPERATION_NAME);
        node.get(DESCRIPTION).set(bundle.getString("jvm.option.add"));
        node.get(REQUEST_PROPERTIES, JvmAttributes.JVM_OPTION, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JvmAttributes.JVM_OPTION, DESCRIPTION).set(bundle.getString("jvm.option"));
        node.get(REQUEST_PROPERTIES, JvmAttributes.JVM_OPTION, REQUIRED).set(true);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getOptionRemoveOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(JVMOptionRemoveHandler.OPERATION_NAME);
        node.get(DESCRIPTION).set(bundle.getString("jvm.option.remove"));
        node.get(REQUEST_PROPERTIES, JvmAttributes.JVM_OPTION, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, JvmAttributes.JVM_OPTION, DESCRIPTION).set(bundle.getString("jvm.option"));
        node.get(REQUEST_PROPERTIES, JvmAttributes.JVM_OPTION, REQUIRED).set(true);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getEnvVarAddOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(JVMEnvironmentVariableAddHandler.OPERATION_NAME);
        node.get(DESCRIPTION).set(bundle.getString("jvm.environment-variable.add"));
        node.get(REQUEST_PROPERTIES, ModelDescriptionConstants.NAME, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, ModelDescriptionConstants.NAME, DESCRIPTION).set(bundle.getString("jvm.environment-variable.name"));
        node.get(REQUEST_PROPERTIES, ModelDescriptionConstants.NAME, REQUIRED).set(true);
        node.get(REQUEST_PROPERTIES, ModelDescriptionConstants.VALUE, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, ModelDescriptionConstants.VALUE, DESCRIPTION).set(bundle.getString("jvm.environment-variable.value"));
        node.get(REQUEST_PROPERTIES, ModelDescriptionConstants.VALUE, REQUIRED).set(true);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static ModelNode getEnvVarRemoveOperation(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(JVMEnvironmentVariableRemoveHandler.OPERATION_NAME);
        node.get(DESCRIPTION).set(bundle.getString("jvm.environment-variable.remove"));
        node.get(REQUEST_PROPERTIES, ModelDescriptionConstants.NAME, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, ModelDescriptionConstants.NAME, DESCRIPTION).set(bundle.getString("jvm.environment-variable.name"));
        node.get(REQUEST_PROPERTIES, ModelDescriptionConstants.NAME, REQUIRED).set(true);
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
