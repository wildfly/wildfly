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

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestParser;
import org.jboss.as.cli.operation.parsing.NodeState;
import org.jboss.as.cli.operation.parsing.OperationNameState;
import org.jboss.as.cli.operation.parsing.OperationRequestState;
import org.jboss.as.cli.operation.parsing.OperationState;
import org.jboss.as.cli.operation.parsing.ParsingContext;
import org.jboss.as.cli.operation.parsing.ParsingStateCallbackHandler;
import org.jboss.as.cli.operation.parsing.PropertyListState;
import org.jboss.as.cli.operation.parsing.PropertyState;
import org.jboss.as.cli.operation.parsing.PropertyValueState;
import org.jboss.as.cli.operation.parsing.StateParser;

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

    public static final OperationRequestParser INSTANCE = new DefaultOperationRequestParser();

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
    public void parse(String operationRequest, final CallbackHandler handler) throws OperationFormatException {

        if(operationRequest == null || operationRequest.isEmpty()) {
            return;
        }

        ParsingStateCallbackHandler stateCallbackHandler = new ParsingStateCallbackHandler() {

            StringBuilder buffer = new StringBuilder();

            int propNameValueSep = -1;
            StringBuilder propName;
            boolean propValueContent;

            @Override
            public void enteredState(ParsingContext ctx) throws OperationFormatException {

                String stateId = ctx.getState().getId();
                //System.out.println("entered " + stateId + " '" + ctx.getCharacter() + "'");

                if(stateId.equals(OperationState.ID)) {
                    handler.addressOperationSeparator(ctx.getLocation());
                } else if (stateId.equals(PropertyListState.ID)) {
                    handler.propertyListStart(ctx.getLocation());
                } else if(stateId.equals(PropertyState.ID)) {
                    propName = new StringBuilder();
                } else if(stateId.equals(PropertyValueState.ID)) {
                    propNameValueSep = ctx.getLocation();
                    propValueContent = true;
                    buffer.setLength(0);
                } else if(stateId.equals(NodeState.ID)) {
                    propValueContent = true;
                    buffer.setLength(0);
                }

                if(!propValueContent) {
                   buffer.setLength(0);
                }
            }

            @Override
            public void leavingState(ParsingContext ctx) throws OperationFormatException {

                String stateId = ctx.getState().getId();
                //System.out.println("leaving " + stateId + " '" + ctx.getCharacter() + "'");

                if (stateId.equals(PropertyListState.ID)) {
                    if(ctx.getCharacter() == ')') {
                        handler.propertyListEnd(ctx.getLocation());
                    }
                } else if(stateId.equals(PropertyState.ID)) {
                    if(buffer.length() > 0) {
                        handler.property(propName.toString().trim(), buffer.toString().trim(), propNameValueSep);
                    } else {
                        handler.propertyName(propName.toString().trim());
                        if(propNameValueSep != -1) {
                            handler.propertyNameValueSeparator(propNameValueSep);
                        }
                    }

                    if(ctx.getCharacter() == ',') {
                        handler.propertySeparator(ctx.getLocation());
                    }

                    propName = null;
                    propNameValueSep = -1;
                    propValueContent = false;
                } else if(stateId.equals(OperationNameState.ID)) {
                    handler.operationName(buffer.toString().trim());
                } else if(stateId.equals(NodeState.ID)) {
                    char ch = ctx.getCharacter();
                    if(buffer.length() == 0) {
                        if(ch == '/') {
                            handler.rootNode();
                            handler.nodeSeparator(ctx.getLocation());
                        }
                    } else {
                        if (ch == '=') {
                            handler.nodeType(buffer.toString().trim());
                            handler.nodeTypeNameSeparator(ctx.getLocation());
                        } else if (ch == ':') {
                            handler.nodeName(buffer.toString().trim());
                        } else {
                            final String value = buffer.toString().trim();
                            if (".".equals(value)) {
                                // stay at the current address
                            } else if ("..".equals(value)) {
                                handler.parentNode();
                            } else if (".type".equals(value)) {
                                handler.nodeType();
                            } else {
                                if(ch == '/') {
                                    if ("".equals(value)) {
                                        handler.rootNode();
                                    } else {
                                        handler.nodeName(value);
                                    }
                                } else {
                                    handler.nodeTypeOrName(value);
                                }
                            }

                            if(ch == '/') {
                                handler.nodeSeparator(ctx.getLocation());
                            }
                        }
                    }
                    propValueContent = false;
                }
            }

            @Override
            public void character(ParsingContext ctx)
                    throws OperationFormatException {

                //System.out.println(ctx.getState().getId() + " '" + ctx.getCharacter() + "'");

                String stateId = ctx.getState().getId();
                if (stateId.equals(PropertyState.ID)) {
                    propName.append(ctx.getCharacter());
                } else {
                    buffer.append(ctx.getCharacter());
                }
            }
        };

        try {
            StateParser.parse(operationRequest, stateCallbackHandler, OperationRequestState.INSTANCE);
        } catch (CommandFormatException e) {
            throw new OperationFormatException("Failed to parse operation request '" + operationRequest + "'", e);
        }
    }

}
