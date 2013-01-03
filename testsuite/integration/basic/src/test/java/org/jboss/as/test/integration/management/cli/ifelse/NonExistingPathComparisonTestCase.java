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
package org.jboss.as.test.integration.management.cli.ifelse;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Alexey Loubyansky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class NonExistingPathComparisonTestCase {

    private static final String RESPONSE_VALUE_PREFIX = "\"value\" => \"";

    private static final String PROP_NAME = "jboss-cli-test";

    private static ByteArrayOutputStream cliOut;

    @BeforeClass
    public static void setup() throws Exception {
        cliOut = new ByteArrayOutputStream();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        cliOut = null;
    }

    @Test
    public void testEquals() throws Exception {
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.connectController();
            ctx.handle(this.getAddPropertyReq("\"false\""));
            assertEquals("false", runIf(ctx, "==", "&&", "\"false\""));
            assertEquals("true", runIf(ctx, "==", "||", "\"false\""));
        } finally {
            ctx.handleSafe(this.getRemovePropertyReq());
            ctx.terminateSession();
            cliOut.reset();
        }
    }

    @Test
    public void testNotEqual() throws Exception {
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.connectController();
            ctx.handle(this.getAddPropertyReq("\"false\""));
            assertEquals("false", runIf(ctx, "!=", "&&", "\"true\""));
            assertEquals("true", runIf(ctx, "!=", "||", "\"true\""));
        } finally {
            ctx.handleSafe(this.getRemovePropertyReq());
            ctx.terminateSession();
            cliOut.reset();
        }
    }

    @Test
    public void testGreaterThan() throws Exception {
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.connectController();
            ctx.handle(this.getAddPropertyReq("\"5\""));
            assertEquals("5", runIf(ctx, ">", "&&", "\"1\""));
            assertEquals("true", runIf(ctx, ">", "||", "\"1\""));
        } finally {
            ctx.handleSafe(this.getRemovePropertyReq());
            ctx.terminateSession();
            cliOut.reset();
        }
    }

    @Test
    public void testLessThan() throws Exception {
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.connectController();
            ctx.handle(this.getAddPropertyReq("\"5\""));
            assertEquals("5", runIf(ctx, "<", "&&", "\"7\""));
            assertEquals("true", runIf(ctx, "<", "||", "\"7\""));
        } finally {
            ctx.handleSafe(this.getRemovePropertyReq());
            ctx.terminateSession();
            cliOut.reset();
        }
    }

    @Test
    public void testNotLessThan() throws Exception {
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.connectController();
            ctx.handle(this.getAddPropertyReq("\"5\""));
            assertEquals("5", runIf(ctx, ">=", "&&", "\"5\""));
            assertEquals("true", runIf(ctx, ">=", "||", "\"5\""));
        } finally {
            ctx.handleSafe(this.getRemovePropertyReq());
            ctx.terminateSession();
            cliOut.reset();
        }
    }

    @Test
    public void testNotGreaterThan() throws Exception {
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.connectController();
            ctx.handle(this.getAddPropertyReq("\"5\""));
            assertEquals("5", runIf(ctx, "<=", "&&", "\"5\""));
            assertEquals("true", runIf(ctx, "<=", "||", "\"5\""));
        } finally {
            ctx.handleSafe(this.getRemovePropertyReq());
            ctx.terminateSession();
            cliOut.reset();
        }
    }

    protected String runIf(CommandContext ctx, String comparison, String logical, String value) throws Exception {
        ctx.handle(getIfStatement(comparison, logical, value));
        ctx.handle(this.getWritePropertyReq("\"true\""));
        ctx.handle("end-if");
        cliOut.reset();
        ctx.handle(getReadPropertyReq());
        return getValue();
    }

    protected String getIfStatement(String comparison, String logical, String value) {
        return new StringBuilder().append("if (result.value ").append(comparison).append(' ').append(value).append(' ').
                append(logical).
                append(" result.value2 ").append(comparison).append(' ').append(value).
                append(") of ").append(getReadPropertyReq()).toString();
    }

    protected String getAddPropertyReq(String value) {
        return "/system-property=" + PROP_NAME + ":add(value=" + value + ")";
    }

    protected String getReadPropertyReq() {
        return "/system-property=" + PROP_NAME + ":read-resource";
    }

    protected String getRemovePropertyReq() {
        return "/system-property=" + PROP_NAME + ":remove";
    }

    protected String getWritePropertyReq(String value) {
        return "/system-property=" + PROP_NAME + ":write-attribute(name=\"value\",value=" + value + ")";
    }

    protected String getValue() {
        final String response = cliOut.toString();
        final int start = response.indexOf(RESPONSE_VALUE_PREFIX);
        if(start < 0) {
            Assert.fail("Value not found in the response: " + response);
        }
        final int end = response.indexOf('"', start + RESPONSE_VALUE_PREFIX.length() + 1);
        if(end < 0) {
            Assert.fail("Couldn't locate the closing quote: " + response);
        }
        return response.substring(start + RESPONSE_VALUE_PREFIX.length(), end);
    }
}
