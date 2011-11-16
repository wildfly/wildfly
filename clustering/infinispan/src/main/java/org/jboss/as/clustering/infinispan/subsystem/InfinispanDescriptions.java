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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class InfinispanDescriptions {

    public static final String RESOURCE_NAME = InfinispanDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    private InfinispanDescriptions() {
        // Hide
    }

    static ModelNode getSubsystemDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createDescription(resources, "infinispan");
        description.get(ModelDescriptionConstants.HEAD_COMMENT_ALLOWED).set(true);
        description.get(ModelDescriptionConstants.TAIL_COMMENT_ALLOWED).set(true);
        description.get(ModelDescriptionConstants.NAMESPACE).set(Namespace.CURRENT.getUri());

        description.get(CHILDREN, ModelKeys.CACHE_CONTAINER, DESCRIPTION).set(resources.getString("infinispan.container"));
        description.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MIN_OCCURS).set(1);
        description.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MAX_OCCURS).set(Integer.MAX_VALUE);
        description.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MODEL_DESCRIPTION).setEmptyObject();
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
        ModelNode requestProperties = description.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        addNode(requestProperties, ModelKeys.DEFAULT_CACHE, resources.getString("infinispan.container.default-cache"), ModelType.STRING, true);
        addNode(requestProperties, ModelKeys.LISTENER_EXECUTOR, resources.getString("infinispan.container.listener-executor"), ModelType.STRING, false);
        addNode(requestProperties, ModelKeys.EVICTION_EXECUTOR, resources.getString("infinispan.container.eviction-executor"), ModelType.STRING, false);
        addNode(requestProperties, ModelKeys.REPLICATION_QUEUE_EXECUTOR, resources.getString("infinispan.container.replication-queue-executor"), ModelType.STRING, false);
        addNode(requestProperties, ModelKeys.JNDI_NAME, resources.getString("infinispan.container.jndi-name"), ModelType.STRING, false);
        addNode(requestProperties, ModelKeys.ALIAS, resources.getString("infinispan.container.alias"), ModelType.LIST, false).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);

        ModelNode transport = addNode(requestProperties, ModelKeys.TRANSPORT, resources.getString("infinispan.container.transport"), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        addNode(transport, ModelKeys.SITE, resources.getString("infinispan.container.transport.site"), ModelType.STRING, false);
        addNode(transport, ModelKeys.RACK, resources.getString("infinispan.container.transport.rack"), ModelType.STRING, false);
        addNode(transport, ModelKeys.MACHINE, resources.getString("infinispan.container.transport.machine"), ModelType.STRING, false);
        addNode(transport, ModelKeys.SITE, resources.getString("infinispan.container.transport.site"), ModelType.STRING, false);
        addNode(transport, ModelKeys.EXECUTOR, resources.getString("infinispan.container.transport.executor"), ModelType.STRING, false);
        addNode(transport, ModelKeys.LOCK_TIMEOUT, resources.getString("infinispan.container.transport.lock-timeout"), ModelType.LONG, false);

        addNode(requestProperties, ModelKeys.CACHE, resources.getString("infinispan.container.cache"), ModelType.LIST, false).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.OBJECT);

        return description;
    }

    static ModelNode getCacheContainerRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createCacheContainerOperationDescription(ModelDescriptionConstants.REMOVE, resources);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES).setEmptyObject();
        return description;
    }

    private static ResourceBundle getResources(Locale locale) {
        return ResourceBundle.getBundle(RESOURCE_NAME, (locale == null) ? Locale.getDefault() : locale);
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

    private static ModelNode addNode(ModelNode parent, String attribute, String description,
             ModelType type, boolean required) {
       ModelNode node = parent.get(attribute);
       node.get(ModelDescriptionConstants.DESCRIPTION).set(description);
       node.get(ModelDescriptionConstants.TYPE).set(type);
       node.get(ModelDescriptionConstants.REQUIRED).set(required);

       return node;
    }
}
