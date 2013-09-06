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
package org.jboss.as.cli.operation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jboss.as.cli.operation.OperationRequestAddress;

/**
 * This implementation is not thread-safe.
 *
 * @author Alexey Loubyansky
 */
public class DefaultOperationRequestAddress implements OperationRequestAddress {

    private List<NodeImpl> nodes = Collections.emptyList();

    public DefaultOperationRequestAddress() {
    }

    /**
     * Creates a prefix and initializes it to the value of the argument.
     * @param initial  the initial value
     */
    public DefaultOperationRequestAddress(OperationRequestAddress initial) {
        if(!initial.isEmpty()) {
            for(Node node : initial) {
                toNode(node.getType(), node.getName());
            }
        }
    }

    @Override
    public void toNodeType(String nodeType) {

        addNode(new NodeImpl(nodeType, null));
    }

    @Override
    public void toNode(String nodeName) {

        if(nodes.isEmpty())
            throw new IllegalStateException("Node name '" + nodeName + "' should follow a node type.");

        nodes.get(nodes.size() - 1).name = nodeName;
    }

    @Override
    public void toNode(String nodeType, String nodeName) {

        if(endsOnType()) {
            throw new IllegalStateException("The prefix ends on a type. A node name must be specified before this method can be invoked.");
        }
        addNode(new NodeImpl(nodeType, nodeName));
    }

    @Override
    public void appendPath(OperationRequestAddress path) {
        if(path == null || path.isEmpty()) {
            return;
        }
        if(nodes.isEmpty()) {
            nodes = new ArrayList<NodeImpl>(path.length());
        }
        for(Node n : path) {
            nodes.add(new NodeImpl(n.getType(), n.getType()));
        }
    }

    @Override
    public String toNodeType() {

        if(nodes.isEmpty()) {
            return null;
        }
        final int index = nodes.size() - 1;
        String name = nodes.get(index).name;
        nodes.get(index).name = null;
        return name;
    }

    @Override
    public Node toParentNode() {

        if(nodes.isEmpty()) {
            return null;
        }
        return nodes.remove(nodes.size() - 1);
    }

    @Override
    public void reset() {
        nodes.clear();
    }

    @Override
    public boolean endsOnType() {
        if(nodes.isEmpty()) {
            return false;
        }

        NodeImpl node = nodes.get(nodes.size() - 1);
        return node.name == null;
    }

    @Override
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Override
    public int length() {
        return nodes.size();
    }

    @Override
    public Iterator<Node> iterator() {

        if(nodes.isEmpty()) {
            return Collections.emptyListIterator();
        }

        final Node[] array = nodes.toArray(new Node[nodes.size()]);
        return new Iterator<Node>() {

            int i = 0;

            @Override
            public boolean hasNext() {
                return i < array.length;
            }

            @Override
            public Node next() {
                return array[i++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    @Override
    public String getNodeType() {

        if(nodes.isEmpty()) {
            return null;
        }
        return nodes.get(nodes.size() - 1).type;
    }

    @Override
    public String getNodeName() {

        if(nodes.isEmpty()) {
            return null;
        }
        return nodes.get(nodes.size() - 1).name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof OperationRequestAddress))
            return false;

        OperationRequestAddress other = (OperationRequestAddress) obj;

        if(isEmpty() != other.isEmpty())
            return false;

        Iterator<Node> thisIterator = iterator();
        Iterator<Node> otherIterator = other.iterator();
        boolean result = true;
        while(result) {
            if(!thisIterator.next().equals(otherIterator.next())) {
                result = false;
            } else {
                if (!thisIterator.hasNext()) {
                    if (otherIterator.hasNext()) {
                        result = false;
                    }
                    break;
                }
                if (!otherIterator.hasNext()) {
                    if (thisIterator.hasNext()) {
                        result = false;
                    }
                    break;
                }
            }
        }
        return result;
    }

    private void addNode(NodeImpl node) {
        if(nodes.isEmpty()) {
            nodes = new ArrayList<NodeImpl>();
        }
        nodes.add(node);
    }

    private static final class NodeImpl implements Node {

        String type;
        String name;

        NodeImpl(String type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public String getType() {
            return type;
        }
        @Override
        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;

            if(!(obj instanceof Node))
                return false;

            Node other = (Node) obj;
            if (name == null) {
                if (other.getName() != null)
                    return false;
            } else if (!name.equals(other.getName()))
                return false;
            if (type == null) {
                if (other.getType() != null)
                    return false;
            } else if (!type.equals(other.getType()))
                return false;
            return true;
        }
    }
}
