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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Alexey Loubyansky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CommandsArgumentTestCase {

    private static final String PROP_NAME = "cli-arg-test";
    private static final String SET_PROP_COMMAND = "/system-property=" + PROP_NAME + ":add(value=set)";
    private static final String GET_PROP_COMMAND = "/system-property=" + PROP_NAME + ":read-resource";
    private static final String REMOVE_PROP_COMMAND = "/system-property=" + PROP_NAME + ":remove";

    /**
     * Tests that the exit code is 0 value after a successful operation
     * @throws Exception
     */
    @Test
    public void testSuccess() throws Exception {
        int exitCode = execute(SET_PROP_COMMAND, true);
        if(exitCode == 0) {
            execute(REMOVE_PROP_COMMAND, true);
        }
        assertEquals(0, exitCode);
    }

    /**
     * Tests that the exit code is not 0 after a failed operation.
     * @throws Exception
     */
    @Test
    public void testFailure() throws Exception {
        assertFailure(GET_PROP_COMMAND);
    }

    /**
     * Tests that commands following a failed command aren't executed.
     * @throws Exception
     */
    @Test
    public void testValidCommandAfterInvalidCommand() throws Exception {
        int exitCode = execute('\"' + GET_PROP_COMMAND + ',' + SET_PROP_COMMAND + '\"', false);
        if(exitCode == 0) {
            execute(REMOVE_PROP_COMMAND, true);
        } else {
            assertFailure(GET_PROP_COMMAND);
        }
        assertTrue(exitCode != 0);
    }

    /**
     * Tests that commands preceding a failed command are't affected by the failure.
     * @throws Exception
     */
    @Test
    public void testValidCommandBeforeInvalidCommand() throws Exception {
        int exitCode = execute(SET_PROP_COMMAND + ",bad-wrong-illegal", false);
        assertSuccess(GET_PROP_COMMAND);
        execute(REMOVE_PROP_COMMAND, true);
        assertTrue(exitCode != 0);
    }

    protected void assertSuccess(String cmd) throws Exception {
        assertEquals(0, execute(cmd, true));
    }

    protected void assertFailure(String cmd) throws Exception {
        assertTrue(execute(cmd, false) != 0);
    }

    protected int execute(String cmd, boolean logFailure) throws IOException, InterruptedException {
        final String jbossDist = System.getProperty("jboss.dist");
        if(jbossDist == null) {
            fail("jboss.dist system property is not set");
        }
        final String modulePath = System.getProperty("module.path");
        if(modulePath == null) {
            fail("module.path system property is not set");
        }

        final ProcessBuilder builder = new ProcessBuilder();
        builder.command("java" , "-jar" , jbossDist + File.separatorChar + "jboss-modules.jar", "-mp", modulePath, "org.jboss.as.cli", "-c", cmd);
        final Process cliProc = builder.start();
        int exitCode = cliProc.waitFor();
        if (logFailure && exitCode != 0) {
            System.out.println("Failed to execute '" + cmd + "'");
            int bytesTotal = cliProc.getInputStream().available();
            if (bytesTotal > 0) {
                final byte[] bytes = new byte[bytesTotal];
                cliProc.getInputStream().read(bytes);
                System.out.println("Command's output: '"+ new String(bytes) + "'");
            }

            bytesTotal = cliProc.getErrorStream().available();
            if (bytesTotal > 0) {
                final byte[] bytes = new byte[bytesTotal];
                cliProc.getErrorStream().read(bytes);
                System.out.println("Command's error log: '" + new String(bytes) + "'");
            } else {
                System.out.println("No output data for the command.");
            }
        }
        return exitCode;
    }
}
