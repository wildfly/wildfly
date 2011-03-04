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

import java.util.Iterator;

import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestAddress.Node;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultOperationRequestBuilder extends ValidatingOperationCallbackHandler {

    private ModelNode request = new ModelNode();
    private OperationRequestAddress prefix;

    public DefaultOperationRequestBuilder(OperationRequestAddress prefix) {
        if(prefix == null) {
            throw new IllegalArgumentException("Prefix can't be null");
        }
        this.prefix = new DefaultOperationRequestAddress(prefix);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#rootNode()
     */
    @Override
    public void rootNode() {
        prefix.reset();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#parentNode()
     */
    @Override
    public void parentNode() {
        prefix.toParentNode();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#nodeType()
     */
    @Override
    public void nodeType() {
        prefix.toNodeType();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#nodeTypeNameSeparator(int)
     */
    @Override
    public void nodeTypeNameSeparator(int index) {
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#nodeSeparator(int)
     */
    @Override
    public void nodeSeparator(int index) {
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#addressOperationSeparator(int)
     */
    @Override
    public void addressOperationSeparator(int index) {
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#operationName(java.lang.String)
     */
    @Override
    public void validatedOperationName(String operationName) {
        request.get("operation").set(operationName);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#propertyListStart(int)
     */
    @Override
    public void propertyListStart(int index) {
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#propertyNameValueSeparator(int)
     */
    @Override
    public void propertyNameValueSeparator(int index) {
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#propertySeparator(int)
     */
    @Override
    public void propertySeparator(int index) {
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#propertyListEnd(int)
     */
    @Override
    public void propertyListEnd(int index) {
    }

    @Override
    protected void validatedNodeType(String nodeType)
            throws OperationFormatException {
        prefix.toNodeType(nodeType);
    }

    @Override
    protected void validatedNodeName(String nodeName)
            throws OperationFormatException {
        prefix.toNode(nodeName);
    }

    @Override
    protected void validatedPropertyName(String propertyName)
            throws OperationFormatException {
        throw new OperationFormatException("Property '" + propertyName + "' is missing the value.");
    }

    @Override
    protected void validatedProperty(String name, String value,
            int nameValueSeparatorIndex) throws OperationFormatException {

        if(name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("The argument name is not specified: '" + name + "'");
        if(value == null || value.trim().isEmpty())
            throw new IllegalArgumentException("The argument value is not specified: '" + value + "'");
        ModelNode toSet = null;
        try {
            toSet = ModelNode.fromString(value);
        } catch (Exception e) {
            // just use the string
            toSet = new ModelNode().set(value);
        }
        request.get(name).set(toSet);
     }

    @Override
    protected void validatedNodeTypeOrName(String typeOrName)
            throws OperationFormatException {

        if(prefix.endsOnType()) {
            validatedNodeName(typeOrName);
        } else {
            validatedNodeType(typeOrName);
        }
    }

    /**
     * Makes sure that the operation name and the address have been set and returns a ModelNode
     * representing the operation request.
     */
    public ModelNode buildRequest() throws OperationFormatException {

        ModelNode address = request.get("address");
        if(prefix.isEmpty()) {
            address.setEmptyList();
        } else {
            Iterator<Node> iterator = prefix.iterator();
            while (iterator.hasNext()) {
                OperationRequestAddress.Node node = iterator.next();
                if (node.getName() != null) {
                    address.add(node.getType(), node.getName());
                } else if (iterator.hasNext()) {
                    throw new OperationFormatException(
                            "The node name is not specified for type '"
                                    + node.getType() + "'");
                }
            }
        }

        if(!request.hasDefined("operation")) {
            throw new OperationFormatException("The operation name is missing or the format of the operation request is wrong.");
        }

        return request;
    }
}
