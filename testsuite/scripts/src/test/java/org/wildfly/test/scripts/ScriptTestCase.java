/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.scripts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.test.common.ServerConfigurator;
import org.wildfly.test.common.ServerHelper;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class ScriptTestCase {

    static final Map<String, String> MAVEN_JAVA_OPTS = new LinkedHashMap<>();

    private final String scriptBaseName;
    private final boolean testCommonConfOnly;
    private ExecutorService service;


    ScriptTestCase(final String scriptBaseName) {
        this(scriptBaseName, false);
    }

    ScriptTestCase(final String scriptBaseName, final boolean testCommonConfOnly) {
        this.scriptBaseName = scriptBaseName;
        this.testCommonConfOnly = testCommonConfOnly;
    }

    @BeforeClass
    public static void configureEnvironment() throws Exception {
        final String localRepo = System.getProperty("maven.repo.local");
        if (localRepo != null) {
            MAVEN_JAVA_OPTS.put("JAVA_OPTS", "-Dmaven.repo.local=" + localRepo);
        }
        ServerConfigurator.configure();
    }

    @Before
    public void setup() {
        service = Executors.newCachedThreadPool();
    }

    @After
    public void cleanup() {
        service.shutdownNow();
    }

    @Test
    public void testBatchScript() throws Exception {
        Assume.assumeTrue(Shell.BATCH.isSupported());
        executeTests(Shell.BATCH);
    }

    @Test
    public void testPowerShellScript() throws Exception {
        Assume.assumeTrue(TestSuiteEnvironment.isWindows() && Shell.POWERSHELL.isSupported());
        executeTests(Shell.POWERSHELL);
    }

    @Test
    public void testBashScript() throws Exception {
        Assume.assumeTrue(!TestSuiteEnvironment.isWindows() && Shell.BASH.isSupported());
        executeTests(Shell.BASH);
    }

    @Test
    public void testDashScript() throws Exception {
        Assume.assumeTrue(!TestSuiteEnvironment.isWindows() && Shell.DASH.isSupported());
        executeTests(Shell.DASH);
    }

    @Test
    public void testKshScript() throws Exception {
        Assume.assumeTrue(!TestSuiteEnvironment.isWindows() && Shell.KSH.isSupported());
        executeTests(Shell.KSH);
    }

    void testScript(final ScriptProcess script) throws InterruptedException, TimeoutException, IOException {
        // Just test the help, there's not much more we should do here unless we start a standalone server
        script.start(MAVEN_JAVA_OPTS, "-h");
        Assert.assertNotNull("The process is null and may have failed to start.", script);

        validateProcess(script);

        // Simply check for the "usage"
        boolean missing = true;
        for (String line : script.getStdout()) {
            if (line.startsWith("usage:")) {
                missing = false;
                break;
            }
        }
        if (missing) {
            Assert.fail(script.getErrorMessage("Expected the \"usage:\" to be present"));
        }
    }

    void validateProcess(final ScriptProcess script) throws InterruptedException {
        if (script.waitFor(ServerHelper.TIMEOUT, TimeUnit.SECONDS)) {
            // The script has exited, validate the exit code is valid
            final int exitValue = script.exitValue();
            if (exitValue != 0) {
                Assert.fail(script.getErrorMessage(String.format("Expected an exit value 0f 0 got %d", exitValue)));
            }
        } else {
            Assert.fail(script.getErrorMessage("The script process did not exit within " + ServerHelper.TIMEOUT + " seconds."));
        }
    }

    private void executeTests(final Shell shell) throws InterruptedException, IOException, TimeoutException {
        for (Path path : ServerConfigurator.PATHS) {
            try (ScriptProcess script = new ScriptProcess(path, scriptBaseName, shell, ServerHelper.TIMEOUT)) {
                if (!testCommonConfOnly) {
                    testScript(script);
                    script.close();
                }
                testCommonConf(script, shell);
            }
        }
    }

    private void testCommonConf(final ScriptProcess script, final Shell shell) throws InterruptedException, IOException, TimeoutException {
        testCommonConf(script, true, shell);
        testCommonConf(script, false, shell);
    }

    private void testCommonConf(final ScriptProcess script, final boolean useEnvVar, final Shell shell) throws InterruptedException, IOException, TimeoutException {
        final Map<String, String> env = new HashMap<>();
        final Path confFile;
        if (useEnvVar) {
            confFile = Paths.get(TestSuiteEnvironment.getTmpDir(), "test-common" + shell.getConfExtension());
            env.put("COMMON_CONF", confFile.toString());
        } else {
            confFile = script.getContainerHome().resolve("bin").resolve("common" + shell.getConfExtension());
        }
        // Create the common conf file which will simply echo some text and then exit the script
        final String text = "Test from common configuration to " + confFile.getFileName().toString();
        try (BufferedWriter writer = Files.newBufferedWriter(confFile, StandardCharsets.UTF_8)) {
            if (shell == Shell.POWERSHELL) {
                writer.write("Write-Output \"");
                writer.write(text);
                writer.write('"');
                writer.newLine();
                writer.write("break");
            } else {
                writer.write("echo \"");
                writer.write(text);
                writer.write('"');
                writer.newLine();
                writer.write("exit");
            }
            writer.newLine();
        }
        try {
            script.start(env);
            if (!script.waitFor(ServerHelper.TIMEOUT, TimeUnit.SECONDS)) {
                Assert.fail(script.getErrorMessage("Failed to exit script from " + confFile));
            }
            // Batch scripts print the quotes around the text
            final String expectedText = (shell == Shell.BATCH ? "\"" + text + "\"" : text);
            final List<String> lines = script.getStdout();
            Assert.assertEquals(script.getErrorMessage("There should only be one line logged before the script exited"),
                    1, lines.size());
            Assert.assertEquals(expectedText, lines.get(0));
        } finally {
            script.close();
            Files.delete(confFile);
        }
    }
}
