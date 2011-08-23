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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestAddress.Node;
import org.jboss.as.cli.parsing.ParserUtil;
import org.jboss.dmr.ModelNode;

/**
*
* @author Alexey Loubyansky
*/
public class DefaultCallbackHandler extends ValidatingCallbackHandler implements ParsedCommandLine {

    private static final int SEPARATOR_NONE = 0;
    private static final int SEPARATOR_NODE_TYPE_NAME = 1;
    private static final int SEPARATOR_NODE = 2;
    private static final int SEPARATOR_ADDRESS_OPERATION = 3;
    private static final int SEPARATOR_OPERATION_ARGUMENTS = 4;
    private static final int SEPARATOR_ARG_NAME_VALUE = 5;
    private static final int SEPARATOR_ARG = 6;

    private static final DefaultOperationRequestAddress EMPTY_ADDRESS = new DefaultOperationRequestAddress();

    private int separator = SEPARATOR_NONE;
    private int lastSeparatorIndex = -1;
    private int lastChunkIndex = 0;

    private boolean operationComplete;
    private String operationName;
    private OperationRequestAddress address;
    private Map<String, String> props = new HashMap<String, String>();
    private List<String> otherArgs = new ArrayList<String>();
    private String outputTarget;

    private String lastPropName;
    private String lastPropValue;

    public DefaultCallbackHandler() {
    }

    public DefaultCallbackHandler(OperationRequestAddress prefix) {
        address = prefix;
    }

    public void parse(OperationRequestAddress initialAddress, String argsStr) throws CommandFormatException {
        reset();
        if(initialAddress != null) {
            address = new DefaultOperationRequestAddress(initialAddress);
        }
        ParserUtil.parse(argsStr, this);
    }

    public void parseOperation(OperationRequestAddress prefix, String argsStr) throws CommandFormatException {
        reset();
        if(prefix != null) {
            address = new DefaultOperationRequestAddress(prefix);
        }
        ParserUtil.parseOperationRequest(argsStr, this);
    }

    public void reset() {
        operationComplete = false;
        operationName = null;
        address = null;
        props.clear();
        otherArgs.clear();
        outputTarget = null;
        lastPropName = null;
        lastPropValue = null;
        lastSeparatorIndex = -1;
        lastChunkIndex = 0;
    }

    public List<String> getOtherProperties() {
        return otherArgs;
    }

    @Override
    public boolean isRequestComplete() {
        return operationComplete;
    }

    @Override
    public boolean endsOnPropertySeparator() {
        return separator == SEPARATOR_ARG;
    }

    @Override
    public boolean endsOnPropertyValueSeparator() {
        return separator == SEPARATOR_ARG_NAME_VALUE;
    }

    @Override
    public boolean endsOnPropertyListStart() {
        return separator == SEPARATOR_OPERATION_ARGUMENTS;
    }

    @Override
    public boolean endsOnAddressOperationNameSeparator() {
        return separator == SEPARATOR_ADDRESS_OPERATION;
    }

    @Override
    public boolean endsOnNodeSeparator() {
        return separator == SEPARATOR_NODE;
    }

    @Override
    public boolean endsOnNodeTypeNameSeparator() {
        return separator == SEPARATOR_NODE_TYPE_NAME;
    }

    @Override
    public boolean endsOnSeparator() {
        return separator != SEPARATOR_NONE;
    }

    @Override
    public boolean hasAddress() {
        return address != null;
    }

    @Override
    public OperationRequestAddress getAddress() {
        return address == null ? EMPTY_ADDRESS : address;
    }

    @Override
    public boolean hasOperationName() {
        return operationName != null;
    }

    @Override
    public String getOperationName() {
        return operationName;
    }

    @Override
    public boolean hasProperties() {
        return !props.isEmpty() || !otherArgs.isEmpty();
    }

    @Override
    public boolean hasProperty(String propertyName) {
        return props.containsKey(propertyName);
    }

    @Override
    public void validatedNodeType(int index, String nodeType) throws OperationFormatException {

        if(address == null) {
            address = new DefaultOperationRequestAddress();
        } else if (address.endsOnType()) {
            throw new OperationFormatException(
                    "Can't proceed with node type '"
                            + nodeType
                            + "' until the node name for the previous node type has been specified.");
        }

        address.toNodeType(nodeType);
        separator = SEPARATOR_NONE;
        lastChunkIndex = index;
    }

    @Override
    public void nodeTypeNameSeparator(int index) {
        separator = SEPARATOR_NODE_TYPE_NAME;
        this.lastSeparatorIndex = index;
    }

    @Override
    public void validatedNodeName(int index, String nodeName) throws OperationFormatException {

        if(address == null) {
            address = new DefaultOperationRequestAddress();
        }

        if(!address.endsOnType()) {
            throw new OperationFormatException("Node path format is wrong around '" + nodeName + "' (index=" + index + ").");
        }

        address.toNode(nodeName);
        separator = SEPARATOR_NONE;
        lastChunkIndex = index;
    }

    @Override
    public void nodeSeparator(int index) {
        separator = SEPARATOR_NODE;
        this.lastSeparatorIndex = index;
    }

    @Override
    public void addressOperationSeparator(int index) {
        separator = SEPARATOR_ADDRESS_OPERATION;
        this.lastSeparatorIndex = index;
    }

    @Override
    public void validatedOperationName(int index, String operationName) throws OperationFormatException {

        this.operationName = operationName;
        separator = SEPARATOR_NONE;
        lastChunkIndex = index;
    }

