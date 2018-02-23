/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.scripts;

import java.util.HashMap;
import java.util.Map;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertTrue;

/**
 * Provide a test case for script wsprovide that reside under JBOSS_HOME/bin
 * This basically verifies all dependencies are met to run the shell scripts.
 */
public class WSProvideScriptTestCase extends ScriptUtil {

    @Test
    @RunAsClient
    public void testWSProvideFromCommandLine() throws Exception {

        Path scriptFile = Paths.get(JBOSS_HOME, "bin", "wsprovide" + EXT).normalize();

        StringBuilder command = new StringBuilder();
        command.append(scriptFile.toFile().getAbsolutePath());
        command.append(" -k -c ");
        command.append(ABSOUTPUT);
        command.append(" -o ");
        command.append(ABSOUTPUT);
        command.append(" ");
        command.append(ENDPOINT_CLASS);

        Map<String, String> env = new HashMap<>();
        env.put("JAVA_OPTS", "-Djava.security.policy=" + SECURITY_POLICY);

        // no execute permissions are scripts.  Run accordingly
        String result = "";
        if (".sh".equals(EXT)) {
            result = executeCommand("sh " + command.toString(), null, "wsprovide", env);
        } else {
            result = executeCommand(command.toString(), null, "wsprovide", env);
        }

        Path javaSource = Paths.get(ABSOUTPUT, "org", "jboss", "as", "testsuite",
            "integration", "scripts", "test", "tools", "jaxws", "EchoPlus1.java");

        // rls debug
        StringBuilder sb = new StringBuilder();
        sb.append("Input CMD: ");
        sb.append(command.toString());
        sb.append("\n");
        sb.append("CMD Output: " + result);
        sb.append("\n");
        sb.append("Expected File: " + javaSource.toFile().getAbsolutePath());
        sb.append("\n");

        assertTrue(sb.toString(), javaSource.toFile().exists());  // rls debug
        assertTrue(sb.toString(), false);  // rls debug
    }
}