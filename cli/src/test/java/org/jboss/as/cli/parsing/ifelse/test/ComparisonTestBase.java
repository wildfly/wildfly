package org.jboss.as.cli.parsing.ifelse.test;

import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.completion.mock.MockCommandContext;
import org.jboss.as.cli.handlers.ifelse.ExpressionParser;
import org.jboss.as.cli.handlers.ifelse.Operation;
import org.jboss.dmr.ModelNode;

public class ComparisonTestBase {

    private MockCommandContext ctx = new MockCommandContext();
    private ExpressionParser parser = new ExpressionParser();

    public ComparisonTestBase() {
        super();
    }

    protected void assertTrue(ModelNode node, final String expr)
            throws CommandLineException {
                parser.reset();
                Operation op = parser.parseExpression(expr);
                final Object value = op.resolveValue(ctx, node);
                org.junit.Assert.assertTrue((Boolean)value);
            }

    protected void assertFalse(ModelNode node, final String expr)
            throws CommandLineException {
                parser.reset();
                Operation op = parser.parseExpression(expr);
                final Object value = op.resolveValue(ctx, node);
                org.junit.Assert.assertFalse((Boolean)value);
            }

}