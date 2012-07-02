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
package org.jboss.as.cli.handlers.ifelse;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class ExpressionParser {

    private static final String AND = "&&";
    private static final String OR = "||";

    private String input;
    private int pos;

    protected Operation parseExpression(String input) {
        System.out.println("parsing: '" + input + "'");
        final String prevInput = this.input;
        final int prevPos = this.pos;

        this.input = input;
        this.pos = 0;

        Operation op = null;
        while(!isEOL()) {
            op = getNextOperationFor(op);
        }

        if(prevInput != null) {
            this.input = prevInput;
            this.pos += prevPos + 2;
        }

        System.out.println("parsed: " + op);
        return op;
    }

    private BaseOperation lookedAheadOp;

    public Operation getNextOperationFor(Operand firstOperand) {
        if(firstOperand == null) {
            firstOperand = parseOperand();
            if(firstOperand == null) {
                return null;
            }
        }
        if(isEOL()) {
            return null;
        }

        final BaseOperation op;
        if(lookedAheadOp != null) {
            op = lookedAheadOp;
            lookedAheadOp = null;
        } else if(input.startsWith(AND, pos)) {
            op = new AndOperation();
        } else if(input.startsWith(OR, pos)) {
            op = new OrOperation();
        } else {
            throw new IllegalStateException("Unrecognized operation at " + pos + ": " + input);
        }

        op.addOperand(firstOperand);
        while (input.startsWith(op.getName(), pos) && input.length() >= pos + op.getName().length()) {
            pos += op.getName().length();

            Operand operand = parseOperand();
            if(operand == null) {
                return op;
            }

            if(!isEOL()) {
                if (input.startsWith(AND, pos)) {
                    lookedAheadOp = new AndOperation();
                } else if (input.startsWith(OR, pos)) {
                    lookedAheadOp = new OrOperation();
                } else {
                    throw new IllegalStateException("Unrecognized operation at " + pos + ": " + input);
                }
                if (lookedAheadOp.getPriority() > op.getPriority()) {
                    operand = getNextOperationFor(operand);
                }
            }
            op.addOperand(operand);
        }

        return op;
    }

    public boolean isEOL() {
        return pos >= input.length();
    }

    protected Operand parseOperand() {

        skipWhitespaces();
        if(pos == input.length()) {
            return null;
        }

        if(input.charAt(pos) == '(') {
            int depthCount = 0;
            int endIndex = pos;
            while(++endIndex < input.length()) {
                if(input.charAt(endIndex) == '(') {
                    ++depthCount;
                } else if(input.charAt(endIndex) == ')') {
                    if(depthCount == 0) {
                        return parseExpression(input.substring(pos + 1, endIndex));
                    } else {
                        --depthCount;
                    }
                }
            }
        }

        int start = pos;
        for(;pos < input.length() && !Character.isWhitespace(input.charAt(pos)); ++pos);
        final String op = input.substring(start, pos);
        skipWhitespaces();

        return new Operand() {
            public String toString() {
                return '\'' + op + '\'';
            }
        };
    }

    protected String nextWord() {
        skipWhitespaces();
        if(pos == input.length()) {
            return null;
        }
        int start = pos;
        for(;pos < input.length() && !Character.isWhitespace(input.charAt(pos)); ++pos);
        final String op = input.substring(start, pos);
        skipWhitespaces();
        return op;
    }

    protected void skipWhitespaces() {
        for(;pos < input.length() && Character.isWhitespace(input.charAt(pos)); ++pos);
    }

    class AndOperation extends BaseOperation {
        AndOperation() {
            super("&&", 4);
        }
    }

    class OrOperation extends BaseOperation {
        OrOperation() {
            super("||", 2);
        }
    }

    abstract class BaseOperation implements Operation, Comparable<Operation> {
        private final String name;
        private final int priority;
        private final List<Operand> operands;

        BaseOperation(String name, int priority) {
            if(name == null) {
                throw new IllegalArgumentException("name is null.");
            }
            this.name = name;
            this.priority = priority;
            operands = new ArrayList<Operand>();
        }

        String getName() {
            return name;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public List<Operand> getOperands() {
            return operands;
        }

        void addOperand(Operand operand) {
            if(operand == null) {
                throw new IllegalArgumentException("operand can't be null.");
            }
            operands.add(operand);
        }

        @Override
        public int compareTo(Operation o) {
            if(o == null) {
                throw new IllegalArgumentException("can't compare to null.");
            }
            return priority < o.getPriority() ? -1 : (priority > o.getPriority() ? 1 : 0);
        }

        @Override
        public String toString() {
            return '(' + name + ' ' + operands + ')';
        }
    }

    public static void main(String[] args) throws Exception {
        testExpression("a && b && c");
        testExpression("a || b || c");
        testExpression("  a && b && c ||  d && e || f");
        testExpression("  a && (b || c )&&  d ");
    }

    protected static void testExpression(final String input) {
        new ExpressionParser().parseExpression(input);
    }
}
