/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management.util;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;

/**
 * CLI executor with custom configuration file jboss-cli.xml Used for testing
 * two-way SSL connection
 *
 * @author Filip Bogyai
 */
public class CustomCLIExecutor {

    public static final int MANAGEMENT_NATIVE_PORT = 9999;
    public static final int MANAGEMENT_HTTP_PORT = 9990;
    public static final int MANAGEMENT_HTTPS_PORT = 9443;

    private static Logger LOGGER = Logger.getLogger(CustomCLIExecutor.class);
    private static final int CLI_PROC_TIMEOUT = 5000;
    private static final int STATUS_CHECK_INTERVAL = 2000;

    public static String execute(File cliConfigFile, String operation) {
        return execute(cliConfigFile, operation, (List<String>)null, (Map<String, String>)null);
    }

    public static String execute(File cliConfigFile, String operation, List<String> parameters, Map<String, String> environment) {

        String defaultController = TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getServerPort();
        return execute(cliConfigFile, operation, defaultController, false, parameters, environment);
    }

    public static String execute(File cliConfigFile, String operation, String controller) {

        return execute(cliConfigFile, operation, controller, false);
    }

    public static String execute(File cliConfigFile, String operation, String controller, boolean logFailure) {
        return execute(cliConfigFile, operation, controller, logFailure, (List<String>)null, (Map<String, String>)null);
    }

    /**
     * Externally executes CLI operation with cliConfigFile settings and checks
     * expectedStatusCode
     *
     * @return String cliOutput
     */
    public static String execute(File cliConfigFile, String operation, String controller, boolean logFailure,
            List<String> parameters, Map<String, String> environment) {

        String cliOutput;
        String jbossDist = System.getProperty("jboss.dist");
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
        command.add(TestSuiteEnvironment.getJavaPath());
        TestSuiteEnvironment.getIpv6Args(command);
        if (cliConfigFile != null) {
            command.add("-Djboss.cli.config=" + cliConfigFile.getAbsolutePath());
        } else {
            command.add("-Djboss.cli.config=" + jbossDist + File.separator + "bin" + File.separator + "jboss-cli.xml");
        }
        command.add("-jar");
        command.add(jbossDist + File.separatorChar + "jboss-modules.jar");
        command.add("-mp");
        command.add(modulePath);
        command.add("org.jboss.as.cli");
        command.add("-c");
        command.add("--controller=" + controller);

        if (parameters!=null) {
            for (String param : parameters) {
                command.add(param);
            }
        }

        if (environment!=null) {
            Map<String, String> builderEnvironment = builder.environment();
            builderEnvironment.putAll(environment);
        }

        command.add(operation);
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
            } catch (IllegalThreadStateException e) {
                // cli still working
            }
            if (runningTime >= CLI_PROC_TIMEOUT) {
                readStream(cliOutBuf, cliStream);
                cliProc.destroy();
                wait = false;
            }
        } while (wait);

        cliOutput = cliOutBuf.toString();

        if (logFailure && exitCode != 0) {
            LOGGER.info("Command's output: '" + cliOutput + "'");
            try {
                int bytesTotal = cliProc.getErrorStream().available();
                if (bytesTotal > 0) {
                    final byte[] bytes = new byte[bytesTotal];
                    cliProc.getErrorStream().read(bytes);
                    LOGGER.info("Command's error log: '" + new String(bytes) + "'");
                } else {
                    LOGGER.info("No output data for the command.");
                }
            } catch (IOException e) {
                fail("Failed to read command's error output: " + e.getLocalizedMessage());
            }
        }
        return exitCode + ": " + cliOutput;
    }

    private static void readStream(final StringBuilder cliOutBuf, InputStream cliStream) {
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

    public static void waitForServerToReload(int timeout, File cliConfigFile) throws Exception {
        waitForServerToReloadWithUserAndPassword(timeout, cliConfigFile, null, null);
    }

    public static void waitForServerToReloadWithUserAndPassword(int timeout, File cliConfigFile, String user, String password) throws Exception {

        Thread.sleep(TimeoutUtil.adjust(500));
        long start = System.currentTimeMillis();
        long now;
        do {
            try {
                String result = null;
                if (user!=null && password!=null) {
                    List<String> params = new ArrayList<String>();
                    params.add("--user=" + user);
                    params.add("--password=" + password);
                    result = CustomCLIExecutor.execute(cliConfigFile, READ_ATTRIBUTE_OPERATION + " server-state", params, null);
                } else {
                    result = CustomCLIExecutor.execute(cliConfigFile, READ_ATTRIBUTE_OPERATION + " server-state");
                }
                boolean normal = result.contains("running");
                if (normal) {
                    return;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            now = System.currentTimeMillis();
        } while (now - start < timeout);

        fail("Server did not reload in the imparted time.");
    }
}
