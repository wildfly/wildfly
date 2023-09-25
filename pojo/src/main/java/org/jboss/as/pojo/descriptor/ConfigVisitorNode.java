/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

/**
 * Config visitor node.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface ConfigVisitorNode {
    /**
     * Visit metadata node.
     * e.g. add dependencies to service builder.
     *
     * @param visitor the config visitor
     */
    void visit(ConfigVisitor visitor);

    /**
     * Get children.
     *
     * @param visitor the current visitor
     * @return the config node children
     */
    Iterable<ConfigVisitorNode> getChildren(ConfigVisitor visitor);
}
