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
package org.jboss.as.cli.operation;



/**
 * An instance of this interface represents a prefix for the operation request address part.
 *
 * @author Alexey Loubyansky
 */
public interface OperationRequestAddress extends Iterable<OperationRequestAddress.Node> {

    /**
     * Appends the node type to the prefix.
     * Note, the current prefix must end on the node name before this method
     * is invoked.
     *
     * @param nodeType  the node type to append to the prefix.
     */
    void toNodeType(String nodeType);

    /**
     * Appends the node name to the prefix.
     * Note, the current prefix must end on the node type before this method
     * is invoked.
     *
     * @param nodeName the node name to append to the prefix.
     */
    void toNode(String nodeName);

    /**
     * Appends the node to the prefix.
     * Note, the current prefix must end on the node (i.e. node name) before
     * this method is invoked.
     *
     * @param nodeType  the node type of the node to append to the prefix
     * @param nodeName  the node name of the node to append to the prefix
     */
    void toNode(String nodeType, String nodeName);

    /**
     * Sets the current prefix to the node type of the current node,
     * i.e. the node name is removed from the end of the prefix.
     * @return the node name the prefix ended on
     */
    String toNodeType();

    /**
     * Removes the last node in the prefix, i.e. moves the value a node up.
     * @return the node the prefix ended on
     */
    Node toParentNode();

    /**
     * Resets the prefix, i.e. this will make the prefix empty.
     */
    void reset();

    /**
     * Checks whether the prefix ends on a node type or a node name.
     * @return  true if the prefix ends on a node type, otherwise false.
     */
    boolean endsOnType();

    /**
     * Checks whether the prefix is empty.
     * @return  true if the prefix is empty, otherwise false.
     */
    boolean isEmpty();

    /**
     * Returns the node type of the last node.
     * @return the node type of the last node or null if the prefix is empty.
     */
    String getNodeType();

    /**
     * Returns the node name of the last node.
     * @return  the node name of the last node or null if the prefix ends
     * on a type or is empty.
     */
    String getNodeName();

    interface Node {

        String getType();

        String getName();
    }
}
