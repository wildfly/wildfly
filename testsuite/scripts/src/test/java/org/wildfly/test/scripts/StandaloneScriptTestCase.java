/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.scripts;

import static org.wildfly.test.common.ServerHelper.DEFAULT_EXPECTED_INPUT_ARGS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.test.common.ServerHelper;

@RunWith(Parameterized.class)
public class StandaloneScriptTestCase extends ScriptTestCase {
    private static final String STANDALONE_BASE_NAME = "standalone";

    @Parameter
    public Map<String, String> env;

    private static final Function<ModelControllerClient, Boolean> STANDALONE_CHECK = ServerHelper::isStandaloneRunning;

    public StandaloneScriptTestCase() {
        super(STANDALONE_BASE_NAME);
    }

    @Parameters
    public static Collection<Object> data() {
        return List.of(
                Map.of(),
                Map.of("GC_LOG", "true")
        );
    }

    @Override
    void testScript(final ScriptProcess script) throws InterruptedException, TimeoutException, IOException {
        List<String> args = new ArrayList<>(Arrays.asList(ServerHelper.DEFAULT_SERVER_JAVA_OPTS));
        args.add("-c");
        args.add("standalone-full-ha.xml");
        script.start(STANDALONE_CHECK, env, args.toArray(String[]::new));

        ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        ServerHelper.checkBootErrors(client, PathAddress.EMPTY_ADDRESS);
        ServerHelper.checkInputArgs(client, PathAddress.EMPTY_ADDRESS, DEFAULT_EXPECTED_INPUT_ARGS);

        Assert.assertNotNull("The process is null and may have failed to start.", script);
        Assert.assertTrue("The process is not running and should be", script.isAlive());

        final var stdout = script.getStdoutAsString();
        Assert.assertFalse("Did not expect to find -Djava.security.manager=allow in the JVM parameters.", stdout.contains("-Djava.security.manager=allow"));

        // Shutdown the server
        @SuppressWarnings("Convert2Lambda")
        final Callable<ModelNode> callable = new Callable<ModelNode>() {
            @Override
            public ModelNode call() throws Exception {
                try (ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient()) {
                    return executeOperation(client, Operations.createOperation("shutdown"));
                }
            }
        };
        execute(callable);
        validateProcess(script);

        // Check if the gc.log exists assuming we have the GC_LOG environment variable added
        if ("true".equals(env.get("GC_LOG"))) {
            final Path logDir = script.getContainerHome().resolve("standalone").resolve("log");
            Assert.assertTrue(Files.exists(logDir));
            final String fileName = "gc.log";
            Assert.assertTrue(String.format("Missing %s file in %s", fileName, logDir), Files.exists(logDir.resolve(fileName)));
        }
    }
}
