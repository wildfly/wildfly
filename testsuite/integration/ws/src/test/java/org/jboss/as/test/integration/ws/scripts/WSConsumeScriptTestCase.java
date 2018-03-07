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
 * Provide a test case for script wsconsume that reside under JBOSS_HOME/bin
 * This basically verifies all dependencies are met to run the shell scripts.
 */
public class WSConsumeScriptTestCase extends ScriptUtil {

    @Test
    @RunAsClient
    public void testWSConsumeFromCommandLine() throws Exception {
        Path WSDL_LOCATION_PATH = Paths.get("ws",  "scripts", "TestServiceCatalog.wsdl");

        // use absolute path for the output to be re-usable
        String absWsdlLoc = getResourceFile(WSDL_LOCATION_PATH).toFile().getAbsolutePath();

        Path scriptFile = Paths.get(JBOSS_HOME,  "bin", "wsconsume" + EXT).normalize();

        StringBuilder command = new StringBuilder();
        command.append(scriptFile.toFile().getAbsolutePath());
        command.append(" -v -k -o ");
        command.append(ABSOUTPUT);
        command.append(" ");
        command.append(absWsdlLoc);

        Map<String, String> env = new HashMap<>();
        env.put("JAVA_OPTS", "-Djava.security.policy=" + SECURITY_POLICY);

        // no execute permissions are scripts.  Run accordingly
        String result = "";
        if (".sh".equals(EXT)) {
            result = executeCommand("sh " + command.toString(), null, "wsconsume", env);
        } else {
            result = executeCommand(command.toString(), null, "wsconsume", env);
        }

        Path javaSource = Paths.get(TEST_DIR, "org", "openuri", "_2004",
            "_04", "helloworld", "EndpointInterface.java");

        StringBuilder sb = new StringBuilder();
        sb.append("MAVEN_REPO_LOCAL: " + MAVEN_REPO_LOCAL + "\n");
        sb.append("Input CMD: ");
        sb.append(command.toString());
        sb.append("\n");
        sb.append("CMD Output: " + result);
        sb.append("\n");
        sb.append("Expected File: " + javaSource.toFile().getAbsolutePath());
        sb.append("\n");

        assertTrue(sb.toString(), javaSource.toFile().exists());
        assertTrue(sb.toString(), false); //debug
    }
}
