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

import org.jboss.as.cli.OperationRequestBuilder;
import org.jboss.dmr.ModelNode;

/**
 * The default request builder implementation.
 * An instance of this class is not thread-safe and is created once for each request.
 *
 * @author Alexey Loubyansky
 */
public class DefaultOperationRequestBuilder implements OperationRequestBuilder {

    private ModelNode request = new ModelNode();

    private String prefixNodeType;

    /**
     * Will throw an IllegalArgumentException if the name is null, an empty string or
     * a whitespace combination.
     *
     * @see org.jboss.as.cli.OperationRequestBuilder#setOperationName(java.lang.String)
     */
    @Override
    public void setOperationName(String name) {
        if(name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("The operation name is not specified: '" + name + "'");
        request.get("operation").set(name);
    }

    /**
     * Will throw an IllegalArgumentException if the type or the name is null, an empty string or
     * a whitespace combination.
     *
     * @see org.jboss.as.cli.OperationRequestBuilder#addNode(java.lang.String, java.lang.String)
     */
    @Override
    public void addNode(String type, String name) {
        if(prefixNodeType != null)
            throw new IllegalStateException("Can't accept node '" + type + "=" + name +
                    "' since the prefix ended on the node type '" + prefixNodeType + "'");
        if(type == null || type.trim().isEmpty())
            throw new IllegalArgumentException("The node type is not specified: '" + type + "'");
        if(name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("The node name is not specified: '" + name + "'");
        request.get("address").add(type, name);
    }

    @Override
    public void addNodeType(String type) {

        if(prefixNodeType != null)
            throw new IllegalStateException("Can't accept another node type '" + type +
                    "', the node type has been set to '" + prefixNodeType + "'.");

        this.prefixNodeType = type;
    }

    @Override
    public void addNodeName(String name) {

        if(prefixNodeType == null)
            throw new IllegalArgumentException("Can't accept node name '" + name +
                    "' since the prefix doesn't appear to end on a node type");
        String nodeType = prefixNodeType;
        prefixNodeType = null;
        addNode(nodeType, name);
    }

    /**
     * Will throw an IllegalArgumentException if the name or the value is null, an empty string or
     * a whitespace combination.
     * TODO: for the value an empty string or a whitespace is probably a valid value.
     *
     * @see org.jboss.as.cli.OperationRequestBuilder#addArgument(java.lang.String, java.lang.String)
     */
    @Override
    public void addArgument(String name, String value) {
        if(name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("The argument name is not specified: '" + name + "'");
        if(value == null || value.trim().isEmpty())
            throw new IllegalArgumentException("The argument value is not specified: '" + value + "'");
        request.get(name).set(value);
    }

    /**
     * Makes sure that the operation name and the address have been set and returns a ModelNode
     * representing the operation request.
     *
     * @see org.jboss.as.cli.OperationRequestBuilder#buildRequest()
     */
    @Override
    public ModelNode buildRequest() {
        request.require("operation");
        //request.require("address");
        if(!request.has("address")) {
            request.get("address").setEmptyList();
        }

        return request;
    }
}
