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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author Alexey Loubyansky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class FileWithPropertiesTestCase {

    private static final int CLI_PROC_TIMEOUT = 10000;
    private static final int STATUS_CHECK_INTERVAL = 2000;

    private static final String SERVER_PROP_NAME = "cli-arg-test";
    private static final String CLI_PROP_NAME = "cli.prop.name";
    private static final String CLI_PROP_VALUE = "cli.prop.value";
    private static final String HOST_PROP_NAME = "cli.host.name";
    private static final String HOST_PROP_VALUE = TestSuiteEnvironment.getServerAddress();
    private static final String PORT_PROP_NAME = "cli.port.name";
    private static final String PORT_PROP_VALUE = String.valueOf(TestSuiteEnvironment.getServerPort());
    private static final String CONNECT_COMMAND = "connect ${" + HOST_PROP_NAME + "}:${" + PORT_PROP_NAME + "}";
    private static final String SET_PROP_COMMAND = "/system-property=" + SERVER_PROP_NAME + ":add(value=${cli.prop.name})";
    private static final String GET_PROP_COMMAND = "/system-property=" + SERVER_PROP_NAME + ":read-resource";
    private static final String REMOVE_PROP_COMMAND = "/system-property=" + SERVER_PROP_NAME + ":remove";

    private static final String SCRIPT_NAME = "jboss-cli-file-arg-test.cli";
    private static final String PROPS_NAME = "jboss-cli-file-arg-test.properties";
    private static final File SCRIPT_FILE;
    private static final File PROPS_FILE;
    private static final File TMP_JBOSS_CLI_FILE;
    static {
        SCRIPT_FILE = new File(new File(TestSuiteEnvironment.getTmpDir()), SCRIPT_NAME);
        PROPS_FILE = new File(new File(TestSuiteEnvironment.getTmpDir()), PROPS_NAME);
        TMP_JBOSS_CLI_FILE = new File(new File(TestSuiteEnvironment.getTmpDir()), "tmp-jboss-cli.xml");
    }

    @BeforeClass
    public static void setup() {
        ensureRemoved(TMP_JBOSS_CLI_FILE);
        jbossDist = TestSuiteEnvironment.getSystemProperty("jboss.dist");
        if(jbossDist == null) {
            fail("jboss.dist system property is not set");
        }
        final File jbossCliXml = new File(jbossDist, "bin" + File.separator + "jboss-cli.xml");
        if(!jbossCliXml.exists()) {
            fail(jbossCliXml + " doesn't exist.");
        }
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(jbossCliXml));
            writer = new BufferedWriter(new FileWriter(TMP_JBOSS_CLI_FILE));
            String line = reader.readLine();
            boolean replaced = false;
            while(line != null) {
                if(!replaced) {
                    final int i = line.indexOf("<resolve-parameter-values>false</resolve-parameter-values>");
                    if(i >= 0) {
                        line = line.substring(0, i) + "<resolve-parameter-values>true</resolve-parameter-values>" +
                               line.substring(i + "<resolve-parameter-values>false</resolve-parameter-values>".length());
                        replaced = true;
                    }
                }
                writer.write(line);
                writer.newLine();
                line = reader.readLine();
            }
            if(!replaced) {
                fail(jbossCliXml.getAbsoluteFile() + " doesn't contain resolve-parameter-values element set to false.");
            }
        } catch(IOException e) {
            fail(e.getLocalizedMessage());
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {}
            }
            if(writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {}
            }
        }

        ensureRemoved(SCRIPT_FILE);
        ensureRemoved(PROPS_FILE);
        try {
            writer = new BufferedWriter(new FileWriter(SCRIPT_FILE));
            writer.write(CONNECT_COMMAND);
            writer.newLine();
            writer.write(SET_PROP_COMMAND);
            writer.newLine();
            writer.write(GET_PROP_COMMAND);
            writer.newLine();
            writer.write(REMOVE_PROP_COMMAND);
            writer.newLine();
        } catch (IOException e) {
            fail("Failed to write to " + SCRIPT_FILE.getAbsolutePath() + ": " + e.getLocalizedMessage());
        } finally {
            if(writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }

        writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(PROPS_FILE));
            writer.write(CLI_PROP_NAME); writer.write('='); writer.write(CLI_PROP_VALUE); writer.newLine();
            writer.write(HOST_PROP_NAME); writer.write('='); writer.write(HOST_PROP_VALUE); writer.newLine();
            writer.write(PORT_PROP_NAME); writer.write('='); writer.write(PORT_PROP_VALUE); writer.newLine();
        } catch (IOException e) {
            fail("Failed to write to " + PROPS_FILE.getAbsolutePath() + ": " + e.getLocalizedMessage());
        } finally {
            if(writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @AfterClass
    public static void cleanUp() {
        ensureRemoved(SCRIPT_FILE);
        ensureRemoved(PROPS_FILE);
        ensureRemoved(TMP_JBOSS_CLI_FILE);
    }

    protected static void ensureRemoved(File f) {
        if(f.exists()) {
            if(!f.delete()) {
                fail("Failed to delete " + f.getAbsolutePath());
            }
        }
    }

    private String cliOutput;
    private static String jbossDist;

    /**
     * Ensures that system properties are resolved by the CLI.
     * @throws Exception
     */
    @Test
    public void testResolved() {
        assertEquals(0, execute(true, true));
        assertNotNull(cliOutput);
        assertEquals(CLI_PROP_VALUE, getValue("value"));
    }

    /**
     * Ensures that system properties are not resolved by the CLI.
     * @throws Exception
     */
    @Test
    public void testNotresolved() {
        assertEquals(1, execute(false, true));
        assertNotNull(cliOutput);
        assertEquals("failed", getValue("outcome"));
        assertTrue(getValue("failure-description").contains("WFLYCTL0211"));
    }

    protected String getValue(final String value) {
        final String valuePrefix = "\"" + value + "\" => \"";
        int i = cliOutput.indexOf(valuePrefix, 0);
        if(i < 0) {
            fail("The output doesn't contain '" + value + "': " + cliOutput);
        }
        int endQuote = cliOutput.indexOf('\"', valuePrefix.length() + i);
        if(endQuote < 0) {
            fail("The output doesn't contain '" + value + "': " + cliOutput);
        }

        return cliOutput.substring(i + valuePrefix.length(), endQuote);
    }

    protected int execute(boolean resolveProps, boolean logFailure) {
        if(jbossDist == null) {
            fail("jboss.dist system property is not set");
        }
        final String modulePath = TestSuiteEnvironment.getSystemProperty("module.path");
        if(modulePath == null) {
            fail("module.path system property is not set");
        }

        final ProcessBuilder builder = new ProcessBuilder();
        builder.redirectErrorStream(true);
        final List<String> command = new ArrayList<String>();
        command.add(TestSuiteEnvironment.getJavaPath());
        TestSuiteEnvironment.getIpv6Args(command);
        if(resolveProps) {
            command.add("-Djboss.cli.config=" + TMP_JBOSS_CLI_FILE.getAbsolutePath());
        } else {
            command.add("-Djboss.cli.config=" + jbossDist + File.separator + "bin" + File.separator + "jboss-cli.xml");
        }
        command.add("-jar");
        command.add(jbossDist + File.separatorChar + "jboss-modules.jar");
        command.add("-mp");
        command.add(modulePath);
        command.add("org.jboss.as.cli");
        //command.add("-c");
        //command.add("--controller=" + TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getServerPort());
        command.add("--file=" + SCRIPT_FILE.getAbsolutePath());
        command.add("--properties=" + PROPS_FILE.getAbsolutePath());
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
