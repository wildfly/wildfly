/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.scripts;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.test.common.ServerHelper;

@RunWith(Parameterized.class)
public class WsConsumeSecmgrScriptTestCase extends ScriptTestCase {

    @Parameter(0)
    public Map<String, String> env;

    @Parameter(1)
    public String[] args;

    @Parameter(2)
    public String expectedError;

    public WsConsumeSecmgrScriptTestCase() {
        super("wsconsume");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return List.of(
                new Object[] { Map.of("SECMGR", "true"), new String[0], "ERROR: The SECMGR option has been removed" },
                new Object[] { Map.of("JAVA_OPTS", "-Djava.security.manager"), new String[0], "ERROR: The use of -Djava.security.manager is not supported" },
                new Object[] { Map.of("JAVA_OPTS", "-Djava.security.manager=allow"), new String[0], "ERROR: The use of -Djava.security.manager is not supported" }
        );
    }

    @Override
    void testScript(final ScriptProcess script) throws InterruptedException, TimeoutException, IOException {
        script.start(env, args);
        try (script) {
            if (!script.waitFor(ServerHelper.TIMEOUT, TimeUnit.SECONDS)) {
                Assert.fail(script.getErrorMessage("Script did not exit after security manager error"));
            }
            Assert.assertEquals(script.getErrorMessage("Expected exit code 1"), 1, script.exitValue());
            final var stdout = script.getStdoutAsString();
            Assert.assertTrue("Expected error message containing '" + expectedError + "' for a server started with "
                            + script.getLastExecutedCmd() + "\nThe server output was: \n" + stdout,
                    stdout.contains(expectedError));
        }
    }
}
