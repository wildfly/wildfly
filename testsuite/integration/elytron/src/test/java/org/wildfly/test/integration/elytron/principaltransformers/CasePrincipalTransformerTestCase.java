/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.elytron.principaltransformers;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import java.net.MalformedURLException;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.PropertyFileBasedDomain;


/**
 * Test case for 'case-principal-transformer' Elytron subsystem resource.
 *
 * @author Sonia Zaldana Calles <szaldana@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({CasePrincipalTransformerTestCase.SetupTask.class})
public class CasePrincipalTransformerTestCase {

    private static final String UPPER_CASE_SECURITY_DOMAIN = "upper-case-principal-domain";
    private static final String LOWER_CASE_SECURITY_DOMAIN = "lower-case-principal-domain";
    private static final String WITHOUT_TRANSFORMER_SECURITY_DOMAIN = "without-case-principal-domain";
    private static final String UPPER_USER = "USER1";
    private static final String LOWER_USER = "user1";
    private static final String PASSWORD = "password1";
    private static final String ROLE = "JBossAdmin";

    @Deployment(name = UPPER_CASE_SECURITY_DOMAIN)
    public static WebArchive createUpperCaseDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, UPPER_CASE_SECURITY_DOMAIN + ".war");
        war.addClasses(SimpleServlet.class);
        war.addClasses(SimpleSecuredServlet.class);
        war.addAsWebInfResource(ConstantPrincipalTransformerTestCase.class.getPackage(), "principal-transformer-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(UPPER_CASE_SECURITY_DOMAIN), "jboss-web.xml");
        return war;
    }

    @Deployment(name = LOWER_CASE_SECURITY_DOMAIN)
    public static WebArchive createLowerCaseDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, LOWER_CASE_SECURITY_DOMAIN + ".war");
        war.addClasses(SimpleServlet.class);
        war.addClasses(SimpleSecuredServlet.class);
        war.addAsWebInfResource(ConstantPrincipalTransformerTestCase.class.getPackage(), "principal-transformer-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(LOWER_CASE_SECURITY_DOMAIN), "jboss-web.xml");
        return war;
    }

    @Deployment(name = WITHOUT_TRANSFORMER_SECURITY_DOMAIN)
    public static WebArchive createWithoutTransformerDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WITHOUT_TRANSFORMER_SECURITY_DOMAIN + ".war");
        war.addClasses(SimpleServlet.class);
        war.addClasses(SimpleSecuredServlet.class);
        war.addAsWebInfResource(ConstantPrincipalTransformerTestCase.class.getPackage(), "principal-transformer-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(WITHOUT_TRANSFORMER_SECURITY_DOMAIN), "jboss-web.xml");
        return war;
    }

    /**
     * Test whether name of user passed to security domain is adjusted into upper case with
     * the 'case-principal-transformer'. Test also checks that authentication passes for correct password.
     */
    @Test
    @OperateOnDeployment(UPPER_CASE_SECURITY_DOMAIN)
    public void testPassingExistingUserUpperCase(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareUrl(webAppURL);
        Utils.makeCallWithBasicAuthn(url, LOWER_USER, PASSWORD, SC_OK); // UPPER_CASE ("USER1") is stored in the realm
    }

    /**
     * Test checks that authentication fails for incorrect password and incorrect user
     * if upper case 'case-principal-transformer is used.
     */
    @Test
    @OperateOnDeployment(UPPER_CASE_SECURITY_DOMAIN)
    public void testPassingNonExistingUserWithUpperCase(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareUrl(webAppURL);
        Utils.makeCallWithBasicAuthn(url, "wrongUser", PASSWORD, SC_UNAUTHORIZED);
    }

    /**
     * Tests whether authentication fails when the principal transformer is not configured in the domain.
     */
     @Test
     @OperateOnDeployment(WITHOUT_TRANSFORMER_SECURITY_DOMAIN)
     public void testPassingExistingUserWithoutTransformer(@ArquillianResource URL webAppUrl) throws Exception {
         URL url = prepareUrl(webAppUrl);
         Utils.makeCallWithBasicAuthn(url, LOWER_USER, PASSWORD, SC_UNAUTHORIZED); // UPPER_USER ("USER1") is in the realm
     }

    /**
     *  Test whether name of user passed to security domain is adjusted into lower case with
     *  the 'case-principal-transformer'. Test also checks that authentication passes for correct password.
     */
     @Test
     @OperateOnDeployment(LOWER_CASE_SECURITY_DOMAIN)
     public void testPassingExistingUserWithLowerCase(@ArquillianResource URL webAppUrl) throws Exception {
         URL url = prepareUrl(webAppUrl);
         Utils.makeCallWithBasicAuthn(url, UPPER_USER, PASSWORD, SC_OK); // LOWER_USER ("user1") is in the realm
     }


    private URL prepareUrl(URL url) throws MalformedURLException {
        return new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
    }

    static class SetupTask implements ServerSetupTask {

        private static final String UPPER_TRANSFORMER_DOMAIN_NAME = "withUpperTransformerDomain";
        private static final String LOWER_TRANSFORMER_DOMAIN_NAME = "withLowerTransformerDomain";
        private static final String WITHOUT_TRANSFORMER_DOMAIN_NAME = "withoutTransformerDomain";
        private static final String UPPER_PRINCIPAL_TRANSFORMER = "upperTransformer";
        private static final String LOWER_PRINCIPAL_TRANSFORMER = "lowerTransformer";
        private static final String PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY = "global";

        PropertyFileBasedDomain domainWithUpperTransformer;
        PropertyFileBasedDomain domainWithLowerTransformer;
        PropertyFileBasedDomain domainWithoutTransformer;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            setUpTestDomain(domainWithUpperTransformer, UPPER_CASE_SECURITY_DOMAIN, UPPER_TRANSFORMER_DOMAIN_NAME, UPPER_USER, PASSWORD, ROLE, UPPER_PRINCIPAL_TRANSFORMER, true);
            setUpTestDomain(domainWithLowerTransformer, LOWER_CASE_SECURITY_DOMAIN, LOWER_TRANSFORMER_DOMAIN_NAME, LOWER_USER, PASSWORD, ROLE, LOWER_PRINCIPAL_TRANSFORMER, false);
            setUpTestDomain(domainWithoutTransformer, WITHOUT_TRANSFORMER_SECURITY_DOMAIN, WITHOUT_TRANSFORMER_DOMAIN_NAME, UPPER_USER, PASSWORD, ROLE); // no transformer

            ServerReload.reloadIfRequired(managementClient);
        }


        public void setUpTestDomain(PropertyFileBasedDomain domain, String testDomain, String domainName, String user, String password, String role, String transformerName, boolean upperCase) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                domain = PropertyFileBasedDomain.builder().withName(domainName)
                        .withUser(user, password, role)
                        .build();
                domain.create(cli);

                // optionally sets up a transformer
                if (transformerName != null) {
                    cli.sendLine(String.format(
                            "/subsystem=elytron/case-principal-transformer=%s:add(upper-case=%s)",
                            transformerName, upperCase));
                    cli.sendLine(String.format(
                            "/subsystem=elytron/security-domain=%s:write-attribute(name=realms[0].principal-transformer,value=%s)",
                            domainName, transformerName));
                }

                cli.sendLine(String.format(
                        "/subsystem=elytron/http-authentication-factory=%1$s:add(http-server-mechanism-factory=%2$s,security-domain=%1$s,"
                                + "mechanism-configurations=[{mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=\"%1$s\"}]}])",
                        domainName, PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY));

                cli.sendLine(String.format(
                        "/subsystem=undertow/application-security-domain=%s:add(http-authentication-factory=%s)",
                        testDomain, domainName));
            }
        }

        public void setUpTestDomain(PropertyFileBasedDomain domain, String testDomain, String domainName, String user, String password, String role) throws Exception{
            setUpTestDomain(domain, testDomain, domainName, user, password, role, null, false);
        }


        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            tearDownTestDomain(domainWithUpperTransformer, UPPER_CASE_SECURITY_DOMAIN, UPPER_TRANSFORMER_DOMAIN_NAME, UPPER_PRINCIPAL_TRANSFORMER);
            tearDownTestDomain(domainWithLowerTransformer, LOWER_CASE_SECURITY_DOMAIN, LOWER_TRANSFORMER_DOMAIN_NAME, LOWER_PRINCIPAL_TRANSFORMER);
            tearDownTestDomain(domainWithoutTransformer, WITHOUT_TRANSFORMER_SECURITY_DOMAIN, WITHOUT_TRANSFORMER_DOMAIN_NAME);

            ServerReload.reloadIfRequired(managementClient);
        }

        public void tearDownTestDomain(PropertyFileBasedDomain domain, String testDomain, String domainName, String transformerName) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                if (transformerName != null) {
                    cli.sendLine(String.format(
                            "/subsystem=elytron/security-domain=%s:undefine-attribute(name=realms[0].principal-transformer)",
                            transformerName));
                }
                cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:remove()", testDomain));
                cli.sendLine(String.format("/subsystem=elytron/http-authentication-factory=%s:remove()", domainName));

                if (transformerName != null) {
                    cli.sendLine(String.format("/subsystem=elytron/case-principal-transformer=%s:remove()", transformerName));
                }

                domain.remove(cli);
            }
        }

        public void tearDownTestDomain(PropertyFileBasedDomain domain, String testDomain, String domainName) throws Exception {
            tearDownTestDomain(domain, testDomain, domainName, null);
        }

    }
}
