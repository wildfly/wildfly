/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.security.common;

import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.wildfly.test.security.common.elytron.ModelNodeConvertable;

/**
 * Helper methods for {@link ModelNode} class.
 *
 * @author Josef Cacek
 */
public class ModelNodeUtil {

    /**
     * Set attribute of given node if the value is not-<code>null</code>.
     */
    public static void setIfNotNull(ModelNode node, String attribute, String value) {
        if (value != null) {
            node.get(attribute).set(value);
        }
    }

    /**
     * Set attribute of given node if the value is not-<code>null</code>.
     */
    public static void setIfNotNull(ModelNode node, String attribute, Boolean value) {
        if (value != null) {
            node.get(attribute).set(value);
        }
    }

    /**
     * Set attribute of given node if the value is not-<code>null</code>.
     */
    public static void setIfNotNull(ModelNode node, String attribute, Integer value) {
        if (value != null) {
            node.get(attribute).set(value);
        }
    }

    /**
     * Set attribute of given node if the value is not-<code>null</code>.
     */
    public static void setIfNotNull(ModelNode node, String attribute, Long value) {
        if (value != null) {
            node.get(attribute).set(value.longValue());
        }
    }

    /**
     * Set attribute of given node if the value is not-<code>null</code>.
     */
    public static void setIfNotNull(ModelNode node, String attribute, ModelNodeConvertable value) {
        if (value != null) {
            ModelNode modelNode = value.toModelNode();
            if (modelNode != null) {
                node.get(attribute).set(modelNode);
            }
        }
    }

    /**
     * Set list attribute of given node if the value is not-<code>null</code>.
     */
    public static void setIfNotNull(ModelNode node, String attribute, Map<String, String> objectValue) {
        if (objectValue != null) {
            ModelNode objectNode = node.get(attribute);
            for (Map.Entry<String, String> entry : objectValue.entrySet()) {
                objectNode.get(entry.getKey()).set(entry.getValue());
            }
        }
    }

    /**
     * Set list attribute of given node if the value is not-<code>null</code>.
     */
    public static void setIfNotNull(ModelNode node, String attribute, String... listValue) {
        if (listValue != null) {
            ModelNode listNode = node.get(attribute);
            for (String value : listValue) {
                listNode.add(value);
            }
        }
    }

    /**
     * Adds given items to list attribute.
     */
    public static void setIfNotNull(ModelNode node, String attribute, ModelNodeConvertable... items) {
        if (items != null && items.length > 0) {
            ModelNode listNode = node.get(attribute);
            for (ModelNodeConvertable item : items) {
                listNode.add(item.toModelNode());
            }
        }
    }

}
