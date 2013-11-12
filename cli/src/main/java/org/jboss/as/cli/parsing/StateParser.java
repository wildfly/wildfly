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

        try {
            doParse(str, callbackHandler, initialState, strict);
        } catch(CommandFormatException e) {
            throw e;
        } catch(Throwable t) {
            throw new CommandFormatException("Failed to parse '" + str + "'", t);
        }
    }

    protected static void doParse(String str, ParsingStateCallbackHandler callbackHandler, ParsingState initialState,
            boolean strict) throws CommandFormatException {
        if (str == null || str.isEmpty()) {
            return;
        }

        ParsingContextImpl ctx = new ParsingContextImpl();
        ctx.initialState = initialState;
        ctx.callbackHandler = callbackHandler;
        ctx.input = str;
        ctx.strict = strict;

        ctx.ch = str.charAt(0);
        ctx.location = 0;
        initialState.getEnterHandler().handle(ctx);

        ctx.parse();

        ParsingState state = ctx.getState();
        while(state != ctx.initialState) {
            state.getEndContentHandler().handle(ctx);
            ctx.leaveState();
            state = ctx.getState();
        }
        initialState.getEndContentHandler().handle(ctx);
        initialState.getLeaveHandler().handle(ctx);
    }

    static class ParsingContextImpl implements ParsingContext {

        private final Deque<ParsingState> stack = new ArrayDeque<ParsingState>();

        String input;
        int location;
        char ch;
        ParsingStateCallbackHandler callbackHandler;
        ParsingState initialState;
        boolean strict;
        CommandFormatException error;

        void parse() throws CommandFormatException {
            while (location < input.length()) {
                ch = input.charAt(location);
                final CharacterHandler handler = getState().getHandler(ch);
                handler.handle(this);
                ++location;
            }
        }

        @Override
        public boolean begins(String seq) {
            if(location + seq.length() < input.length()) {
                int i = 0;
                while(i < seq.length()) {
                    if(input.charAt(location + i) != seq.charAt(i++)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public void replaceProperty(boolean exceptionIfNotResolved) throws CommandFormatException {
            if(input.charAt(location) == '$' &&
                    input.length() > location + 3 && // there must be opening {, closing } and something in the middle
                    input.charAt(location + 1) == '{') {
                final int end = input.indexOf('}', location + 2);
                if(end == -1) {
                    return;
                }
                final String prop = input.substring(location, end + 1);
                final String resolved = Util.resolveProperties(prop);
                if (!resolved.equals(prop)) {
                    StringBuilder buf = new StringBuilder(input.length() - prop.length() + resolved.length());
                    buf.append(input.substring(0, location)).append(resolved);
                    if (end < input.length() - 1) {
                        buf.append(input.substring(end + 1));
                    }
                    input = buf.toString();
                    --location;
                } else if(exceptionIfNotResolved) {
                    throw new CommandFormatException("Couldn't resolve property " + prop + " in '" + input + "'");
                }
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
            return location;
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
            return input;
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
