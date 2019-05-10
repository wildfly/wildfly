/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.jndi.logging;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Automated test for [ WFLY-11848 ] - Tests if JNDI bindings is correctly built in case there is no appName.
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JNDIBindingsNoAppNameTestCase {

    private static final String JAR_NAME = "ejb-jndi";
    private static String HOST = TestSuiteEnvironment.getServerAddress();
    private static int PORT = TestSuiteEnvironment.getHttpPort();

    @Deployment
    public static JavaArchive createJar() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        jar.addClasses(JNDIBindingsNoAppNameTestCase.class, Hello.class, HelloBean.class);
        return jar;
    }

    @Test
    public void testJNDIBindingsNoAppName() throws Exception {
        boolean passed = false;
        Context ctx = getInitialContext(HOST, PORT);
        Hello ejb = (Hello) ctx.lookup("ejb:/ejb-jndi/Hello!org.jboss.as.test.integration.ejb.jndi.logging.Hello");
        Assert.assertNotNull("Null object returned for local business interface lookup in the ejb namespace", ejb);
        List<String> lines = this.readServerLogLines();
        int i = 0;
        while (i < lines.size() && !passed) {
            String line = lines.get(i);
            if (line.contains("ejb:/ejb-jndi/Hello!org.jboss.as.test.integration.ejb.jndi.logging.Hello")) {
                passed = true;
            }
            i++;
        }
        Assert.assertTrue(passed);
    }

    private static Context getInitialContext(String host, Integer port)  throws NamingException {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        props.put(Context.PROVIDER_URL, String.format("%s://%s:%d", "remote+http", host, port));
        return new InitialContext(props);
    }

    private static List<String> readServerLogLines() {
        String jbossHome = System.getProperty("jboss.home");
        String logPath = String.format("%s%sstandalone%slog%sserver.log", jbossHome,
                (jbossHome.endsWith(File.separator) || jbossHome.endsWith("/")) ? "" : File.separator,
                File.separator, File.separator);
        logPath = logPath.replace('/', File.separatorChar);
        try {
            return Files.readAllLines(Paths.get(logPath));
        } catch (MalformedInputException e1) {
            try {
                return Files.readAllLines(Paths.get(logPath), StandardCharsets.ISO_8859_1);
            } catch (IOException e4) {
                throw new RuntimeException("Server logs has not standard Charsets (UTF8 or ISO_8859_1)");
            }
        } catch (IOException e) {
            // server.log file is not created, it is the same as server.log is empty
        }
        return new ArrayList<>();
    }
}
