/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.http;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.Base64;

import org.jboss.as.cli.Util;

import static org.jboss.as.test.shared.CliUtils.asAbsolutePath;

/**
 * Tests for org.wildfly.security.http.util.RequestInformationCallbackMechanismFactory
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SetRequestInformationCallbackMechanismFactoryTestCase {

    private static final String NAME = SetRequestInformationCallbackMechanismFactoryTestCase.class.getSimpleName();

    @ClassRule
    public static TemporaryFolder tmpDir = new TemporaryFolder();

    private static final File EXCEPTION_OCCURED_IN_CUSTOM_REALM_FILE = Paths.get("target",
            SetRequestInformationCallbackMechanismFactoryTestCase.class.getSimpleName() + "-test.log").toFile();

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war")
                .addClasses(SimpleServlet.class)
                .addAsWebInfResource(CustomRealm.class.getPackage(), "SetRequestInformationCallbackTest-web.xml", "web.xml")
                .addAsWebInfResource(CustomRealm.class.getPackage(), "custom-realm-jboss-web.xml", "jboss-web.xml");
    }

    @BeforeClass
    public static void prepareSecurityRealmThatUsesJaasRealmViaCustomRealm() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "testJaas.jar")
                .addAsResource(new StringAsset("Dependencies: org.wildfly.security"), "META-INF/MANIFEST.MF")
                .addClass(CustomRealm.class);
        File jarFile = new File(tmpDir.getRoot(), "testJaas.jar");
        jar.as(ZipExporter.class).exportTo(jarFile, true);
        CLIWrapper cli = new CLIWrapper(true, null, null, 10000);

        cli.sendLine("module add --name=customRealmModule "
                + " --resources=" + jarFile.getAbsolutePath()
                + " --dependencies=org.wildfly.security.elytron", true);
        cli.sendLine("/subsystem=elytron/custom-realm=customRealm:add(module=customRealmModule,class-name=org.wildfly.test.integration.elytron.http.CustomRealm, configuration={pathToLogFile=" + asAbsolutePath(EXCEPTION_OCCURED_IN_CUSTOM_REALM_FILE) + "})");
        cli.sendLine("/subsystem=elytron/security-domain=RequestInfoApplicationDomain:add(realms=[{realm=customRealm}],default-realm=customRealm,permission-mapper=default-permission-mapper)");
        cli.sendLine("/subsystem=elytron/http-authentication-factory=example-fs-http-auth:add(http-server-mechanism-factory=global,security-domain=RequestInfoApplicationDomain,mechanism-configurations=[{mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=RequestInfoApplicationDomain}]}])");
        cli.sendLine("/subsystem=undertow/application-security-domain=RequestInfoDomain:add(http-authentication-factory=example-fs-http-auth)");
        cli.sendLine("reload");
    }

    @Test
    public void testRealmSuccessfulAuthenticationEventContainsInfo() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Authorization", getBasicAuthenticationHeader("myadmin", "mypassword"))
                .uri(new URI("http://localhost:8080/SetRequestInformationCallbackMechanismFactoryTestCase/SimpleServlet"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(200, response.statusCode());
        checkErrorDidNotOccur();
    }

    @Test
    public void testRealmFailedAuthenticationEventContainsInfo() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Authorization", getBasicAuthenticationHeader("wrongUser", "mypassword"))
                .uri(new URI("http://localhost:8080/SetRequestInformationCallbackMechanismFactoryTestCase/SimpleServlet"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(401, response.statusCode());
        checkErrorDidNotOccur();
    }

    @After
    public void cleanFile() {
        File errorFile = new File(asAbsolutePath(EXCEPTION_OCCURED_IN_CUSTOM_REALM_FILE));
        if (errorFile.exists()) {
            Assert.assertTrue(errorFile.delete());
        }
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        CLIWrapper cli = new CLIWrapper(true, null, null, 10000);
        cli.sendLine("/subsystem=undertow/application-security-domain=RequestInfoDomain:remove");
        cli.sendLine("/subsystem=elytron/http-authentication-factory=example-fs-http-auth:remove");
        cli.sendLine("/subsystem=elytron/security-domain=RequestInfoApplicationDomain:remove");
        cli.sendLine("/subsystem=elytron/custom-realm=customRealm:remove");
        try {
            cli.sendLine("module remove --name=" + "customRealmModule");
        } catch (AssertionError e) {
            // ignore failure on Windows, cannot remove module on running server due to file locks
            if (!Util.isWindows())
                throw e;
        }
        cli.sendLine("reload");

        File jarFile = new File(tmpDir.getRoot(), "testJaas.jar");
        if (jarFile.exists()) {
            jarFile.delete();
        }
    }

    private void checkErrorDidNotOccur() {
        File f = new File(asAbsolutePath(EXCEPTION_OCCURED_IN_CUSTOM_REALM_FILE));
        if (f.exists() && f.length() > 0) {
            Assert.fail("AuthenticationEvent did not contain expected information");
        }
    }

    private static String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }
}
