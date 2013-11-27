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

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Alexey Loubyansky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MultipleLinesCommandsTestCase {

    private static String[] operation;
    private static String[] command;

    @BeforeClass
    public static void init() {
        final String lineSep = TestSuiteEnvironment.getSystemProperty("line.separator");

        operation = new String[]{
                ":\\" + lineSep,
                "read-resource(\\" + lineSep,
                "include-defaults=true,\\" + lineSep,
                "recursive=false)"
        };

        command = new String[]{
                "read-attribute\\" + lineSep,
                "product-name\\" + lineSep,
                "--verbose"
        };
    }

    protected void handleAsOneString(String[] arr) throws Exception {
        final StringBuilder buf = new StringBuilder();
        for(String line : arr) {
            buf.append(line);
        }
        final CommandContext ctx = CLITestUtil.getCommandContext();
        try {
            ctx.connectController();
            ctx.handle(buf.toString());
        } finally {
            ctx.terminateSession();
        }
    }

    protected void handleInPieces(String[] arr) throws Exception {
        final CommandContext ctx = CLITestUtil.getCommandContext();
        try {
            ctx.connectController();
            for(String line : arr) {
                ctx.handle(line);
            }
        } finally {
            ctx.terminateSession();
        }
    }

    @Test
    public void testOperationAsOneString() throws Exception {
        handleAsOneString(operation);
    }

    @Test
    public void testOperationInPieces() throws Exception {
        handleInPieces(operation);
    }

    @Test
    public void testCommandAsOneString() throws Exception {
        handleAsOneString(command);
    }

    @Test
    public void testCommandInPieces() throws Exception {
        handleInPieces(command);
    }
}
