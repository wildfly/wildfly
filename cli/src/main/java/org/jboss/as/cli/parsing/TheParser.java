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
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestParser;
import org.jboss.as.cli.operation.parsing.NodeState;
import org.jboss.as.cli.operation.parsing.OperationRequestState;
import org.jboss.as.cli.operation.parsing.OutputTargetState;
import org.jboss.as.cli.operation.parsing.ParsingContext;
import org.jboss.as.cli.operation.parsing.ParsingStateCallbackHandler;
import org.jboss.as.cli.operation.parsing.PropertyListState;
import org.jboss.as.cli.operation.parsing.StateParser;

/**
 *
 * @author Alexey Loubyansky
 */
public class TheParser {

/*    public interface CallbackHandler {

        void start(String operationString);

        void rootNode();

        void parentNode();

        void nodeType();

        void nodeType(String nodeType) throws OperationFormatException;

        void nodeTypeNameSeparator(int index);

        void nodeName(String nodeName) throws OperationFormatException;

        void nodeSeparator(int index);

        void addressOperationSeparator(int index);

        void operationName(String operationName) throws CommandFormatException;

        void propertyListStart(int index);

        void propertyName(String propertyName) throws OperationFormatException;

        void propertyNameValueSeparator(int index);

        void property(String name, String value, int nameValueSeparatorIndex) throws OperationFormatException;

        void propertySeparator(int index);

        void propertyListEnd(int index);

        // TODO this is not good
        void nodeTypeOrName(String typeOrName) throws OperationFormatException;

        void outputTarget(String outputTarget) throws CommandFormatException;
    }
*/
    public static void parse(String commandLine, final OperationRequestParser.CallbackHandler handler) throws CommandFormatException {
        if(commandLine == null || commandLine.isEmpty()) {
            return;
        }
        final ParsingStateCallbackHandler callbackHandler = getCallbackHandler(handler);
        StateParser.parse(commandLine, callbackHandler, InitialState.INSTANCE);
    }

    public static void parseOperationRequest(String commandLine, final OperationRequestParser.CallbackHandler handler) throws CommandFormatException {
        if(commandLine == null || commandLine.isEmpty()) {
            return;
        }
        final ParsingStateCallbackHandler callbackHandler = getCallbackHandler(handler);
        StateParser.parse(commandLine, callbackHandler, OperationRequestState.INSTANCE);
    }

    public static void parseCommandArgs(String commandLine, final OperationRequestParser.CallbackHandler handler) throws CommandFormatException {
        if(commandLine == null || commandLine.isEmpty()) {
            return;
        }
        final ParsingStateCallbackHandler callbackHandler = getCallbackHandler(handler);
        StateParser.parse(commandLine, callbackHandler, ArgumentListState.INSTANCE);
    }

    protected static ParsingStateCallbackHandler getCallbackHandler(final OperationRequestParser.CallbackHandler handler) {

        return new ParsingStateCallbackHandler() {

            private int nameValueSeparator = -1;
            private String name;
            final StringBuilder buffer = new StringBuilder();
            int bufferStartIndex = 0;
            boolean inValue;

            @Override
            public void enteredState(ParsingContext ctx)
                    throws OperationFormatException {
                final String id = ctx.getState().getId();
                //System.out.println("entered " + id);

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
                }
            }

            @Override
            public void leavingState(ParsingContext ctx)
                    throws CommandFormatException {

                final String id = ctx.getState().getId();
                //System.out.println("leaving " + id);

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
                    handler.operationName(bufferStartIndex, buffer.toString().trim());
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
                } else if (OutputTargetState.ID.equals(id)) {
                    handler.outputTarget(bufferStartIndex, buffer.toString().trim());
                    buffer.setLength(0);
                }
            }

            @Override
            public void character(ParsingContext ctx) throws OperationFormatException {
                //System.out.println(ctx.getState().getId() + " '" + ctx.getCharacter() + "'");
                buffer.append(ctx.getCharacter());
            }
        };
    }

    public static void main(String[] args) throws Exception {

        //final String line = "cmd ../../../../my\\ dir/ > ../../../../my\\ dir/cli.log";
        //final String line = "/a=b/../c=d/.type/e:op(p1=v1,p2=v2)";
        //final String line = "cmd --p1=v1 --p2=v2 --p3 v3";
        final String line = "/subsystem=datasources/data-source=java\\:jboss\\/datasources\\/ExampleDS";
        System.out.println(line);
        TheParser.parse(line,
                new OperationRequestParser.CallbackHandler(){
                    @Override
                    public void property(String name, String value, int separator) {
                        StringBuilder buf = new StringBuilder();
                        if(name == null) {
                            buf.append('\'').append(value).append('\'');
                        } else if(value == null) {
                            buf.append('\'').append(name).append('\'');
                        } else {
                            buf.append('\'').append(name).append("'='").append(value).append('\'');
                        }
                        System.out.println(buf.toString());
                    }

                    @Override
                    public void operationName(int index, String name) throws CommandFormatException {
                        System.out.println("command: '" + name + "'");
                    }

                    @Override
                    public void outputTarget(int index, String outputTarget) throws CommandFormatException {
                        System.out.println("output: '" + outputTarget + "'");
                    }

                    @Override
                    public void start(String operationString) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void rootNode(int index) {
                        System.out.println("rootNode " + index);
                    }

                    @Override
                    public void parentNode(int index) {
                        System.out.println("parentNode");
                    }

                    @Override
                    public void nodeType(int index) {
                        System.out.println("nodeType");
                    }

                    @Override
                    public void nodeType(int index, String nodeType)
                            throws OperationFormatException {
                        System.out.println("nodeType: '" + nodeType + "'");
                    }

                    @Override
                    public void nodeTypeNameSeparator(int index) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void nodeName(int index, String nodeName)
                            throws OperationFormatException {
                        System.out.println("nodeName: '" + nodeName + "'");
                    }

                    @Override
                    public void nodeSeparator(int index) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void addressOperationSeparator(int index) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void propertyListStart(int index) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void propertyName(int index, String propertyName)
                            throws OperationFormatException {
                        // TODO Auto-generated method stub
                        System.out.println("propertyName: " + propertyName);
                    }

                    @Override
                    public void propertyNameValueSeparator(int index) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void propertySeparator(int index) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void propertyListEnd(int index) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void nodeTypeOrName(int index, String typeOrName)
                            throws OperationFormatException {
                        // TODO Auto-generated method stub

                    }});
    }
}
