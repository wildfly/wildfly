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
package org.jboss.as.test.integration.management.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
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
public class TryCatchFinallyTestCase {

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
    public void testSuccessfulTry() throws Exception {
        cliOut.reset();
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.connectController();
            ctx.handle("try");
            ctx.handle(getAddPropertyReq("try"));
            ctx.handle("catch");
            ctx.handle(getRemovePropertyReq());
            ctx.handle("end-try");
            cliOut.reset();
            ctx.handle(getReadPropertyReq());
            assertEquals("try", getValue());
        } finally {
            ctx.handleSafe(getRemovePropertyReq());
            ctx.terminateSession();
            cliOut.reset();
        }
    }

    @Test
    public void testCatch() throws Exception {
        cliOut.reset();
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.connectController();
            ctx.handle("try");
            ctx.handle(this.getReadNonexistingPropReq());
            ctx.handle("catch");
            ctx.handle(getAddPropertyReq("catch"));
            ctx.handle("end-try");
            cliOut.reset();
            ctx.handle(getReadPropertyReq());
            assertEquals("catch", getValue());
        } finally {
            ctx.handleSafe(getRemovePropertyReq());
            ctx.terminateSession();
            cliOut.reset();
        }
    }

    @Test
    public void testErrorInCatch() throws Exception {
        cliOut.reset();
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.connectController();
            ctx.handle("try");
            ctx.handle(this.getReadNonexistingPropReq());
            ctx.handle("catch");
            ctx.handle(this.getReadNonexistingPropReq());
            ctx.handle("end-try");
            fail("catch is expected to throw an exception");
        } catch(CommandLineException e) {
            // expected
        } finally {
            ctx.handleSafe(getRemovePropertyReq());
            ctx.terminateSession();
            cliOut.reset();
        }
    }

    @Test
    public void testTryFinally() throws Exception {
        cliOut.reset();
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.connectController();
            ctx.handle("try");
            ctx.handle(this.getAddPropertyReq("try"));
            ctx.handle("finally");
            ctx.handle(this.getWritePropertyReq("finally"));
            ctx.handle("end-try");
            cliOut.reset();
            ctx.handle(getReadPropertyReq());
            assertEquals("finally", getValue());
        } finally {
            ctx.handleSafe(getRemovePropertyReq());
            ctx.terminateSession();
            cliOut.reset();
        }
    }

    @Test
    public void testErrorInTryCatchFinally() throws Exception {
        cliOut.reset();
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.connectController();
            ctx.handle("try");
            ctx.handle(this.getReadNonexistingPropReq());
            ctx.handle("catch");
            ctx.handle(this.getAddPropertyReq("catch"));
            ctx.handle("finally");
            ctx.handle(this.getWritePropertyReq("finally"));
            ctx.handle("end-try");
            cliOut.reset();
            ctx.handle(getReadPropertyReq());
            assertEquals("finally", getValue());
        } finally {
            ctx.handleSafe(getRemovePropertyReq());
            ctx.terminateSession();
            cliOut.reset();
        }
    }

    @Test
    public void testErrorInTryErroInCatchFinally() throws Exception {
        cliOut.reset();
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.connectController();
            ctx.handle("try");
            ctx.handle(this.getReadNonexistingPropReq());
            ctx.handle("catch");
            ctx.handle(this.getReadNonexistingPropReq());
            ctx.handle("finally");
            ctx.handle(this.getAddPropertyReq("finally"));
            ctx.handle("end-try");
            fail("catch is expceted to throw an exception");
        } catch(CommandLineException e) {
            cliOut.reset();
            ctx.handle(getReadPropertyReq());
            assertEquals("finally", getValue());
        } finally {
            ctx.handleSafe(getRemovePropertyReq());
            ctx.terminateSession();
            cliOut.reset();
        }
    }

    @Test
    public void testErrorInFinally() throws Exception {
        cliOut.reset();
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.connectController();
            ctx.handle("try");
            ctx.handle(this.getAddPropertyReq("try"));
            ctx.handle("finally");
            ctx.handle(this.getReadNonexistingPropReq());
            ctx.handle("end-try");
            fail("finally is expceted to throw an exception");
        } catch(CommandLineException e) {
            cliOut.reset();
            ctx.handle(getReadPropertyReq());
            assertEquals("try", getValue());
        } finally {
            ctx.handleSafe(getRemovePropertyReq());
            ctx.terminateSession();
            cliOut.reset();
        }
    }

    protected String getAddPropertyReq(String value) {
        return "/system-property=" + PROP_NAME + ":add(value=" + value + ")";
    }

    protected String getWritePropertyReq(String value) {
        return "/system-property=" + PROP_NAME + ":write-attribute(name=\"value\",value=" + value + ")";
    }

    protected String getReadPropertyReq() {
        return "/system-property=" + PROP_NAME + ":read-resource";
    }

    protected String getReadNonexistingPropReq() {
        return "/system-property=itcantexist:read-resource";
    }

    protected String getRemovePropertyReq() {
        return "/system-property=" + PROP_NAME + ":remove";
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
