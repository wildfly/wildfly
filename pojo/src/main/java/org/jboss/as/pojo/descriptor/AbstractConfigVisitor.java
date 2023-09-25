/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Abstract config visitor.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractConfigVisitor implements ConfigVisitor {
    private Deque<ConfigVisitorNode> nodes = new ArrayDeque<ConfigVisitorNode>();

    public void visit(ConfigVisitorNode node) {
        nodes.push(node);
        try {
            for (ConfigVisitorNode child : node.getChildren(this)) {
                child.visit(this);
            }
        } finally {
            nodes.pop();
        }
    }

    /**
     * Get current nodes.
     *
     * @return the current nodes
     */
    public Deque<ConfigVisitorNode> getCurrentNodes() {
        return nodes;
    }
}
