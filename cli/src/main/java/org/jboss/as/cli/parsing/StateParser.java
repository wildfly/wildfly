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

import java.util.ArrayDeque;
import java.util.Deque;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;

/**
 *
 * @author Alexey Loubyansky
 */
public class StateParser {

    private final DefaultParsingState initialState = new DefaultParsingState("INITIAL");

    public void addState(char ch, ParsingState state) {
        initialState.enterState(ch, state);
    }

    public void parse(String str, ParsingStateCallbackHandler callbackHandler) throws CommandFormatException {
        parse(str, callbackHandler, initialState);
    }

    public static void parse(String str, ParsingStateCallbackHandler callbackHandler, ParsingState initialState) throws CommandFormatException {
        parse(str, callbackHandler, initialState, true);
    }

    public static void parse(String str, ParsingStateCallbackHandler callbackHandler, ParsingState initialState, boolean strict) throws CommandFormatException {
        parse(str, callbackHandler, initialState, strict, null);
    }

    public static void parse(String str, ParsingStateCallbackHandler callbackHandler, ParsingState initialState,
            boolean strict, CommandContext ctx) throws CommandFormatException {

        try {
            doParse(str, callbackHandler, initialState, strict, ctx);
        } catch(CommandFormatException e) {
            throw e;
        } catch(Throwable t) {
            throw new CommandFormatException("Failed to parse '" + str + "'", t);
        }
    }

    protected static void doParse(String str, ParsingStateCallbackHandler callbackHandler, ParsingState initialState,
            boolean strict) throws CommandFormatException {
        doParse(str, callbackHandler, initialState, strict, null);
    }

    protected static void doParse(String str, ParsingStateCallbackHandler callbackHandler, ParsingState initialState,
            boolean strict, CommandContext cmdCtx) throws CommandFormatException {

        if (str == null || str.isEmpty()) {
            return;
        }

        ParsingContextImpl ctx = new ParsingContextImpl();
        ctx.initialState = initialState;
        ctx.callbackHandler = callbackHandler;
        ctx.input = str;
        ctx.strict = strict;
        ctx.cmdCtx = cmdCtx;

        ctx.parse();
    }

    static class ParsingContextImpl implements ParsingContext {

        private final Deque<ParsingState> stack = new ArrayDeque<ParsingState>();

        String input;
        int variableCorrection;
        String originalInput;
        int location;
        char ch;
        ParsingStateCallbackHandler callbackHandler;
        ParsingState initialState;
        boolean strict;
        CommandFormatException error;
        CommandContext cmdCtx;

        void parse() throws CommandFormatException {

            ch = input.charAt(0);
            originalInput = input;
            location = 0;
            variableCorrection = 0;

            initialState.getEnterHandler().handle(this);

            while (location < input.length()) {
                ch = input.charAt(location);
                final CharacterHandler handler = getState().getHandler(ch);
                handler.handle(this);
                ++location;
            }

            ParsingState state = getState();
            while(state != initialState) {
                state.getEndContentHandler().handle(this);
                leaveState();
                state = getState();
            }
            initialState.getEndContentHandler().handle(this);
            initialState.getLeaveHandler().handle(this);

        }

        @Override
        public void resolveExpression(boolean systemProperty, boolean exceptionIfNotResolved)
            throws UnresolvedExpressionException {
            if(input.charAt(location) != '$') {
                return;
            }

            final int inputLength = input.length();
            if(inputLength - 1 > location && input.charAt(location + 1) == '{') {
                if(!systemProperty) {
                    return;
                }
                final int endBrace = input.indexOf('}', location + 1);
                if(endBrace - location - 2 <= 0) {
                    return;
                }
                final String prop = input.substring(location, endBrace + 1);
                final String resolved = Util.resolveProperties(prop);
                if (!resolved.equals(prop)) {
                    StringBuilder buf = new StringBuilder(input.length() - prop.length() + resolved.length());
                    buf.append(input.substring(0, location)).append(resolved);
                    if (endBrace < input.length() - 1) {
                        buf.append(input.substring(endBrace + 1));
                    }
                    variableCorrection += resolved.length() - prop.length();
                    input = buf.toString();
                    ch = input.charAt(location);
                    return;
                } else if(exceptionIfNotResolved) {
                    throw new UnresolvedExpressionException(prop);
                }
            }

            // TODO resolve variables
            int endIndex = location + 1;
            if(endIndex >= input.length() || !Character.isJavaIdentifierStart(input.charAt(endIndex))) {
                // simply '$'
                return;
            }
            while(++endIndex < input.length()) {
                if(!Character.isJavaIdentifierPart(input.charAt(endIndex))) {
                    break;
                }
            }

            final String name = input.substring(location+1, endIndex);
            final String value = cmdCtx == null ? null : cmdCtx.getVariable(name);
            if(value == null) {
                if (exceptionIfNotResolved) {
                    throw new UnresolvedExpressionException(name);
                }
            } else {
                StringBuilder buf = new StringBuilder(input.length() - name.length() + value.length());
                buf.append(input.substring(0, location)).append(value);
                if (endIndex < input.length()) {
                    buf.append(input.substring(endIndex));
                }
                variableCorrection += value.length() - name.length() - 1;
                input = buf.toString();
                ch = input.charAt(location);
            }
        }

        @Override
        public boolean isStrict() {
            return strict;
        }

        @Override
        public ParsingState getState() {
            return stack.isEmpty() ? initialState : stack.peek();
        }

        @Override
        public void enterState(ParsingState state) throws CommandFormatException {
            stack.push(state);
            callbackHandler.enteredState(this);
            state.getEnterHandler().handle(this);
        }

        @Override
        public ParsingState leaveState() throws CommandFormatException {
            stack.peek().getLeaveHandler().handle(this);
            callbackHandler.leavingState(this);
            ParsingState pop = stack.pop();
            if(!stack.isEmpty()) {
                stack.peek().getReturnHandler().handle(this);
            } else {
                initialState.getReturnHandler().handle(this);
            }
            return pop;
        }

        @Override
        public ParsingStateCallbackHandler getCallbackHandler() {
            return callbackHandler;
        }

        @Override
        public char getCharacter() {
            return ch;
        }

        @Override
        public int getLocation() {
            return location - variableCorrection;
        }

        @Override
        public void reenterState() throws CommandFormatException {
            callbackHandler.leavingState(this);
            ParsingState state = stack.peek();
            state.getLeaveHandler().handle(this);
            callbackHandler.enteredState(this);
            state.getEnterHandler().handle(this);
        }

        @Override
        public boolean isEndOfContent() {
            return location >= input.length();
        }

        @Override
        public String getInput() {
            return originalInput;
        }

        @Override
        public void advanceLocation(int offset) throws IndexOutOfBoundsException {
//            if(location + offset >= input.length()) {
//                throw new IndexOutOfBoundsException("Location=" + location + ", offset=" + offset + ", length=" + input.length());
//            }
            if(isEndOfContent()) {
                throw new IndexOutOfBoundsException("Location=" + location + ", offset=" + offset + ", length=" + input.length());
            }

            location += offset;
            if(location < input.length()) {
                ch = input.charAt(location);
            }
        }

        @Override
        public CommandFormatException getError() {
            return error;
        }

        @Override
        public void setError(CommandFormatException e) {
            if(error == null) {
                error = e;
            }
        }
    }
}
