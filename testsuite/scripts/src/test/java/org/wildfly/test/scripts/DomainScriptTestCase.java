/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.scripts;

import static org.wildfly.test.common.ServerHelper.DEFAULT_EXPECTED_INPUT_ARGS;

import java.io.IOException;
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
import org.wildfly.test.common.ServerHelper;

@RunWith(Parameterized.class)
public class DomainScriptTestCase extends ScriptTestCase {

    private static final Function<ModelControllerClient, Boolean> HOST_CONTROLLER_CHECK = ServerHelper::isDomainRunning;

    private static final PathAddress PRIMARY_HOST = PathAddress.pathAddress("host", "primary");

    @Parameterized.Parameter
    public Map<String, String> env;
    public DomainScriptTestCase() {
        super("domain");
    }

    @Parameterized.Parameters
    public static Collection<Object> data() {
        return List.of(
                Map.of()
        );
    }

    @Override
    void testScript(final ScriptProcess script) throws InterruptedException, TimeoutException, IOException {
        List<String> args = new ArrayList<>(Arrays.asList(ServerHelper.DEFAULT_SERVER_JAVA_OPTS));
        args.add("-c");
        args.add("domain.xml");
        args.add("--host-config");
        args.add("host-primary.xml");
        script.start(HOST_CONTROLLER_CHECK, env, args.toArray(String[]::new));

        Assert.assertNotNull("The process is null and may have failed to start.", script);
        Assert.assertTrue("The process is not running and should be", script.isAlive());

        final var stdout = script.getStdoutAsString();
        Assert.assertFalse("Did not expect to find -Djava.security.manager=allow in the JVM parameters.", stdout.contains("-Djava.security.manager=allow"));

        ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        ServerHelper.checkBootErrors(client, PRIMARY_HOST);
        ServerHelper.checkInputArgs(client, PRIMARY_HOST, DEFAULT_EXPECTED_INPUT_ARGS);

        // Shutdown the server
        @SuppressWarnings("Convert2Lambda")
        final Callable<ModelNode> callable = new Callable<ModelNode>() {
            @Override
            public ModelNode call() throws Exception {
                try (ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient()) {
                    return executeOperation(client, Operations.createOperation("shutdown", ServerHelper.determineHostAddress(client)));
                }
            }
        };
        execute(callable);
        validateProcess(script);
    }

}
