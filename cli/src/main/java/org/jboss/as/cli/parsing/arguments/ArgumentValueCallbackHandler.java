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
import org.jboss.as.cli.parsing.ParsingContext;
import org.jboss.as.cli.parsing.ParsingStateCallbackHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author Alexey Loubyansky
 */
public class ArgumentValueCallbackHandler implements ParsingStateCallbackHandler {

    private Deque<ValueState> stack;
    private ValueState currentState;

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
                currentState = new DefaultValueState(currentState.isList());
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
            currentState.nameSeparator();
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

        void nameSeparator() throws CommandFormatException;

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

        public DefaultValueState(boolean list) {
            this.list = list;
        }

        @Override
        public String getName() {
            return name;
        }
        @Override
        public void nameSeparator() throws CommandFormatException {
            if(buf == null) {
                throw new CommandFormatException("Property name is null.");
            }
            name = buf.toString();
            buf.setLength(0);
        }
        @Override
        public void character(char ch) {
            if(buf == null) {
                buf = new StringBuilder();
            }
            buf.append(ch);
        }
        @Override
        public void itemSeparator() {
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
                        wrapper.add(getStringValue());
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
                addChild(getValue(), child.getName(), child.getValue());
            }
        }
        @Override
        public ModelNode getValue() {
            return wrapper != null ? wrapper : getStringValue();
        }
        private ModelNode getStringValue() {
            final ModelNode value = new ModelNode();
            if(buf != null) {
                value.set(buf.toString());
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
    }

    class ListValueState implements ValueState {

        private String name;
        private ModelNode list;

        @Override
        public void addChild(ValueState child) {
            if(list != null) {
                throw new IllegalStateException();
            }
            name = child.getName();
            list = child.getValue();
            if(list.getType() != ModelType.LIST) {
                ModelNode list = new ModelNode();
                list.add(this.list);
                this.list = list;
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void nameSeparator() throws CommandFormatException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void itemSeparator() throws CommandFormatException {
            throw new UnsupportedOperationException();
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

    public static void main(String[] args) throws Exception {

        ModelNode one = new ModelNode();
        one.get("prop1").set("value1");
        one.get("prop2").set("value2");
        System.out.println(one);

        ModelNode two = new ModelNode();
        two.add("prop1", "value1");
        two.add("prop2", "value2");
        System.out.println(two);
    }
}
