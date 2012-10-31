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
package org.jboss.as.cli.parsing.arguments;

import java.util.ArrayDeque;
import java.util.Deque;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.parsing.EscapeCharacterState;
import org.jboss.as.cli.parsing.ParsingContext;
import org.jboss.as.cli.parsing.ParsingStateCallbackHandler;
import org.jboss.as.cli.parsing.QuotesState;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author Alexey Loubyansky
 */
public class ArgumentValueCallbackHandler implements ParsingStateCallbackHandler {

    private static byte QUOTES = 1;
    private static byte ESCAPE = 2;

    private Deque<ValueState> stack;
    private ValueState currentState;
    private byte flag;

    /* (non-Javadoc)
     * @see org.jboss.as.cli.parsing.ParsingStateCallbackHandler#enteredState(org.jboss.as.cli.parsing.ParsingContext)
     */
    @Override
    public void enteredState(ParsingContext ctx) throws CommandFormatException {
        final String stateId = ctx.getState().getId();
        //System.out.println("entered " + stateId + " '" + ctx.getCharacter() + "'");

        if(ArgumentValueState.ID.equals(stateId)) {
            if(currentState != null) {
                if (stack == null) {
                    stack = new ArrayDeque<ValueState>();
                }
                stack.push(currentState);
                if(ctx.getCharacter() == '{') {
                    currentState = new DefaultValueState(false, true);
                } else {
                    currentState = new DefaultValueState(currentState.isList());
                }
            } else if(ctx.getCharacter() == '{') {
                currentState = new DefaultValueState(false, true);
            } else {
                currentState = new DefaultValueState(false);
            }
        } else if(ListState.ID.equals(stateId)) {
            if(currentState != null) {
                if(stack == null) {
                    stack = new ArrayDeque<ValueState>();
                }
                stack.push(currentState);
            }
            currentState = new ListValueState();
        } else if(ListItemSeparatorState.ID.equals(stateId)) {
            currentState.itemSeparator();
        } else if(NameValueSeparatorState.ID.equals(stateId)) {
            currentState.nameSeparator(ctx);
        } else if(QuotesState.ID.equals(stateId)) {
            flag ^= QUOTES;
        } else if(EscapeCharacterState.ID.equals(stateId)) {
            flag ^= ESCAPE;
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.parsing.ParsingStateCallbackHandler#leavingState(org.jboss.as.cli.parsing.ParsingContext)
     */
    @Override
    public void leavingState(ParsingContext ctx) throws CommandFormatException {
        final String stateId = ctx.getState().getId();
        //System.out.println("left " + stateId + " '" + ctx.getCharacter() + "'");

        if(ArgumentValueState.ID.equals(stateId) || ListState.ID.equals(stateId)) {
            currentState.complete();
            if(stack != null && stack.peek() != null) {
                stack.peek().addChild(currentState);
                currentState = stack.pop();
            }
        } else if(QuotesState.ID.equals(stateId)) {
            flag ^= QUOTES;
        } else if(EscapeCharacterState.ID.equals(stateId)) {
            flag ^= ESCAPE;
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.parsing.ParsingStateCallbackHandler#character(org.jboss.as.cli.parsing.ParsingContext)
     */
    @Override
    public void character(ParsingContext ctx) throws CommandFormatException {
        //System.out.println("char " + ctx.getState().getId() + " '" + ctx.getCharacter() + "'");
        currentState.character(ctx.getCharacter());
    }

    public ModelNode getResult() {
        return currentState.getValue();
    }

    interface ValueState {

        void addChild(ValueState child);

        String getName();

        void nameSeparator(ParsingContext ctx) throws CommandFormatException;

        void itemSeparator() throws CommandFormatException;

        void complete() throws CommandFormatException;

        void character(char ch);

        ModelNode getValue();

        boolean isList();
    }

    class DefaultValueState implements ValueState {

        private ModelNode wrapper;
        private boolean list;

        protected String name;
        protected StringBuilder buf;
        protected int trimToSize = -1;
        protected boolean dontQuote;

        public DefaultValueState(boolean list) {
            this(list, false);
        }

        public DefaultValueState(boolean list, boolean initWrapper) {
            this.list = list;
            if(initWrapper) {
                wrapper = new ModelNode();
            }
        }

        @Override
        public String getName() {
            return name;
        }
        @Override
        public void nameSeparator(ParsingContext ctx) throws CommandFormatException {
            if(buf == null) {
                throw new CommandFormatException("Property name is null.");
            }
            if(name != null) {
                // the equals sign is a part of the content
                buf.append(ctx.getCharacter());
            } else {
                name = getTrimmedString();
                buf.setLength(0);
            }
        }
        @Override
        public void character(char ch) {
            if(buf == null) {
                buf = new StringBuilder();
            }
            if((byte)(flag & ESCAPE) > 0) {
                buf.append(ch);
            } else
            // trim whitespaces unless in quotes
            if((byte)(flag & QUOTES) > 0) {
                if(ch == '"') {
                    if(buf.length() == 0) {
                        dontQuote = true;
                    } else if(!dontQuote) {
                        buf.append(ch);
                    }
                } else {
                    buf.append(ch);
                }
            } else if(!Character.isWhitespace(ch)) {
                buf.append(ch);
                if(trimToSize >= 0) {
                    trimToSize = -1;
                }
            } else if(buf.length() > 0) {
                if(trimToSize < 0) {
                    trimToSize = buf.length();
                }
                buf.append(ch);
            }
        }
        @Override
        public void itemSeparator() {
            if(buf.length() == 0) {
                return;
            }
            if(wrapper == null) {
                wrapper = new ModelNode();
            }
            if(name == null) {
                wrapper.add(getStringValue());
            } else {
                addChild(wrapper, name, getStringValue());
                name = null;
            }
            buf.setLength(0);
        }
        @Override
        public void complete() throws CommandFormatException{
            if(wrapper != null) {
                if(name == null) {
                    if(buf != null && buf.length() > 0) {
                        if(list || wrapper.getType().equals(ModelType.LIST)) {
                            wrapper.add(getStringValue());
                        } else {
                            wrapper.set(getStringValue());
                        }
                    }
                } else {
                    addChild(wrapper, name, getStringValue());
                    name = null;
                }
            }
        }
        @Override
        public void addChild(ValueState child) {

            if(wrapper != null) {
                if(buf != null && buf.length() > 0) {
                    wrapper.add(getStringValue());
                } else {
                    final ModelNode childNode;
                    if(child.getName() != null) {
                        childNode = new ModelNode();
                        childNode.get(child.getName()).set(child.getValue());
                    } else {
                        childNode = child.getValue();
                    }
                    addChild(wrapper, name, childNode);
                    name = null;
                }
            } else {
                final String childName = child.getName() != null ? child.getName() : name;
                addChild(getValue(), childName, child.getValue());
            }
        }
        @Override
        public ModelNode getValue() {
            return wrapper != null ? wrapper : getStringValue();
        }
        private ModelNode getStringValue() {
            final ModelNode value = new ModelNode();
            if(buf != null) {
                value.set(getTrimmedString());
            }
            return value;
        }
        protected void addChild(ModelNode parent, String name, ModelNode child) {
            if(list) {
                if(name != null) {
                    parent.add(name, child);
                } else {
                    parent.add(child);
                }
            } else {
                if(name != null) {
                    parent.get(name).set(child);
                } else {
                    parent.set(child);
                }
            }
        }

        @Override
        public boolean isList() {
            return false;
        }

        protected String getTrimmedString() {
            if(trimToSize >= 0) {
                buf.setLength(trimToSize);
                trimToSize = -1;
            }
            return buf.toString();
        }
    }

    class ListValueState implements ValueState {

        private ModelNode list;

        @Override
        public void addChild(ValueState child) {
            if(list != null) {
                list.add(child.getValue());
            } else {
                list = child.getValue();
                if(list.getType() != ModelType.LIST) {
                    ModelNode list = new ModelNode();
                    list.add(this.list);
                    this.list = list;
                }
            }
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void nameSeparator(ParsingContext ctx) throws CommandFormatException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void itemSeparator() throws CommandFormatException {
            //throw new UnsupportedOperationException();
        }

        @Override
        public void complete() throws CommandFormatException {
        }

        @Override
        public void character(char ch) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ModelNode getValue() {
            return list;
        }

        @Override
        public boolean isList() {
            return true;
        }
    }
}
