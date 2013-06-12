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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.logmanager.LogManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 *
 * @author Alexey Loubyansky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SilentModeTestCase {

    private static ByteArrayOutputStream cliOut;
    public static final String CLI_LOG_CFG = "/jboss-cli-logging.properties";
    public static final String CLI_LOG_FILE = "jboss-cli.log";

    @BeforeClass
    public static void setup() throws Exception {
        cliOut = new ByteArrayOutputStream();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        cliOut = null;
    }

    @Test
    public void testMain() throws Exception {
        cliOut.reset();
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.handle("help");
            assertFalse(cliOut.toString().isEmpty());

            cliOut.reset();
            ctx.setSilent(true);
            ctx.handle("help");
            assertTrue(cliOut.toString().isEmpty());

            cliOut.reset();
            ctx.setSilent(false);
            ctx.handle("help");
            assertFalse(cliOut.toString().isEmpty());
        } finally {
            ctx.terminateSession();
            cliOut.reset();
        }
    }

    @Test
    public void testLogging() throws Exception {
        setupCliLogging();
        cliOut.reset();

        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);

        try {
            ctx.setCurrentDir(new File("."));
            ctx.setSilent(true);
            ctx.connectController();
            ctx.handleSafe(":read-resource");
            assertTrue(cliOut.toString().isEmpty());
            assertFalse(checkIfEmpty(new File(CLI_LOG_FILE)));
        } finally {
            ctx.terminateSession();
            cliOut.reset();
            tearDownCLiLogging();
        }
    }

    @Test
    public void testOutputTarget() throws Exception {
        cliOut.reset();
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);

        File target = new File("cli_output");

        try {
            ctx.setCurrentDir(new File("."));
            ctx.setSilent(true);
            ctx.handleSafe("help > " + target.getAbsolutePath());
            assertTrue(cliOut.toString().isEmpty());
            assertFalse(checkIfEmpty(target));
        } finally {
            ctx.terminateSession();
            cliOut.reset();
            target.delete();
        }
    }

    private void setupCliLogging() throws Exception {
        File cliLogFile = new File(CLI_LOG_FILE);
        if (cliLogFile.exists()) {
            cliLogFile.delete();
        }

        InputStream is = null;
        try {
            is = getClass().getResourceAsStream(CLI_LOG_CFG);
            LogManager.getLogManager().readConfiguration(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private void tearDownCLiLogging() throws Exception {
        LogManager.getLogManager().reset();
        LogManager.getLogManager().readConfiguration();
    }

    private boolean checkIfEmpty(File file) throws Exception {
        boolean empty = false;
        FileInputStream fis = null;
        assertTrue(file.exists());
        try {
            fis = new FileInputStream(file);
            empty = fis.read() == -1;
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return empty;
    }
}
