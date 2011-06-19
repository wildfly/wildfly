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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class LocalDescriptions {
    private LocalDescriptions() {
        // Hide
    }

    static ModelNode getSubsystemDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createDescription(resources, "infinispan");
        description.get(ModelDescriptionConstants.HEAD_COMMENT_ALLOWED).set(true);
        description.get(ModelDescriptionConstants.TAIL_COMMENT_ALLOWED).set(true);
        description.get(ModelDescriptionConstants.NAMESPACE).set(Namespace.CURRENT.getUri());
        return description;
    }

    static ModelNode getSubsystemAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createSubsystemOperationDescription(ModelDescriptionConstants.ADD, resources);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.DEFAULT_CACHE_CONTAINER, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.DEFAULT_CACHE_CONTAINER, ModelDescriptionConstants.DESCRIPTION).set(resources.getString("infinispan.default-container"));
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.DEFAULT_CACHE_CONTAINER, ModelDescriptionConstants.REQUIRED).set(false);
        return description;
    }

    static ModelNode getSubsystemDescribeDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createSubsystemOperationDescription(ModelDescriptionConstants.DESCRIBE, resources);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES).setEmptyObject();
        description.get(ModelDescriptionConstants.REPLY_PROPERTIES, ModelDescriptionConstants.TYPE).set(ModelType.LIST);
        description.get(ModelDescriptionConstants.REPLY_PROPERTIES, ModelDescriptionConstants.VALUE_TYPE).set(ModelType.OBJECT);
        return description;
    }

    static ModelNode getCacheContainerDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        return createDescription(resources, "infinispan.container");
    }

    static ModelNode getCacheContainerAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createCacheContainerOperationDescription(ModelDescriptionConstants.ADD, resources);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.DEFAULT_CACHE, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.DEFAULT_CACHE, ModelDescriptionConstants.DESCRIPTION).set(resources.getString("infinispan.container.default-cache"));
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.LISTENER_EXECUTOR, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.LISTENER_EXECUTOR, ModelDescriptionConstants.DESCRIPTION).set(resources.getString("infinispan.container.listener-executor"));
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.EVICTION_EXECUTOR, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.EVICTION_EXECUTOR, ModelDescriptionConstants.DESCRIPTION).set(resources.getString("infinispan.container.eviction-executor"));
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.REPLICATION_QUEUE_EXECUTOR, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.REPLICATION_QUEUE_EXECUTOR, ModelDescriptionConstants.DESCRIPTION).set(resources.getString("infinispan.container.replication-queue-executor"));
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.ALIAS, ModelDescriptionConstants.TYPE).set(ModelType.LIST);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.ALIAS, ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.ALIAS, ModelDescriptionConstants.DESCRIPTION).set(resources.getString("infinispan.container.alias"));
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.TRANSPORT, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.TRANSPORT, ModelDescriptionConstants.DESCRIPTION).set(resources.getString("infinispan.container.transport"));
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.CACHE, ModelDescriptionConstants.TYPE).set(ModelType.LIST);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.CACHE, ModelDescriptionConstants.VALUE_TYPE).set(ModelType.OBJECT);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.CACHE, ModelDescriptionConstants.DESCRIPTION).set(resources.getString("infinispan.container.cache"));
        return description;
    }

    static ModelNode getCacheContainerRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createCacheContainerOperationDescription(ModelDescriptionConstants.REMOVE, resources);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES).setEmptyObject();
        return description;
    }

    private static ResourceBundle getResources(Locale locale) {
        return ResourceBundle.getBundle(LocalDescriptions.class.getName(), (locale == null) ? Locale.getDefault() : locale);
    }

    private static ModelNode createDescription(ResourceBundle resources, String key) {
        return createOperationDescription(null, resources, key);
    }

    private static ModelNode createOperationDescription(String operation, ResourceBundle resources, String key) {
        ModelNode description = new ModelNode();
        if (operation != null) {
            description.get(ModelDescriptionConstants.OPERATION_NAME).set(operation);
        }
        description.get(ModelDescriptionConstants.DESCRIPTION).set(resources.getString(key));
        return description;
    }

    private static ModelNode createSubsystemOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan." + operation);
    }

    private static ModelNode createCacheContainerOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.container." + operation);
    }
}
