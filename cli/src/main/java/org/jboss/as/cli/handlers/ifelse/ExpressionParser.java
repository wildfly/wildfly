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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class ExpressionParser {

    private static final String AND = "&&";
    private static final String OR = "||";

    private static final String EQ = "==";
    private static final String NOT_EQ = "!=";
    private static final String GT = ">";
    private static final String LT = "<";
    private static final String NLT = ">=";
    private static final String NGT = "<=";

    private static final char[] OPERATION_STARTS = new char[]{'&', '|', '=', '!', '>', '<'};

    private static boolean isEndWord(char ch) {
        if(Character.isWhitespace(ch)) {
            return true;
        }
        for(int i = 0; i < OPERATION_STARTS.length; ++i) {
            if(ch == OPERATION_STARTS[i]) {
                return true;
            }
        }
        return false;
    }

    private String input;
    private int pos;
    private BaseOperation lookedAheadOp;

    public Operation parseExpression(String input) {
//        System.out.println("parsing: '" + input + "'");
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

//        System.out.println("parsed: " + op);
        return op;
    }

    public void reset() {
        input = null;
        pos = 0;
        lookedAheadOp = null;
    }

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
        } else {
            op = getOperationForPosition();
        }

        op.addOperand(firstOperand);
        while (input.startsWith(op.getName(), pos) && input.length() >= pos + op.getName().length()) {
            pos += op.getName().length();

            Operand operand = parseOperand();
            if(operand == null) {
                return op;
            }

            if(!isEOL()) {
                lookedAheadOp = getOperationForPosition();
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
                        final Operation expr = parseExpression(input.substring(pos + 1, endIndex));
                        skipWhitespaces();
                        return expr;
                    } else {
                        --depthCount;
                    }
                }
            }
            throw new IllegalStateException("Failed to locate closing ')' for '(' at " + pos + " in '" + input + "'");
        }

        final int start = pos;
        for(;pos < input.length() && !isEndWord(input.charAt(pos)); ++pos);

        final String op = input.substring(start, pos);
        skipWhitespaces();

        Operand operand = new Operand() {
            public String toString() {
                return '\'' + op + '\'';
            }
        };

        if(!isEOL()) {
            ComparisonOperation comparison = null;
            if(input.startsWith(EQ, pos)) {
                comparison = new ComparisonOperation(EQ);
                pos += 2;
            } else if(input.startsWith(NOT_EQ, pos)) {
                comparison = new ComparisonOperation(NOT_EQ);
                pos += 2;
            } else if(input.charAt(pos) == '>') {
                if(input.length() > pos + 1 && input.charAt(pos + 1) == '=') {
                    comparison = new ComparisonOperation(NLT);
                    pos += 2;
                } else {
                    comparison = new ComparisonOperation(GT);
                    ++pos;
                }
            } else if(input.charAt(pos) == '<') {
                if(input.length() > pos + 1 && input.charAt(pos + 1) == '=') {
                    comparison = new ComparisonOperation(NGT);
                    pos += 2;
                } else {
                    comparison = new ComparisonOperation(LT);
                    ++pos;
                }
            }

            if(comparison != null) {
                comparison.addOperand(operand);
                operand = comparison;
                comparison.addOperand(parseOperand());
            }
        }
        return operand;
    }

    protected void skipWhitespaces() {
        for(;pos < input.length() && Character.isWhitespace(input.charAt(pos)); ++pos);
    }

    protected BaseOperation getOperationForPosition() {
        if(input.startsWith(AND, pos)) {
            return new AndOperation();
        } else if(input.startsWith(OR, pos)) {
            return new OrOperation();
        } else if(input.startsWith(EQ, pos)) {
            return new ComparisonOperation(EQ);
        } else if (input.startsWith(NOT_EQ, pos)) {
            return new ComparisonOperation(NOT_EQ);
        } else if (input.charAt(pos) == '>') {
            if (input.length() > pos + 1 && input.charAt(pos + 1) == '=') {
                return new ComparisonOperation(NLT);
            }
            return new ComparisonOperation(GT);
        } else if (input.charAt(pos) == '<') {
            if (input.length() > pos + 1 && input.charAt(pos + 1) == '=') {
                return new ComparisonOperation(NGT);
            }
            return new ComparisonOperation(LT);
        } else {
            throw new IllegalStateException("Unrecognized operation at " + pos + " in '" + input + "'");
        }
    }

    protected BaseOperation getComparisonForPosition() {
        if(input.startsWith(EQ, pos)) {
            return new ComparisonOperation(EQ);
        } else if(input.startsWith(NOT_EQ, pos)) {
            return new ComparisonOperation(NOT_EQ);
        } else if(input.charAt(pos) == '>') {
            if(input.length() > pos + 1 && input.charAt(pos + 1) == '=') {
                return new ComparisonOperation(NLT);
            }
            return new ComparisonOperation(GT);
        } else if(input.charAt(pos) == '<') {
            if(input.length() > pos + 1 && input.charAt(pos + 1) == '=') {
                return new ComparisonOperation(NGT);
            }
            return new ComparisonOperation(LT);
        } else {
            throw new IllegalStateException("Unrecognized comparison at " + pos + " in '" + input + "'");
        }
    }

    static class ComparisonOperation extends BaseOperation {
        ComparisonOperation(String name) {
            super(name, 8);
        }
    }

    static class AndOperation extends BaseOperation {
        AndOperation() {
            super("&&", 4);
        }
    }

    static class OrOperation extends BaseOperation {
        OrOperation() {
            super("||", 2);
        }
    }

    static abstract class BaseOperation implements Operation, Comparable<Operation> {
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

        protected void addOperand(Operand operand) {
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

    @Test
    public void testParenthesesMixed() {
        reset();
        Operation op = parseExpression("((a==b || c<=d && e>f) && g!=h || i>=j) && (k<l && m>n || o==p)");
        assertOperation(op, AND, 2);
        final List<Operand> topOperands = op.getOperands();

        Operand operand = topOperands.get(0);
        assertOperation(operand, OR, 2);

        Operation or = (Operation) operand;
        operand = or.getOperands().get(0);
        assertOperation(operand, AND, 2);

        Operation and = (Operation) operand;
        operand = and.getOperands().get(0);
        assertOperation(operand, OR, 2);
        assertComparison(((Operation)operand).getOperands().get(0), EQ, "a", "b");
        Operand cdAndEf = ((Operation)operand).getOperands().get(1);
        assertOperation(cdAndEf, AND, 2);
        assertComparison(((Operation)cdAndEf).getOperands().get(0), NGT, "c", "d");
        assertComparison(((Operation)cdAndEf).getOperands().get(1), GT, "e", "f");

        operand = and.getOperands().get(1);
        assertComparison(operand, NOT_EQ, "g", "h");

        operand = or.getOperands().get(1);
        assertComparison(operand, NLT, "i", "j");

        operand = topOperands.get(1);
        assertOperation(operand, OR, 2);
        or = (Operation) operand;
        operand = or.getOperands().get(0);

        assertOperation(operand, AND, 2);
        assertComparison(((Operation)operand).getOperands().get(0), LT, "k", "l");
        assertComparison(((Operation)operand).getOperands().get(1), GT, "m", "n");

        assertComparison(or.getOperands().get(1), EQ, "o", "p");
    }

    @Test
    public void testSimpleParentheses() {
        reset();
        Operation op = parseExpression("  a >=b && (c<d || e> f )&&  g  !=  h ");
        assertOperation(op, AND, 3);
        final List<Operand> operands = op.getOperands();

        Operand operand = operands.get(0);
        assertComparison(operand, NLT, "a", "b");

        operand = operands.get(1);
        assertOperation(operand, OR, 2);
        assertComparison(((Operation)operand).getOperands().get(0), LT, "c", "d");
        assertComparison(((Operation)operand).getOperands().get(1), GT, "e", "f");

        operand = operands.get(2);
        assertComparison(operand, NOT_EQ, "g", "h");
    }

    @Test
    public void testMixNoParentheses() {
        reset();
        Operation op = parseExpression("  a>b && c>=d && e<f ||  g <= h && i==j || k != l");
        assertOperation(op, OR, 3);
        final List<Operand> operands = op.getOperands();

        Operand operand = operands.get(0);
        assertOperation(operand, AND, 3);
        assertComparison(((Operation)operand).getOperands().get(0), GT, "a", "b");
        assertComparison(((Operation)operand).getOperands().get(1), NLT, "c", "d");
        assertComparison(((Operation)operand).getOperands().get(2), LT, "e", "f");

        operand = operands.get(1);
        assertOperation(operand, AND, 2);
        assertComparison(((Operation)operand).getOperands().get(0), NGT, "g", "h");
        assertComparison(((Operation)operand).getOperands().get(1), EQ, "i", "j");

        operand = operands.get(2);
        assertComparison(operand, NOT_EQ, "k", "l");
    }

    @Test
    public void testOrSequence() {
        reset();
        Operation op = parseExpression("a == b || c== d||e==f");
        assertOperation(op, OR, 3);
        final List<Operand> operands = op.getOperands();
        assertComparison(operands.get(0), EQ, "a", "b");
        assertComparison(operands.get(1), EQ, "c", "d");
        assertComparison(operands.get(2), EQ, "e", "f");
    }

    @Test
    public void testAndSequence() {
        reset();
        Operation op = parseExpression("a==b && c == d && e == f");
        assertOperation(op, AND, 3);
        final List<Operand> operands = op.getOperands();
        assertComparison(operands.get(0), EQ, "a", "b");
        assertComparison(operands.get(1), EQ, "c", "d");
        assertComparison(operands.get(2), EQ, "e", "f");
    }

    protected void assertOperation(Operand operand, String opName, int operandsTotal) {
        assertTrue(operand instanceof BaseOperation);
        assertEquals(opName, ((BaseOperation)operand).getName());
        assertEquals(operandsTotal, ((BaseOperation)operand).getOperands().size());
    }

    protected void assertComparison(Operand operand, String opName, String left, String right) {
        assertNotNull(operand);
        assertTrue(operand instanceof ComparisonOperation);
        BaseOperation op = (BaseOperation) operand;
        assertEquals(opName, op.getName());
        assertNotNull(op.getOperands());
        assertEquals(2, op.getOperands().size());
        assertEquals('\'' + left + '\'', op.getOperands().get(0).toString());
        assertEquals('\'' + right + '\'', op.getOperands().get(1).toString());
    }
}
