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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.ParsedOperationRequest;
import org.jboss.as.cli.operation.OperationRequestAddress;

/**
*
* @author Alexey Loubyansky
*/
public class DefaultOperationCallbackHandler extends ValidatingOperationCallbackHandler implements ParsedOperationRequest {

    private static final int SEPARATOR_NONE = 0;
    private static final int SEPARATOR_NODE_TYPE_NAME = 1;
    private static final int SEPARATOR_NODE = 2;
    private static final int SEPARATOR_ADDRESS_OPERATION = 3;
    private static final int SEPARATOR_OPERATION_ARGUMENTS = 4;
    private static final int SEPARATOR_ARG_NAME_VALUE = 5;
    private static final int SEPARATOR_ARG = 6;

    private int separator = SEPARATOR_NONE;
    private int lastSeparatorIndex = -1;

    private boolean operationComplete;
    private String operationName;
    private OperationRequestAddress address;
    private Map<String, String> props;

    public DefaultOperationCallbackHandler() {
    }

    public DefaultOperationCallbackHandler(OperationRequestAddress prefix) {
        address = prefix;
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
    public boolean hasAddress() {
        return address != null;
    }

    @Override
    public OperationRequestAddress getAddress() {
        return address;
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
        return props != null && !props.isEmpty();
    }

    @Override
    public void validatedNodeType(String nodeType) throws OperationFormatException {

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
    }

    @Override
    public void nodeTypeNameSeparator(int index) {
        separator = SEPARATOR_NODE_TYPE_NAME;
        this.lastSeparatorIndex = index;
    }

    @Override
    public void validatedNodeName(String nodeName) throws OperationFormatException {

        if(address == null) {
            address = new DefaultOperationRequestAddress();
        }

        if(!address.endsOnType()) {
            throw new OperationFormatException("The prefix should end with the node type before going to a specific node name.");
        }

        address.toNode(nodeName);
        separator = SEPARATOR_NONE;
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
    public void validatedOperationName(String operationName) throws OperationFormatException {

        this.operationName = operationName;
        separator = SEPARATOR_NONE;
    }

    @Override
    public void propertyListStart(int index) {
        separator = SEPARATOR_OPERATION_ARGUMENTS;
        this.lastSeparatorIndex = index;
    }

    @Override
    public void validatedPropertyName(String argName) throws OperationFormatException {

        if(props == null) {
            props = new HashMap<String, String>();
        }
        props.put(argName, null);
        separator = SEPARATOR_NONE;
    }

    @Override
    public void propertyNameValueSeparator(int index) {
        separator = SEPARATOR_ARG_NAME_VALUE;
        this.lastSeparatorIndex = index;
    }

    @Override
    public void validatedProperty(String name, String value, int nameValueSeparatorIndex) throws OperationFormatException {

        if (value.isEmpty()) {
            throw new OperationFormatException(
                    "The argument value is missing or the format is wrong for argument '"
                            + value + "'");
        }

        if(props == null) {
            props = new HashMap<String, String>();
        }
        props.put(name, value);
        separator = SEPARATOR_NONE;
        this.lastSeparatorIndex = nameValueSeparatorIndex;
    }

    @Override
    public void propertySeparator(int index) {
        separator = SEPARATOR_ARG;
        this.lastSeparatorIndex = index;

    }

    @Override
    public void propertyListEnd(int index) {
        separator = SEPARATOR_NONE;
        operationComplete = true;
        this.lastSeparatorIndex = index;
    }

    @Override
    public void rootNode() {
        if(address == null) {
            address = new DefaultOperationRequestAddress();
        } else {
            address.reset();
        }
        separator = SEPARATOR_NONE;
    }

    @Override
    public void parentNode() {
        if(address == null) {
            throw new IllegalStateException("The address hasn't been initialized yet.");
        }
        address.toParentNode();
        separator = SEPARATOR_NONE;
    }

    @Override
    public void nodeType() {
        if(address == null) {
            throw new IllegalStateException("The address hasn't been initialized yet.");
        }
        address.toNodeType();
        separator = SEPARATOR_NONE;
    }

    @Override
    public void nodeTypeOrName(String typeOrName) throws OperationFormatException {

        if(address == null) {
            address = new DefaultOperationRequestAddress();
        }

        if(address.endsOnType()) {
            nodeName(typeOrName);
        } else {
            nodeType(typeOrName);
        }
        separator = SEPARATOR_NONE;
    }

    @Override
    public Set<String> getPropertyNames() {
        return props == null ? Collections.<String>emptySet() : props.keySet();
    }

    @Override
    public String getPropertyValue(String name) {
        return props == null ? null : props.get(name);
    }

    @Override
    public int getLastSeparatorIndex() {
        return lastSeparatorIndex;
    }
}
