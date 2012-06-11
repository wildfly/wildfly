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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 * @author Alexey Loubyansky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CliArgumentsTestCase extends CliScriptTestBase {

    private static final String tempDir = System.getProperty("java.io.tmpdir");

    @Test
    public void testVersionArgument() throws Exception {
        execute(false, "--version");
        final String result = getLastCommandOutput();
        assertNotNull(result);
        assertTrue(result, result.contains("JBOSS_HOME"));
        assertTrue(result, result.contains("JBoss AS release"));
        assertTrue(result, result.contains("JAVA_HOME"));
        assertTrue(result, result.contains("java.version"));
        assertTrue(result, result.contains("java.vm.vendor"));
        assertTrue(result, result.contains("java.vm.version"));
        assertTrue(result, result.contains("os.name"));
        assertTrue(result, result.contains("os.version"));
    }

    @Test
    public void testVersionAsCommandArgument() throws Exception {
        execute(false, "--command=version");
        final String result = getLastCommandOutput();
        assertNotNull(result);
        assertTrue(result, result.contains("JBOSS_HOME"));
        assertTrue(result, result.contains("JBoss AS release"));
        assertTrue(result, result.contains("JAVA_HOME"));
        assertTrue(result, result.contains("java.version"));
        assertTrue(result, result.contains("java.vm.vendor"));
        assertTrue(result, result.contains("java.vm.version"));
        assertTrue(result, result.contains("os.name"));
        assertTrue(result, result.contains("os.version"));
    }

    @Test
    public void testFileArgument() throws Exception {

        // prepare file
        File cliScriptFile = new File(tempDir, "testScript.cli");
        if (cliScriptFile.exists()) Assert.assertTrue(cliScriptFile.delete());
        FileUtils.writeStringToFile(cliScriptFile, "version" + System.getProperty("line.separator"));

        // pass it to CLI
        execute(false, "--file=" + cliScriptFile.getAbsolutePath());
        final String result = getLastCommandOutput();
        assertNotNull(result);
        assertTrue(result, result.contains("JBOSS_HOME"));
        assertTrue(result, result.contains("JBoss AS release"));
        assertTrue(result, result.contains("JAVA_HOME"));
        assertTrue(result, result.contains("java.version"));
        assertTrue(result, result.contains("java.vm.vendor"));
        assertTrue(result, result.contains("java.vm.version"));
        assertTrue(result, result.contains("os.name"));
        assertTrue(result, result.contains("os.version"));

        cliScriptFile.delete();
    }

    @Test
    public void testConnectArgument() throws Exception {
        execute(false, "--commands=connect,version,ls");

        final String result = getLastCommandOutput();
        assertNotNull(result);
        assertTrue(result, result.contains("JBOSS_HOME"));
        assertTrue(result, result.contains("JBoss AS release"));
        assertTrue(result, result.contains("JAVA_HOME"));
        assertTrue(result, result.contains("java.version"));
        assertTrue(result, result.contains("java.vm.vendor"));
        assertTrue(result, result.contains("java.vm.version"));
        assertTrue(result, result.contains("os.name"));
        assertTrue(result, result.contains("os.version"));

        assertTrue(result.contains("subsystem"));
        assertTrue(result.contains("extension"));
    }

    @Test
    public void testWrongControler() throws Exception {
        int exitCode = execute(TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort() - 1, true, "quit", true);
        assertTrue(exitCode != 0);
    }
}
