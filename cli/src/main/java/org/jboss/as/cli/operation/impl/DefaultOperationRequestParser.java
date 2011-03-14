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

import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestParser;
import org.jboss.as.cli.operation.parsing.ParsingUtil;

/**
 * Default implementation of CommandParser which expects the following command format:
 *
 * [node-type=node-name (, node-type=node-name)*] : operation-name ['(' name=value (, name=value)* ')' ]
 *
 * the whitespaces are insignificant. E.g.
 *
 * profile=production,subsystem=threads,bounded-queue-thread-pool=pool1:write-core-threads(count=0, per-cpu=20)
 *
 * Each node-type, node-name, operation-name and the argument name as strings are checked to be valid identifiers,
 * i.e. the Character.isJavaIdentifierStart(c) should return true for the first character and the rest should
 * satisfy (Character.isJavaIdentifierPart(c) || c == '-')
 *
 * This implementation is thread-safe. The same instance of this class can be re-used multiple times and
 * can be accessed from multiple threads concurrently w/o synchronization.
 *
 * @author Alexey Loubyansky
 */
public class DefaultOperationRequestParser implements OperationRequestParser {

    public static final String FORMAT = "[node-type=node-name (, node-type=node-name)*] : operation-name [ '(' name=value (, name=value)* ')' ]";
    public static final char NODE_TYPE_NAME_SEPARATOR = '=';
    public static final char NODE_SEPARATOR = '/';
    public static final char ADDRESS_OPERATION_NAME_SEPARATOR = ':';
    public static final char ARGUMENTS_LIST_START = '(';
    public static final char ARGUMENT_NAME_VALUE_SEPARATOR = '=';
    public static final char ARGUMENT_SEPARATOR = ',';
    public static final char ARGUMENTS_LIST_END = ')';
    public static final String ROOT_NODE = "/";
    public static final String PARENT_NODE = "..";
    public static final String NODE_TYPE = ".type";

    @Override
    public void parse(String operationRequest, CallbackHandler handler) throws OperationFormatException {

        if(operationRequest == null || operationRequest.isEmpty()) {
            return;
        }

        int aoSep = operationRequest.indexOf(ADDRESS_OPERATION_NAME_SEPARATOR);

        final int addressLength;
        if(aoSep < 0) {
            addressLength =  operationRequest.length();
        } else if(aoSep > 0){
            addressLength = aoSep;
        } else {
            addressLength = 0;
        }

        if(addressLength > 0) {

            final int nodePathStart;
            if(operationRequest.startsWith("./")) {
                nodePathStart = 2;
                handler.nodeSeparator(1);
            } else{
                nodePathStart = 0;
            }

            int nodeIndex = nodePathStart;
            while (nodeIndex < addressLength) {
                final int nodeSepIndex = operationRequest.indexOf(NODE_SEPARATOR, nodeIndex);
                final String node;
                if (nodeSepIndex < 0) {
                    node = operationRequest.substring(nodeIndex, addressLength).trim();
                } else {
                    node = operationRequest.substring(nodeIndex, nodeSepIndex).trim();
                }

                if (node.isEmpty()) {
                    if(nodeSepIndex > 0) {
                        throw new OperationFormatException(
                                "Node type/name is missing or the format is wrong for the prefix '"
                                        + operationRequest.substring(0, addressLength) + "'");
                    } else if(nodeSepIndex == 0) {
                        handler.rootNode();
                    }
                } else {

                    int typeNameSep = node.indexOf(NODE_TYPE_NAME_SEPARATOR);
                    if (typeNameSep < 0) {
                        /* root node is allowed only at the beginning of the address
                        if (ROOT_NODE.equals(node)) {
                            handler.rootNode();
                        } else*/ if (PARENT_NODE.equals(node)) {
                            handler.parentNode();
                        } else if (NODE_TYPE.equals(node)) {
                            handler.nodeType();
                        } else {
                            if(nodeIndex == nodePathStart) {
                                if(nodeSepIndex < 0) {
                                    handler.nodeTypeOrName(node);
                                } else if(aoSep > 0) {
                                    // operation can't be invoked on a type
                                    handler.nodeName(node);
                                } else {
                                    handler.nodeTypeOrName(node);
                                }
                            } else if (nodeSepIndex < 0) {
                                handler.nodeTypeOrName(node);
                            }
                        }
                    } else {
                        String nodeType = node.substring(0, typeNameSep).trim();
                        handler.nodeType(nodeType);
                        handler.nodeTypeNameSeparator(nodeIndex + typeNameSep);
                        String nodeName = node.substring(typeNameSep + 1).trim();
                        // this is to allow parsing of 'node-type='
                        if (!nodeName.isEmpty()) {
                            handler.nodeName(nodeName);
                        }
                    }
                }

                if (nodeSepIndex < 0) {
                    nodeIndex = addressLength;
                } else {
                    handler.nodeSeparator(nodeSepIndex);
                    nodeIndex = nodeSepIndex + 1;
                }
            }
        }

        if(aoSep < 0) {
            return;
        }

        handler.addressOperationSeparator(aoSep);

        String operationName;
        int argListStartIndex = operationRequest.indexOf(ARGUMENTS_LIST_START, aoSep + 1);
        if(argListStartIndex < 0) {
            if(aoSep + 1 < operationRequest.length()) {
                operationName = operationRequest.substring(aoSep + 1);
                handler.operationName(operationName);
            }
            return;
        }
        else {
            operationName = operationRequest.substring(aoSep + 1, argListStartIndex).trim();
            handler.operationName(operationName);
//            handler.propertyListStart(argListStartIndex);
        }

        ParsingUtil.parseParameters(operationRequest, argListStartIndex, handler);

/*        final boolean argListEndPresent = operationRequest.charAt(operationRequest.length() - 1) == ARGUMENTS_LIST_END;
        final int argsLength;
        if(argListEndPresent) {
           argsLength = operationRequest.length() - 1;
        } else {
            argsLength = operationRequest.length();
        }

        int argIndex = argListStartIndex + 1;
        while (argIndex < argsLength) {
            final int argSepIndex = operationRequest.indexOf(ARGUMENT_SEPARATOR, argIndex);
            final String arg;
            if (argSepIndex == -1) {
                arg = operationRequest.substring(argIndex, argsLength).trim();
            } else {
                arg = operationRequest.substring(argIndex, argSepIndex).trim();
            }

            if (arg.isEmpty()) {
                throw new OperationFormatException(
                        "An argument is missing or the command is in the wrong format: '"
                                + operationRequest + "'");
            }

            int argNameValueSepIndex = arg.indexOf(ARGUMENT_NAME_VALUE_SEPARATOR);
            if (argNameValueSepIndex < 0) {
                handler.propertyName(arg);
            } else {
                String argValue = arg.substring(argNameValueSepIndex + 1).trim();
                if(argValue.isEmpty()) {
                    handler.propertyName(arg.substring(0, argNameValueSepIndex).trim());
                    handler.propertyNameValueSeparator(argNameValueSepIndex);
                } else {
                   handler.property(arg.substring(0, argNameValueSepIndex).trim(), argValue, argNameValueSepIndex);
                }
            }

            if(argSepIndex < 0) {
                argIndex = argsLength;
            } else {
                argIndex = argSepIndex + 1;
                handler.propertySeparator(argSepIndex);
            }
        }

        if(argListEndPresent) {
            handler.propertyListEnd(operationRequest.length() - 1);
        }
*/
    }

}
