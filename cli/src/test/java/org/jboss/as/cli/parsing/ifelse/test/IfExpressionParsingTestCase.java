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
package org.jboss.as.cli.parsing.ifelse.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.jboss.as.cli.handlers.ifelse.ExpressionParser.AND;
import static org.jboss.as.cli.handlers.ifelse.ExpressionParser.OR;
import static org.jboss.as.cli.handlers.ifelse.ExpressionParser.EQ;
import static org.jboss.as.cli.handlers.ifelse.ExpressionParser.NOT_EQ;
import static org.jboss.as.cli.handlers.ifelse.ExpressionParser.GT;
import static org.jboss.as.cli.handlers.ifelse.ExpressionParser.LT;
import static org.jboss.as.cli.handlers.ifelse.ExpressionParser.NGT;
import static org.jboss.as.cli.handlers.ifelse.ExpressionParser.NLT;

import java.util.List;

import org.jboss.as.cli.handlers.ifelse.ExpressionParser;
import org.jboss.as.cli.handlers.ifelse.Operand;
import org.jboss.as.cli.handlers.ifelse.Operation;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class IfExpressionParsingTestCase {

    private ExpressionParser parser = new ExpressionParser();

    @Test
    public void testParenthesesMixed() throws Exception {
        parser.reset();
        Operation op = parser.parseExpression("((a==b || c<=d && e>f) && g!=h || i>=j) && (k<l && m>n || o==p)");
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
    public void testSimpleParentheses() throws Exception {
        parser.reset();
        Operation op = parser.parseExpression("  a >=b && (c<d || e> f )&&  g  !=  h ");
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
    public void testMixNoParentheses() throws Exception {
        parser.reset();
        Operation op = parser.parseExpression("  a>b && c>=d && e<f ||  g <= h && i==j || k != l");
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
    public void testOrSequence() throws Exception {
        parser.reset();
        Operation op = parser.parseExpression("a == b || c== d||e==f");
        assertOperation(op, OR, 3);
        final List<Operand> operands = op.getOperands();
        assertComparison(operands.get(0), EQ, "a", "b");
        assertComparison(operands.get(1), EQ, "c", "d");
        assertComparison(operands.get(2), EQ, "e", "f");
    }

    @Test
    public void testAndSequence() throws Exception {
        parser.reset();
        Operation op = parser.parseExpression("a==b && c == d && e == f");
        assertOperation(op, AND, 3);
        final List<Operand> operands = op.getOperands();
        assertComparison(operands.get(0), EQ, "a", "b");
        assertComparison(operands.get(1), EQ, "c", "d");
        assertComparison(operands.get(2), EQ, "e", "f");
    }

    protected void assertOperation(Operand operand, String opName, int operandsTotal) {
        assertTrue(operand instanceof Operation);
        assertEquals(opName, ((Operation)operand).getName());
        assertEquals(operandsTotal, ((Operation)operand).getOperands().size());
    }

    protected void assertComparison(Operand operand, String opName, String left, String right) {
        assertNotNull(operand);
        assertTrue(operand instanceof Operation);
        Operation op = (Operation) operand;
        assertEquals(opName, op.getName());
        assertNotNull(op.getOperands());
        assertEquals(2, op.getOperands().size());
        assertEquals(left, op.getOperands().get(0).toString());
        assertEquals(right, op.getOperands().get(1).toString());
    }
}
