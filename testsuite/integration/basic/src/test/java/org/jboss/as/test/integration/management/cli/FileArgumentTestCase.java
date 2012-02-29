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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author Alexey Loubyansky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class FileArgumentTestCase {

    private static final String PROP_NAME = "cli-arg-test";
    private static final String SET_PROP_COMMAND = "/system-property=" + PROP_NAME + ":add(value=set)";
    private static final String GET_PROP_COMMAND = "/system-property=" + PROP_NAME + ":read-resource";
    private static final String REMOVE_PROP_COMMAND = "/system-property=" + PROP_NAME + ":remove";

    private static final String FILE_NAME = "jboss-cli-file-arg-test.cli";
    private static final File TMP_FILE;
    static {
        TMP_FILE = new File(new File(System.getProperty("java.io.tmpdir")), FILE_NAME);
    }

    @AfterClass
    public static void cleanUp() {
        if(TMP_FILE.exists()) {
            TMP_FILE.delete();
        }
    }

    /**
     * Tests that the exit code is 0 value after a successful operation
     * @throws Exception
     */
    @Test
    public void testSuccess() {
        int exitCode = executeAsFile(new String[]{SET_PROP_COMMAND}, true);
        if(exitCode == 0) {
            executeAsFile(new String[]{REMOVE_PROP_COMMAND}, true);
        }
        assertEquals(0, exitCode);
    }

    /**
     * Tests that the exit code is not 0 after a failed operation.
     * @throws Exception
     */
    @Test
    public void testFailure() {
        assertFailure(GET_PROP_COMMAND);
    }

    /**
     * Tests that commands following a failed command aren't executed.
     * @throws Exception
     */
    @Test
    public void testValidCommandAfterInvalidCommand() {
        int exitCode = executeAsFile(new String[]{GET_PROP_COMMAND, SET_PROP_COMMAND}, false);
        if(exitCode == 0) {
            executeAsFile(new String[]{REMOVE_PROP_COMMAND}, true);
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
    public void testValidCommandBeforeInvalidCommand() {
        int exitCode = executeAsFile(new String[]{SET_PROP_COMMAND, "bad-wrong-illegal"}, true);
        assertSuccess(GET_PROP_COMMAND);
        executeAsFile(new String[]{REMOVE_PROP_COMMAND}, true);
        assertTrue(exitCode != 0);
    }

    protected void assertSuccess(String cmd) {
        assertEquals(0, executeAsFile(new String[]{cmd}, true));
    }

    protected void assertFailure(String cmd) {
        assertTrue(executeAsFile(new String[]{cmd}, false) != 0);
    }

    protected int executeAsFile(String[] cmd, boolean logFailure) {
        createFile(cmd);
        return execute(TMP_FILE, logFailure);
    }

    protected void createFile(String[] cmd) {
        if(TMP_FILE.exists()) {
            if(!TMP_FILE.delete()) {
                fail("Failed to delete " + TMP_FILE.getAbsolutePath());
            }
        }

        FileWriter writer = null;
        try {
            writer = new FileWriter(TMP_FILE);
            for(String line : cmd) {
                writer.write(line);
                writer.write('\n');
            }
        } catch (IOException e) {
            fail("Failed to write to " + TMP_FILE.getAbsolutePath() + ": " + e.getLocalizedMessage());
        } finally {
            if(writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }

    protected int execute(File f, boolean logFailure) {
        final String jbossDist = System.getProperty("jboss.dist");
        if(jbossDist == null) {
            fail("jboss.dist system property is not set");
        }
        final String modulePath = System.getProperty("module.path");
        if(modulePath == null) {
            fail("module.path system property is not set");
        }

        final ProcessBuilder builder = new ProcessBuilder();
        final List<String> command = new ArrayList<String>();
        command.add("java");
        TestSuiteEnvironment.getIpv6Args(command);
        command.add("-jar");
        command.add(jbossDist + File.separatorChar + "jboss-modules.jar");
        command.add("-mp");
        command.add(modulePath);
        command.add("org.jboss.as.cli");
        command.add("-c");
        command.add("--controller=" + TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getServerPort());
        command.add("--file=" + f.getAbsolutePath());
        builder.command(command);
        Process cliProc = null;
        try {
            cliProc = builder.start();
        } catch (IOException e) {
            fail("Failed to start CLI process: " + e.getLocalizedMessage());
        }
        int exitCode = 0;
        try {
            exitCode = cliProc.waitFor();
        } catch (InterruptedException e) {
            fail("Interrupted waiting for the CLI process.");
        }
        if (logFailure && exitCode != 0) {
            System.out.println("Failed to execute " + f.getAbsolutePath());
            try {
                int bytesTotal = cliProc.getInputStream().available();
                if (bytesTotal > 0) {
                    final byte[] bytes = new byte[bytesTotal];
                    cliProc.getInputStream().read(bytes);
                    System.out.println("Command's output: '" + new String(bytes) + "'");
                }
            } catch(Exception e) {
                System.out.println("Failed to read command's output: " + e.getLocalizedMessage());
            }

            try {
                int bytesTotal = cliProc.getErrorStream().available();
                if (bytesTotal > 0) {
                    final byte[] bytes = new byte[bytesTotal];
                    cliProc.getErrorStream().read(bytes);
                    System.out.println("Command's error log: '" + new String(bytes) + "'");
                } else {
                    System.out.println("No output data for the command.");
                }
            } catch(Exception e) {
                System.out.println("Failed to read command's error output: " + e.getLocalizedMessage());
            }
        }
        return exitCode;
    }
}
