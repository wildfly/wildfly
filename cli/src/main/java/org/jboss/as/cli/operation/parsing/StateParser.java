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
package org.jboss.as.cli.operation.parsing;

import java.util.ArrayDeque;
import java.util.Deque;

import org.jboss.as.cli.CommandFormatException;

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

        if (str == null || str.isEmpty()) {
            return;
        }

        int i = 0;
        while(i < str.length()) {
            if(!Character.isWhitespace(str.charAt(i))) {
                break;
            }
            ++i;
        }

        if(i == str.length()) {
            return;
        }

        ParsingContextImpl ctx = new ParsingContextImpl();
        ctx.initialState = initialState;
        ctx.callbackHandler = callbackHandler;

        while (i < str.length()) {
            char ch = str.charAt(i);

            ctx.ch = ch;
            ctx.location = i;

            CharacterHandler handler = ctx.getState().getHandler(ch);
            handler.handle(ctx);
            ++i;
        }

        ctx.location = i;
        ParsingState state = ctx.getState();
        while(state != ctx.initialState) {
            state.getEndContentHandler().handle(ctx);
            ctx.leaveState();
            state = ctx.getState();
        }
    }

    static class ParsingContextImpl implements ParsingContext {

        private final Deque<ParsingState> stack = new ArrayDeque<ParsingState>();

        int location;
        char ch;
        ParsingStateCallbackHandler callbackHandler;
        ParsingState initialState;

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
            callbackHandler.leavingState(this);
            stack.peek().getLeaveHandler().handle(this);
            ParsingState pop = stack.pop();
            if(!stack.isEmpty()) {
                stack.peek().getReturnHandler().handle(this);
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
    }
}
