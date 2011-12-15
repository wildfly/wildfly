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
package org.jboss.as.cli.parsing;


import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.parsing.command.ArgumentListState;
import org.jboss.as.cli.parsing.command.ArgumentState;
import org.jboss.as.cli.parsing.command.ArgumentValueState;
import org.jboss.as.cli.parsing.command.CommandFormat;
import org.jboss.as.cli.parsing.command.CommandNameState;
import org.jboss.as.cli.parsing.command.CommandState;
import org.jboss.as.cli.parsing.operation.HeaderListState;
import org.jboss.as.cli.parsing.operation.HeaderNameState;
import org.jboss.as.cli.parsing.operation.HeaderState;
import org.jboss.as.cli.parsing.operation.HeaderValueState;
import org.jboss.as.cli.parsing.operation.NodeState;
import org.jboss.as.cli.parsing.operation.OperationFormat;
import org.jboss.as.cli.parsing.operation.OperationRequestState;
import org.jboss.as.cli.parsing.operation.PropertyListState;

/**
 *
 * @author Alexey Loubyansky
 */
public class ParserUtil {

    public static void parse(String commandLine, final CommandLineParser.CallbackHandler handler) throws CommandFormatException {
        if(commandLine == null) {
            return;
        }
        final ParsingStateCallbackHandler callbackHandler = getCallbackHandler(handler);
        StateParser.parse(commandLine, callbackHandler, InitialState.INSTANCE);
    }

    public static void parseOperationRequest(String commandLine, final CommandLineParser.CallbackHandler handler) throws CommandFormatException {
        if(commandLine == null) {
            return;
        }
        final ParsingStateCallbackHandler callbackHandler = getCallbackHandler(handler);
        StateParser.parse(commandLine, callbackHandler, OperationRequestState.INSTANCE);
    }

    public static void parseCommandArgs(String commandLine, final CommandLineParser.CallbackHandler handler) throws CommandFormatException {
        if(commandLine == null) {
            return;
        }
        final ParsingStateCallbackHandler callbackHandler = getCallbackHandler(handler);
        StateParser.parse(commandLine, callbackHandler, ArgumentListState.INSTANCE);
    }

    public static void parse(String str, final CommandLineParser.CallbackHandler handler, ParsingState initialState) throws CommandFormatException {
        if(str == null) {
            return;
        }
        final ParsingStateCallbackHandler callbackHandler = getCallbackHandler(handler);
        StateParser.parse(str, callbackHandler, initialState);
    }

