/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common;

import org.jboss.dmr.ModelNode;

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

}
