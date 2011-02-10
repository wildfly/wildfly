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
package org.jboss.as.cli.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.as.cli.OperationRequestBuilder;
import org.jboss.as.cli.Prefix;

/**
 * This implementation is not thread-safe.
 *
 * @author Alexey Loubyansky
 */
public class DefaultPrefix implements Prefix {

    private final List<NodeImpl> nodes = new ArrayList<NodeImpl>();

    /* (non-Javadoc)
     * @see org.jboss.as.cli.Prefix#apply(org.jboss.as.cli.OperationRequestBuilder)
     */
    @Override
    public void apply(OperationRequestBuilder builder) {

        Iterator<NodeImpl> iterator = nodes.iterator();
        while(iterator.hasNext()) {
            Node next = iterator.next();
            if(next.getName() == null) {
                if(iterator.hasNext()) {
                    throw new IllegalStateException("The node name is not specified for type '" + next.getType() + "'");
                } else {
                    builder.addNodeType(next.getType());
                }
            } else {
               builder.addNode(next.getType(), next.getName());
            }
        }
    }

    @Override
    public void toNodeType(String nodeType) {

        nodes.add(new NodeImpl(nodeType, null));
    }

    @Override
    public void toNode(String nodeName) {

        if(nodes.isEmpty())
            throw new IllegalStateException("The prefix should end with the node type before going to a specific node name.");

        nodes.get(nodes.size() - 1).name = nodeName;
    }

    @Override
    public void toNode(String nodeType, String nodeName) {

        nodes.add(new NodeImpl(nodeType, nodeName));
    }

    @Override
    public void toNodeType() {

        nodes.get(nodes.size() - 1).name = null;
    }

    @Override
    public void toParentNode() {

        if(nodes.isEmpty()) {
            return;
        }

        nodes.remove(nodes.size() - 1);
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
    public Iterator<Node> iterator() {

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
    }
}