    protected static ParsingStateCallbackHandler getCallbackHandler(final CommandLineParser.CallbackHandler handler) {

        return new ParsingStateCallbackHandler() {

            private int nameValueSeparator = -1;
            private String name;
            final StringBuilder buffer = new StringBuilder();
            int bufferStartIndex = 0;
            boolean inValue;

            String delegateStateId;
            ParsingStateCallbackHandler delegate;

            @Override
            public void enteredState(ParsingContext ctx) throws CommandFormatException {

                final String id = ctx.getState().getId();
                //System.out.println("entered " + id + " " + ctx.getCharacter());

                if(delegate != null) {
                    delegate.enteredState(ctx);
                    return;
                }

                if(!inValue) {
                    bufferStartIndex = ctx.getLocation();
                }

                if (id.equals(PropertyListState.ID)) {
                    handler.propertyListStart(ctx.getLocation());
                } else if (ArgumentValueState.ID.equals(id)) {
                    inValue = true;
                } else if ("ADDR_OP_SEP".equals(id)) {
                    handler.addressOperationSeparator(ctx.getLocation());
                } else if (NodeState.ID.equals(id)) {
                    inValue = true;
                } else if ("NAME_VALUE_SEPARATOR".equals(id)) {
                    nameValueSeparator = ctx.getLocation();
                    if (buffer.length() > 0) {
                        name = buffer.toString().trim();
                        buffer.setLength(0);
                    }
                } else if(id.equals(CommandState.ID)) {
                    handler.setFormat(CommandFormat.INSTANCE);
                } else if(id.equals(OperationRequestState.ID)) {
                    handler.setFormat(OperationFormat.INSTANCE);
                } else if (HeaderListState.ID.equals(id)) {
                    handler.headerListStart(ctx.getLocation());
                } else if (HeaderValueState.ID.equals(id)) {
                    inValue = true;
                }
            }

            @Override
            public void leavingState(ParsingContext ctx) throws CommandFormatException {

                final String id = ctx.getState().getId();
                //System.out.println("leaving " + id + " " + ctx.getCharacter());

                if(delegateStateId != null && !id.equals(delegateStateId)) {
                    delegate.leavingState(ctx);
                    return;
                }

                if (id.equals(PropertyListState.ID)) {
                    if (!ctx.isEndOfContent()) {
                        handler.propertyListEnd(ctx.getLocation());
                    }
                } else if (ArgumentState.ID.equals(id)) {
                    if (this.name != null) {
                        final String value = buffer.toString().trim();
                        if (value.length() > 0) {
                            handler.property(this.name, value, bufferStartIndex/*nameValueSeparator*/);
                        } else {
                            handler.propertyName(bufferStartIndex, this.name);
                            if (nameValueSeparator != -1) {
                                handler.propertyNameValueSeparator(nameValueSeparator);
                            }
                        }
                    } else {
                        handler.propertyName(bufferStartIndex, buffer.toString().trim());
                        if (nameValueSeparator != -1) {
                            handler.propertyNameValueSeparator(nameValueSeparator);
                        }
                    }
//                    if (ctx.getCharacter() == ',') {
//                        handler.propertySeparator(ctx.getLocation());
//                    } TODO this is not really an equivalent
                    if(!ctx.isEndOfContent()) {
                        handler.propertySeparator(ctx.getLocation());
                    }

                    buffer.setLength(0);
                    name = null;
                    nameValueSeparator = -1;
                } else if (ArgumentValueState.ID.equals(id)) {
                    if (name == null) {
                        handler.property(null, buffer.toString().trim(), bufferStartIndex);
                        buffer.setLength(0);
                        if(!ctx.isEndOfContent()) {
                            handler.propertySeparator(ctx.getLocation());
                        }
                    }
                    inValue = false;
                } else if (CommandNameState.ID.equals(id)) {
                    final String opName = buffer.toString().trim();
                    if(!opName.isEmpty()) {
                        handler.operationName(bufferStartIndex, opName);
                    }
                    buffer.setLength(0);
                } else if (NodeState.ID.equals(id)) {
                    char ch = ctx.getCharacter();
                    if (buffer.length() == 0) {
                        if (ch == '/') {
                            handler.rootNode(bufferStartIndex);
                            handler.nodeSeparator(ctx.getLocation());
                        }
                    } else {
                        final String value = buffer.toString().trim();
                        if (ch == '=') {
                            handler.nodeType(bufferStartIndex, value);
                            handler.nodeTypeNameSeparator(ctx.getLocation());
                        } else if (ch == ':') {
                            handler.nodeName(bufferStartIndex, value);
                        } else {
                            if (".".equals(value)) {
                                // stay at the current address
                            } else if ("..".equals(value)) {
                                handler.parentNode(ctx.getLocation() - 2);
                            } else if (".type".equals(value)) {
                                handler.nodeType(ctx.getLocation() - 5);
                            } else {
                                if (ch == '/') {
                                    if ("".equals(value)) {
                                        handler.rootNode(ctx.getLocation());
                                    } else {
                                        handler.nodeName(bufferStartIndex, value);
                                    }
                                } else {
                                    handler.nodeTypeOrName(bufferStartIndex, value);
                                }
                            }

                            if (ch == '/') {
                                handler.nodeSeparator(ctx.getLocation());
                            }
                        }
                    }
                    buffer.setLength(0);
                    inValue = false;
                } else if (HeaderListState.ID.equals(id)) {
                    if (ctx.getCharacter() == '}') {
                        handler.headerListEnd(ctx.getLocation());
                    }
                } else if (HeaderNameState.ID.equals(id)) {
                    final String headerName = buffer.toString().trim();
                    if(!headerName.isEmpty()) {
                        this.name = headerName;
                        delegate = handler.headerName(bufferStartIndex, headerName);
                        if(delegate != null) {
                            delegateStateId = HeaderState.ID;
                        }
                    }
                    buffer.setLength(0);
                } else if (HeaderValueState.ID.equals(id)) {
                    handler.header(name, buffer.toString().trim(), bufferStartIndex);
                    buffer.setLength(0);
//                    if(!ctx.isEndOfContent()) {
//                        handler.propertySeparator(ctx.getLocation());
//                    }
                    inValue = false;
                } else if (HeaderState.ID.equals(id)) {
                    this.name = null;
                    delegate = null;
                    delegateStateId = null;
                } else if (OutputTargetState.ID.equals(id)) {
                    handler.outputTarget(bufferStartIndex, buffer.toString().trim());
                    buffer.setLength(0);
                }
            }

            @Override
            public void character(ParsingContext ctx) throws CommandFormatException {
                if(delegate != null) {
                    delegate.character(ctx);
                    return;
                }
                //System.out.println(ctx.getState().getId() + " '" + ctx.getCharacter() + "'");
                buffer.append(ctx.getCharacter());
            }
        };
    }
}