    @Override
    public void propertyListStart(int index) {
        separator = SEPARATOR_OPERATION_ARGUMENTS;
        this.lastSeparatorIndex = index;
    }

    @Override
    //public void validatedPropertyName(String argName) throws OperationFormatException {
    public void propertyName(int index, String propertyName)
            throws OperationFormatException {
        props.put(propertyName, null);
        lastPropName = propertyName;
        lastPropValue = null;
        separator = SEPARATOR_NONE;
        lastChunkIndex = index;
    }

    @Override
    public void propertyNameValueSeparator(int index) {
        separator = SEPARATOR_ARG_NAME_VALUE;
        this.lastSeparatorIndex = index;
    }

    @Override
    //public void validatedProperty(String name, String value, int nameValueSeparatorIndex) throws OperationFormatException {
    public void property(String name, String value, int nameValueSeparatorIndex)
            throws OperationFormatException {

/*        if (value.isEmpty()) {
            throw new OperationFormatException(
                    "The argument value is missing or the format is wrong for argument '"
                            + value + "'");
        }
*/
        if(name == null) {
            otherArgs.add(value);
        } else {
            props.put(name, value);
        }
        lastPropName = name;
        lastPropValue = value;
        separator = SEPARATOR_NONE;
        if(nameValueSeparatorIndex >= 0) {
            this.lastSeparatorIndex = nameValueSeparatorIndex;
        }
        lastChunkIndex = nameValueSeparatorIndex;
    }

    @Override
    public void propertySeparator(int index) {
        separator = SEPARATOR_ARG;
        this.lastSeparatorIndex = index;
        this.lastPropName = null;
        this.lastPropValue = null;
    }

    @Override
    public void propertyListEnd(int index) {
        separator = SEPARATOR_NONE;
        operationComplete = true;
        this.lastSeparatorIndex = index;
        this.lastPropName = null;
        this.lastPropValue = null;
    }

    @Override
    public void rootNode(int index) {
        if(address == null) {
            address = new DefaultOperationRequestAddress();
        } else {
            address.reset();
        }
        separator = SEPARATOR_NONE;
        lastChunkIndex = index;
    }

    @Override
    public void parentNode(int index) {
        if(address == null) {
            throw new IllegalStateException("The address hasn't been initialized yet.");
        }
        address.toParentNode();
        separator = SEPARATOR_NONE;
        lastChunkIndex = index;
    }

    @Override
    public void nodeType(int index) {
        if(address == null) {
            throw new IllegalStateException("The address hasn't been initialized yet.");
        }
        address.toNodeType();
        separator = SEPARATOR_NONE;
        lastChunkIndex = index;
    }

    @Override
    public void nodeTypeOrName(int index, String typeOrName) throws OperationFormatException {

        if(address == null) {
            address = new DefaultOperationRequestAddress();
        }

        if(address.endsOnType()) {
            nodeName(index, typeOrName);
        } else {
            nodeType(index, typeOrName);
        }
        separator = SEPARATOR_NONE;
    }

    @Override
    public Set<String> getPropertyNames() {
        return props.keySet();
    }

    @Override
    public String getPropertyValue(String name) {
        return props.get(name);
    }

    @Override
    public int getLastSeparatorIndex() {
        return lastSeparatorIndex;
    }

    @Override
    public int getLastChunkIndex() {
        return lastChunkIndex;
    }

    @Override
    public void outputTarget(int index, String outputTarget) {
        this.outputTarget = outputTarget;
        lastChunkIndex = index;
    }

    public String getOutputTarget() {
        return outputTarget;
    }

    @Override
    protected void validatedProperty(String name, String value,
            int nameValueSeparatorIndex) throws OperationFormatException {
        // TODO Auto-generated method stub

    }

    @Override
    protected void validatedPropertyName(int index, String propertyName)
            throws OperationFormatException {
        // TODO Auto-generated method stub

    }

    @Override
    public String getLastParsedPropertyName() {
        return lastPropName;
    }

    @Override
    public String getLastParsedPropertyValue() {
        return lastPropValue;
    }

    public ModelNode toOperationRequest() throws OperationFormatException {
        ModelNode request = new ModelNode();
        ModelNode addressNode = request.get("address");
        if(address.isEmpty()) {
            addressNode.setEmptyList();
        } else {
            Iterator<Node> iterator = address.iterator();
            while (iterator.hasNext()) {
                OperationRequestAddress.Node node = iterator.next();
                if (node.getName() != null) {
                    addressNode.add(node.getType(), node.getName());
                } else if (iterator.hasNext()) {
                    throw new OperationFormatException(
                            "The node name is not specified for type '"
                                    + node.getType() + "'");
                }
            }
        }

        if(operationName == null || operationName.isEmpty()) {
            throw new OperationFormatException("The operation name is missing or the format of the operation request is wrong.");
        }
        request.get("operation").set(operationName);

        for(String propName : props.keySet()) {
            final String value = props.get(propName);
            if(propName == null || propName.trim().isEmpty())
                throw new OperationFormatException("The argument name is not specified: '" + propName + "'");
            if(value == null || value.trim().isEmpty())
                throw new OperationFormatException("The argument value is not specified for " + propName + ": '" + value + "'");
            ModelNode toSet = null;
            try {
                toSet = ModelNode.fromString(value);
            } catch (Exception e) {
                // just use the string
                toSet = new ModelNode().set(value);
            }
            request.get(propName).set(toSet);
        }

        return request;
    }
}
