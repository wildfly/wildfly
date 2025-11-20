/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.integration.elytron.realm;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.cli.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.URL;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@RunWith(Arquillian.class)
@RunAsClient
public class JaasCustomRealmTest {

    @ClassRule
    public static TemporaryFolder tmpDir = new TemporaryFolder();

    private static final String NAME = JaasCustomRealmTest.class.getSimpleName();

    /**
     * Creates WAR with a secured servlet and BASIC authentication configured in web.xml deployment descriptor.
     */
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war").addClasses(SimpleServlet.class)
                .addAsWebInfResource(JaasCustomRealmTest.class.getPackage(), NAME + "-web.xml", "web.xml")
                .addAsWebInfResource(JaasCustomRealmTest.class.getPackage(), "jaas-jboss-web.xml", "jaas-jboss-web.xml");
    }

    @BeforeClass
    public static void prepareSecurityRealmThatUsesJaasRealmViaCustomRealm() throws Exception {
        Assume.assumeTrue("Galleon layer does not provide org.wildfly.extension.elytron.jaas-realm module", System.getProperty("ts.layers") == null);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "testJaas.jar")
                .addAsResource(new StringAsset("Dependencies: org.wildfly.security"), "META-INF/MANIFEST.MF")
                .addClass(TestLoginModule.class)
                .addClass(TestCallbackHandler.class);
        File jarFile = new File(tmpDir.getRoot(), "testJaas.jar");
        jar.as(ZipExporter.class).exportTo(jarFile, true);
        CLIWrapper cli = new CLIWrapper(true, null, null, 10000);
        // ignore error on windows where the testJaas.jar might exist from previous run
        cli.sendLine("module add --name=jaasLoginModule "
                + " --resources=" + jarFile.getAbsolutePath()
                + " --dependencies=org.wildfly.security.elytron", true);
        cli.sendLine("/subsystem=elytron/custom-realm=customJaasRealm:add(module=org.wildfly.extension.elytron.jaas-realm ," +
                "class-name=org.wildfly.extension.elytron.JaasCustomSecurityRealmWrapper," +
                "configuration={entry=Entry1," +
                "module=jaasLoginModule," +
                "callback-handler=org.wildfly.test.integration.elytron.realm.TestCallbackHandler," +
                "path=" + JaasCustomRealmTest.class.getResource("jaas-login.config").getFile() + "})");
        cli.sendLine("/subsystem=elytron/security-domain=JAASSecurityDomain:add(" +
                "realms=[{realm=customJaasRealm}]," +
                "default-realm=customJaasRealm,permission-mapper=default-permission-mapper)");
        cli.sendLine("/subsystem=elytron/http-authentication-factory=example-fs-http-auth:add(http-server-mechanism-factory=global," +
                "security-domain=JAASSecurityDomain,mechanism-configurations=[{mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=customJaasRealm}]}])");
        cli.sendLine("/subsystem=undertow/application-security-domain=JAASSecurityDomain:add(http-authentication-factory=example-fs-http-auth)");
        cli.sendLine("reload");
    }

    @Test
    public void testUser1Allowed(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");
        Utils.makeCall(servletUrl.toURI(), SC_UNAUTHORIZED); // assert that auth is required
        Utils.makeCallWithBasicAuthn(servletUrl, "user1", "password1", SC_OK);
    }

    @Test
    public void testRoleRequired(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role2");
        Utils.makeCall(servletUrl.toURI(), SC_UNAUTHORIZED); // assert that auth is required
        Utils.makeCallWithBasicAuthn(servletUrl, "user1", "password1", SC_FORBIDDEN);
    }

    @Test
    public void testUser1NotAuthenticated(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role2");
        Utils.makeCall(servletUrl.toURI(), SC_UNAUTHORIZED); // assert that auth is required
        Utils.makeCallWithBasicAuthn(servletUrl, "user1", "wrong_pass", SC_UNAUTHORIZED);
    }

    @Test
    public void testUser2NotAuthenticated(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role2");
        Utils.makeCall(servletUrl.toURI(), SC_UNAUTHORIZED); // assert that auth is required
        Utils.makeCallWithBasicAuthn(servletUrl, "user2", "wrong_pass", SC_UNAUTHORIZED);
    }

    @Test
    public void testRoleUser2Allowed(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role2");
        Utils.makeCall(servletUrl.toURI(), SC_UNAUTHORIZED); // assert that auth is required
        Utils.makeCallWithBasicAuthn(servletUrl, "user2", "password2", SC_OK);
    }

    @Test
    public void testRoleUser2NotAllowed(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");
        Utils.makeCall(servletUrl.toURI(), SC_UNAUTHORIZED); // assert that auth is required
        Utils.makeCallWithBasicAuthn(servletUrl, "user2", "password2", SC_FORBIDDEN);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (System.getProperty("ts.layers") == null) {
            CLIWrapper cli = new CLIWrapper(true, null, null, 10000);
            cli.sendLine("/subsystem=undertow/application-security-domain=JAASSecurityDomain:remove");
            cli.sendLine("/subsystem=elytron/http-authentication-factory=example-fs-http-auth:remove");
            cli.sendLine("/subsystem=elytron/security-domain=JAASSecurityDomain:remove");
            cli.sendLine("/subsystem=elytron/custom-realm=customJaasRealm:remove");
            try {
                cli.sendLine("module remove --name=" + "jaasLoginModule");
            } catch (AssertionError e) {
                // ignore failure on Windows, cannot remove module on running server due to file locks
                if (!Util.isWindows())
                    throw e;
            }
            cli.sendLine("reload");
        }
    }
}
