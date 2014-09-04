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
                if(currentState.isOnSeparator() && !currentState.isComposite()) {
                    currentState.enteredValue();
                }
                pushCurrentState();
                currentState = new DefaultValueState();
            } else {
                currentState = new DefaultValueState();
            }
        } else if(CompositeState.OBJECT.equals(stateId)) {
            if(currentState != null) {
                currentState.enteredValue();
                pushCurrentState();
            }
            if (ctx.getCharacter() == '[') {
                currentState = new ListValueState();
            } else {
                currentState = new ObjectValueState();
            }
        } else if(CompositeState.LIST.equals(stateId)) {
            if(currentState != null) {
                currentState.enteredValue();
                pushCurrentState();
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

    void pushCurrentState() {
        if (stack == null) {
            stack = new ArrayDeque<ValueState>();
        }
        stack.push(currentState);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.parsing.ParsingStateCallbackHandler#leavingState(org.jboss.as.cli.parsing.ParsingContext)
     */
    @Override
    public void leavingState(ParsingContext ctx) throws CommandFormatException {
        final String stateId = ctx.getState().getId();
        //System.out.println("left " + stateId + " '" + ctx.getCharacter() + "'");

        if(ArgumentValueState.ID.equals(stateId) ||
                CompositeState.OBJECT.equals(stateId) || CompositeState.LIST.equals(stateId)) {
            if (!currentState.isOnSeparator()) {
                if (stack != null && stack.peek() != null) {
                    stack.peek().addChild(currentState);
                    currentState = stack.pop();
                    if (!currentState.isComposite()) {
                        if (stack.peek() != null) {
                            stack.peek().addChild(currentState);
                            currentState = stack.pop();
                        }
                    }
                }
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
        final ModelNode result;
        if(currentState.getName() != null) {
            result = new ModelNode();
            result.get(currentState.getName()).set(currentState.getValue());
        } else {
            result = currentState.getValue();
        }
        return result;
    }

    interface ValueState {

        void addChild(ValueState child);

        String getName();

        void nameSeparator(ParsingContext ctx) throws CommandFormatException;

        void itemSeparator() throws CommandFormatException;

        void character(char ch);

        ModelNode getValue();

        boolean isComposite();

        boolean isOnSeparator();

        void enteredValue();
    }

    class DefaultValueState implements ValueState {

        protected String name;
        protected StringBuilder buf;
        protected int trimToSize = -1;
        protected boolean dontQuote;

        protected boolean onSeparator;

        protected ModelNode child;

        @Override
        public boolean isOnSeparator() {
            return onSeparator;
        }

        @Override
        public void enteredValue() {
            onSeparator = false;
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
            onSeparator = true;
        }
        @Override
        public void character(char ch) {
            if(name != null) {
                throw new IllegalStateException("the name is already complete");
            }
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
        }

        @Override
        public void addChild(ValueState child) {
            if(this.child != null) {
                throw new IllegalStateException("child is already initialized");
            }
            this.child = child.getValue();
        }
        @Override
        public ModelNode getValue() {
            if(child != null) {
                return child;
            }
            return getStringValue();
        }
        private ModelNode getStringValue() {
            final ModelNode value = new ModelNode();
            if(buf != null) {
                value.set(getTrimmedString());
            }
            return value;
        }

        @Override
        public boolean isComposite() {
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
        private boolean onSeparator;

        @Override
        public boolean isOnSeparator() {
            return onSeparator;
        }

        @Override
        public void enteredValue() {
            onSeparator = false;
        }

        @Override
        public void addChild(ValueState child) {
            final ModelNode ch;
            if(child.getName() != null) {
                ch = new ModelNode();
                //ch.get(child.getName()).set(child.getValue()); // object
                ch.set(child.getName(), child.getValue()); // property
            } else {
                ch = child.getValue();
            }

            if(list != null) {
                list.add(ch);
            } else {
                list = ch;
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
            onSeparator = true;
        }

        @Override
        public void character(char ch) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ModelNode getValue() {
            if(list == null) {
                final ModelNode node = new ModelNode();
                node.setEmptyList();
                return node;
            }
            return list;
        }

        @Override
        public boolean isComposite() {
            return true;
        }
    }

    class ObjectValueState implements ValueState {

        private ModelNode obj;
        private boolean onSeparator;
        private boolean primitiveList;

        @Override
        public boolean isOnSeparator() {
            return onSeparator;
        }

        @Override
        public void enteredValue() {
            onSeparator = false;
        }

        @Override
        public void addChild(ValueState child) {
            if(child.getName() == null) {
                // the child should be the value
                if(obj != null) {
                    if(obj.getType() == ModelType.LIST) {
                        obj.add(child.getValue());
                    } else {
                        final ModelNode tmp = obj;
                        obj = new ModelNode();
                        obj.add(tmp);
                        obj.add(child.getValue());
                        primitiveList = true;
                    }
                    return;
                }
                obj = child.getValue();
                return;
            } else if(primitiveList) {
                throw new IllegalStateException("Can't add a property " + child.getName() + "=" + child.getValue()
                        + " to a list of values " + obj.asString());
            }
            if(obj == null) {
                obj = new ModelNode();
            }
            obj.get(child.getName()).set(child.getValue());
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
            onSeparator = true;
        }

        @Override
        public void character(char ch) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ModelNode getValue() {
            if(obj == null) {
                return new ModelNode();
            }
            return obj;
        }

        @Override
        public boolean isComposite() {
            return true;
        }
    }
}
