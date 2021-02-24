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
        final int expectedLines = (script.getShell() == Shell.BATCH ? 3 : 2);
        Assert.assertEquals(script.getErrorMessage(String.format("Expected %d lines.", expectedLines)), expectedLines, lines.size());
    }
}
