/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.scripts;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class AppClientScriptTestCase extends ScriptTestCase {

    public AppClientScriptTestCase() {
        super("appclient");
    }

    @Override
    void testScript(final ScriptProcess script) throws InterruptedException, TimeoutException, IOException {
        script.start(MAVEN_JAVA_OPTS, "-v");
        Assert.assertNotNull("The process is null and may have failed to start.", script);

        validateProcess(script);

        final List<String> lines = script.getStdout();
        int count = 2;
        for (String stdout : lines) {
            if (stdout.startsWith("Picked up")) {
                count += 1;
            }
        }
        final int expectedLines = (script.getShell() == Shell.BATCH ? 3 : count );
        Assert.assertEquals(script.getErrorMessage(String.format("Expected %d lines.", expectedLines)), expectedLines,
                lines.size());
    }
}
