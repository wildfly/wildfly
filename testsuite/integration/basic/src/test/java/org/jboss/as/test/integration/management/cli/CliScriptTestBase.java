/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.test.shared.TestSuiteEnvironment;

/**
 * @author Alexey Loubyansky
 */
public class CliScriptTestBase {

    private static final int CLI_PROC_TIMEOUT = 10000;
    private static final int STATUS_CHECK_INTERVAL = 2000;

    private String cliOutput;


    protected void assertSuccess(String cmd) {
        assertEquals(0, execute(cmd, true));
    }

    protected void assertFailure(String cmd) {
        assertTrue(execute(cmd, false) != 0);
    }

    protected String getLastCommandOutput() {
        return cliOutput;
    }

    protected int execute(String cmd) {
        return execute(cmd, false);
    }

    protected int execute(String cmd, boolean logFailure) {
        return execute(true, cmd, logFailure);
    }

    protected int execute(boolean connect, String cmd) {
        return execute(connect, cmd, false);
    }

    protected int execute(boolean connect, String cmd, boolean logFailure) {
        return execute(TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), connect, cmd, logFailure);
    }

    protected int execute(String host, int port, boolean connect, String cmd, boolean logFailure) {
        cliOutput = null;
        final String jbossDist = System.getProperty("jboss.dist");
        if (jbossDist == null) {
            fail("jboss.dist system property is not set");
        }
        final String modulePath = System.getProperty("module.path");
        if (modulePath == null) {
            fail("module.path system property is not set");
        }

        final ProcessBuilder builder = new ProcessBuilder();
        builder.redirectErrorStream(true);
        final List<String> command = new ArrayList<String>();
        command.add("java");
        TestSuiteEnvironment.getIpv6Args(command);
        command.add("-jar");
        command.add(jbossDist + File.separatorChar + "jboss-modules.jar");
        command.add("-mp");
        command.add(modulePath);
        command.add("org.jboss.as.cli");
        if(connect) {
            command.add("-c");
        }
        command.add("--controller=" + host + ":" + port);
        command.add(cmd);
        builder.command(command);
        Process cliProc = null;
        try {
            cliProc = builder.start();
        } catch (IOException e) {
            fail("Failed to start CLI process: " + e.getLocalizedMessage());
        }

        final InputStream cliStream = cliProc.getInputStream();
        final StringBuilder cliOutBuf = new StringBuilder();
        boolean wait = true;
        int runningTime = 0;
        int exitCode = 0;
        do {
            try {
                Thread.sleep(STATUS_CHECK_INTERVAL);
            } catch (InterruptedException e) {
            }
            runningTime += STATUS_CHECK_INTERVAL;
            readStream(cliOutBuf, cliStream);
            try {
                exitCode = cliProc.exitValue();
                wait = false;
                readStream(cliOutBuf, cliStream);
            } catch(IllegalThreadStateException e) {
                // cli still working
            }
            if(runningTime >= CLI_PROC_TIMEOUT) {
                readStream(cliOutBuf, cliStream);
                cliProc.destroy();
                wait = false;
            }
        } while(wait);

        cliOutput = cliOutBuf.toString();

        if (logFailure && exitCode != 0) {
            System.out.println("Failed to execute '" + cmd + "'");
            System.out.println("Command's output: '" + cliOutput + "'");
            try {
                int bytesTotal = cliProc.getErrorStream().available();
                if (bytesTotal > 0) {
                    final byte[] bytes = new byte[bytesTotal];
                    cliProc.getErrorStream().read(bytes);
                    System.out.println("Command's error log: '" + new String(bytes) + "'");
                } else {
                    System.out.println("No output data for the command.");
                }
            } catch (IOException e) {
                fail("Failed to read command's error output: " + e.getLocalizedMessage());
            }
        }
        return exitCode;
    }

    protected void readStream(final StringBuilder cliOutBuf, InputStream cliStream) {
        try {
            int bytesTotal = cliStream.available();
            if (bytesTotal > 0) {
                final byte[] bytes = new byte[bytesTotal];
                cliStream.read(bytes);
                cliOutBuf.append(new String(bytes));
            }
        } catch (IOException e) {
            fail("Failed to read command's output: " + e.getLocalizedMessage());
        }
    }
}