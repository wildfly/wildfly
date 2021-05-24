    /*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
package org.jboss.as.connector.util;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

import java.util.HashMap;
import java.util.Map;

public class ModelNodeUtil {
    public static Long getLongIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key) throws OperationFailedException {

        ModelNode resolvedNode = key.resolveModelAttribute(context, dataSourceNode);
        return resolvedNode.isDefined() ? resolvedNode.asLong() : null;
    }

    public static Integer getIntIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key) throws OperationFailedException {

        ModelNode resolvedNode = key.resolveModelAttribute(context, dataSourceNode);
        return resolvedNode.isDefined() ? resolvedNode.asInt() : null;
    }

    public static Boolean getBooleanIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key) throws OperationFailedException {
        ModelNode resolvedNode = key.resolveModelAttribute(context, dataSourceNode);
        return resolvedNode.isDefined() ? resolvedNode.asBoolean() : null;

    }

    public static String getResolvedStringIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key) throws OperationFailedException {
        ModelNode resolvedNode = key.resolveModelAttribute(context, dataSourceNode);
        String resolvedString = resolvedNode.isDefined() ? resolvedNode.asString() : null;
        if (resolvedString != null && resolvedString.trim().length() == 0) {
            resolvedString = null;
        }
        return resolvedString;

    }

    public static Extension extractExtension(final OperationContext operationContext, final ModelNode dataSourceNode, final SimpleAttributeDefinition classNameAttribute,
                                             final PropertiesAttributeDefinition propertyNameAttribute) throws ValidateException, OperationFailedException {
        return extractExtension(operationContext, dataSourceNode, classNameAttribute, null, propertyNameAttribute);
    }

    public static Extension extractExtension(final OperationContext operationContext, final ModelNode dataSourceNode, final SimpleAttributeDefinition classNameAttribute,
                                             final SimpleAttributeDefinition moduleNameAttribute, final PropertiesAttributeDefinition propertyNameAttribute)
            throws ValidateException, OperationFailedException {
        if (dataSourceNode.hasDefined(classNameAttribute.getName())) {
            String className = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, classNameAttribute);

            String moduleName = null;
            if (moduleNameAttribute != null) {
                moduleName = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, moduleNameAttribute);
            }

            ClassLoader moduleClassLoader = null;
            if (moduleName != null) {
                try {
                    final ModuleIdentifier moduleId = ModuleIdentifier.create(moduleName);
                    moduleClassLoader = Module.getCallerModuleLoader().loadModule(moduleId).getClassLoader();
                } catch (ModuleLoadException exception) {
                    throw ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER.cannotLoadModule(exception);
                }
            }

            Map<String, String> unwrapped = propertyNameAttribute.unwrap(operationContext, dataSourceNode);
            Map<String, String> property = unwrapped.size() > 0 ? unwrapped : null;

            return new Extension(className, moduleClassLoader, property);
        } else {
            return null;
        }
    }

    public static Map<String,String> extractMap(ModelNode operation,  ObjectListAttributeDefinition objList, SimpleAttributeDefinition keyAttribute, SimpleAttributeDefinition valueAttribute) {
            Map<String, String> returnMap = null;
            if (operation.hasDefined(objList.getName())) {
                returnMap = new HashMap<String, String>(operation.get(objList.getName()).asList().size());
                for (ModelNode node : operation.get(objList.getName()).asList()) {
                    returnMap.put(node.asObject().get(keyAttribute.getName()).asString(), node.asObject().get(valueAttribute.getName()).asString());
                }

            }
            return returnMap;
        }
}
