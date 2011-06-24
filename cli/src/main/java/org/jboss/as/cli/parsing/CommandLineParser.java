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
import org.jboss.as.cli.operation.parsing.ParsingContext;
import org.jboss.as.cli.operation.parsing.ParsingStateCallbackHandler;
import org.jboss.as.cli.operation.parsing.StateParser;

/**
 *
 * @author Alexey Loubyansky
 */
/**
 *
 * @author Alexey Loubyansky
 */
public class CommandLineParser {

    public interface CallbackHandler {
        void argument(String name, int nameStart, String value, int valueStart, int end) throws CommandFormatException;
    }

    public static void parse(String commandLine, final CallbackHandler argHandler) throws CommandFormatException {

        if(commandLine == null || commandLine.isEmpty()) {
            return;
        }

        final ParsingStateCallbackHandler callbackHandler = new ParsingStateCallbackHandler() {

            private String name;
            private int nameStart = -1;
            final StringBuilder buffer = new StringBuilder();
            private int valueStart = -1;

            @Override
            public void enteredState(ParsingContext ctx) throws OperationFormatException {
                //System.out.println("entered " + ctx.getState().getId());
                final String id = ctx.getState().getId();

                if(ArgumentState.ID.equals(id)) {
                    nameStart = ctx.getLocation();
                }
                else if(ArgumentValueState.ID.equals(id)) {
                    if(buffer.length() > 0) {
                        name = buffer.toString();
                        buffer.setLength(0);
                        valueStart = ctx.getLocation() + 1;
                    } else {
                        valueStart = ctx.getLocation();
                    }
                }
            }

            @Override
            public void leavingState(ParsingContext ctx) throws CommandFormatException {
                //System.out.println("leaving " + ctx.getState().getId());

                final String id = ctx.getState().getId();
                if(ArgumentState.ID.equals(id)) {
                    if(buffer.length() > 0) {
                        final int endIndex = ctx.getLocation();
                        argHandler.argument(buffer.toString(), nameStart, null, -1, endIndex);
                    }
                    buffer.setLength(0);
                    name = null;
                    nameStart = -1;
                } else if(ArgumentValueState.ID.equals(id)) {
                    final int endIndex = ctx.getLocation();
                    argHandler.argument(name, nameStart, buffer.toString(), valueStart, endIndex);
                    buffer.setLength(0);
                    valueStart = -1;
                }
            }

            @Override
            public void character(ParsingContext ctx) throws OperationFormatException {
                //System.out.println(ctx.getState().getId() + " '" + ctx.getCharacter() + "'");
                buffer.append(ctx.getCharacter());
            }};

        StateParser.parse(commandLine, callbackHandler, ArgumentListState.INSTANCE);
    }

    public static void main(String[] args) throws Exception {

        //final String line = "   ../../\" my dir \"/test-deployment.sar  --name=my.sar --force --server-groups=group1,group2 value   ";
        final String line = "../../../../my\\ dir/";
        //final String line = "--arg=";
        System.out.println(line);
        CommandLineParser.parse(line,
                new CallbackHandler(){
                    @Override
                    public void argument(String name, int nameStart, String value, int valueStart, int end) {
                        StringBuilder buf = new StringBuilder();
                        buf.append("arg[").append(nameStart).append('/').append(valueStart).append('-').append(end).append("]: ");
                        if(name == null) {
                            buf.append('\'').append(line.substring(valueStart, end)).append('\'');
                        } else if(value == null) {
                            buf.append('\'').append(line.substring(nameStart, end)).append('\'');
                        } else {
                            buf.append('\'').append(line.substring(nameStart, valueStart))/*.append("'='")*/.append(line.substring(valueStart, end)).append('\'');
                        }
                        System.out.println(buf.toString());
                    }});
    }
}
