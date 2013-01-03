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


import org.jboss.as.cli.CommandLineException;


/**
 *
 * @author Alexey Loubyansky
 */
public class ExpressionParser {

    public static final String AND = "&&";
    public static final String OR = "||";

    public static final String EQ = "==";
    public static final String NOT_EQ = "!=";
    public static final String GT = ">";
    public static final String LT = "<";
    public static final String NLT = ">=";
    public static final String NGT = "<=";

    private String input;
    private int pos;
    private BaseOperation lookedAheadOp;

    public Operation parseExpression(String input) throws CommandLineException {
        //System.out.println("parsing: '" + input + "'");
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

        //System.out.println("parsed: " + op);
        return op;
    }

    public void reset() {
        input = null;
        pos = 0;
        lookedAheadOp = null;
    }

    public Operation getNextOperationFor(Operand firstOperand) throws CommandLineException {
        if(firstOperand == null) {
            firstOperand = parseOperand();
            if(firstOperand == null) {
                return null;
            }
        }
        if(isEOL()) {
            return (Operation) firstOperand;
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

    protected Operand parseOperand() throws CommandLineException {

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
        int end = pos;
        ComparisonOperation comparison = null;
        while(true) {
            if(pos == input.length()) {
                break;
            }
            final char ch = input.charAt(pos);
            if(Character.isWhitespace(ch)) {
                skipWhitespaces();
                comparison = tryComparison();
                break;
            }
            if(ch == '&' || ch == '|') {
                break;
            }
            comparison = tryComparison();
            if(comparison != null) {
                break;
            }
            ++pos;
            ++end;
        }

        final String op = input.substring(start, end);
        Operand operand;
        if(comparison != null) {
            // TODO this assumes the left one is always a path
            comparison.addOperand(new ModelNodePathOperand(op));
            operand = comparison;
            comparison.addOperand(parseOperand());
        } else {
            operand = new StringValueOperand(op);
        }

        return operand;
    }

    protected ComparisonOperation tryComparison() {
        if(pos >= input.length()) {
            return null;
        }
        if(input.startsWith(EQ, pos)) {
            pos += 2;
            return new EqualsOperation();
        } else if(input.startsWith(NOT_EQ, pos)) {
            pos += 2;
            return new NotEqualsOperation();
        } else if(input.charAt(pos) == '>') {
            if(input.length() > pos + 1 && input.charAt(pos + 1) == '=') {
                pos += 2;
                return new NotLesserThanOperation();
            } else {
                ++pos;
                return new GreaterThanOperation();
            }
        } else if(input.charAt(pos) == '<') {
            if(input.length() > pos + 1 && input.charAt(pos + 1) == '=') {
                pos += 2;
                return new NotGreaterThanOperation();
            } else {
                ++pos;
                return new LesserThanOperation();
            }
        }
        return null;
    }

    protected void skipWhitespaces() {
        for(;pos < input.length() && Character.isWhitespace(input.charAt(pos)); ++pos){}
    }

    protected BaseOperation getOperationForPosition() {
        if(input.startsWith(AND, pos)) {
            return new AndOperation();
        } else if(input.startsWith(OR, pos)) {
            return new OrOperation();
/*        } else if(input.startsWith(EQ, pos)) {
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
*/        } else {
            throw new IllegalStateException("Unexpected operation at " + pos + " in '" + input + "'");
        }
    }
}
