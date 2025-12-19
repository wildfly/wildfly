/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.scripts;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jboss.as.test.shared.AssumeTestGroupUtil;
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
        // First check the standard script
        script.start(MAVEN_JAVA_OPTS, "-v");
        this.test(script);

        if (AssumeTestGroupUtil.isJDKVersionBefore(24)) {
            // Test with the security manager enabled
            script.start(MAVEN_JAVA_OPTS, "-v", "-secmgr");
            this.test(script);
        }
    }

    private void test(final ScriptProcess script) throws InterruptedException, IOException {
        try (script) {
            validateProcess(script);

            List<String> output = script.getStdout();
            // Prune JDK NOTEs, e.g. about JDK environment variable usage
            // Prune JDK WARNINGs, e.g. about deprecated method usage
            Set<String> jdkTokens = Set.of("NOTE: ", "WARNING: ");
            List<String> filteredOutput = output.stream()
                    // Remove ANSI colour escape sequences
                    .map(line -> line.replaceAll("\\e\\[\\d+(?:;\\d+)*m", ""))
                    // Filter out lines starting with JDK message tokens
                    .filter(line -> jdkTokens.stream().noneMatch(line::startsWith))
                    .toList();

            int expectedSize = ((script.getShell() == Shell.BATCH) ? 3 : 2);
            Assert.assertEquals(script.getErrorMessage(String.format("Expected %d lines", expectedSize)), expectedSize, filteredOutput.size());
        }
    }
}
