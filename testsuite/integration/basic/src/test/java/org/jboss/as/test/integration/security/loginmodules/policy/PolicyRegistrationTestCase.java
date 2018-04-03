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

package org.jboss.as.test.integration.security.loginmodules.policy;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Tests if NameNotFoundException due to policyRegistration occurs after logging into deployed secured ejb
 * Test for [ JBEAP-13973 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@ServerSetup({ SecurityTraceServerSetupTask.class })
@RunAsClient
public class PolicyRegistrationTestCase {

    private static final String SECURED_EJB_DEPLOYMENT = "secured-ejb";

    @Deployment(name = SECURED_EJB_DEPLOYMENT, testable = false)
    public static Archive<?> createSecuredEjbDeployment() throws IOException {
        WebArchive war = ShrinkWrap.create(WebArchive.class, SECURED_EJB_DEPLOYMENT + ".war");
        war.addClasses(PolicyRegistrationTestCase.class, SecuredEJBServlet.class, SecuredEJB.class);
        war.setWebXML(Utils.getResource("org/jboss/as/test/integration/security/loginmodules/policy/WEB-INF/web.xml"));
        war.addAsResource(PolicyRegistrationTestCase.class.getPackage(), "jboss-ejb-client.properties", "jboss-ejb-client.properties");
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL index = tccl.getResource("index.html");
        war.addAsWebResource(index, "index.html");
        return war;
    }

    /**
     * Asserts the server.log file does not contain NameNotFoundException
     *
     * @throws IOException
     */
    @AfterClass
    public static final void after() throws IOException {
        File logFile = new File(SecurityTraceServerSetupTask.SERVER_LOG_DIR_VALUE, "server.log");
        Assert.assertTrue("Log file " + logFile + " should exist", logFile.exists());
        String logContent = new String(Files.readAllBytes(Paths.get(logFile.getPath())), StandardCharsets.UTF_8);
        Assert.assertFalse(logContent.contains("javax.naming.NameNotFoundException: policyRegistration"));
    }

    @Test
    @OperateOnDeployment(SECURED_EJB_DEPLOYMENT)
    public void test(@ArquillianResource URL url) throws Exception {
        HttpGet httpget = new HttpGet(url.toString() + "SecuredEJBServlet");
        final String userpassword = "user1" + ":" + "password1";
        final String headerValue = java.util.Base64.getEncoder().encodeToString(userpassword.getBytes());

        Assert.assertNotNull(headerValue);
        httpget.addHeader("Authorization", "Basic " + headerValue);

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            httpclient.execute(httpget);
        } catch (IOException e) {
            throw new RuntimeException("Servlet response IO exception", e);
        }
    }
}
