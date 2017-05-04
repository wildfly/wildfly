/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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

import java.net.MalformedURLException;
import java.net.URL;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
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
 * Test case for 'regex-principal-transformer' Elytron subsystem resource.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({RegexPrincipalTransformerTestCase.SetupTask.class})
public class RegexPrincipalTransformerTestCase {

    private static final String DEP_SECURITY_DOMAIN_E = "regex-principal-transformer-domain-e";
    private static final String DEP_SECURITY_DOMAIN_S = "regex-principal-transformer-domain-s";

    private static final String SOME_USER = "someuser";
    private static final String PASSWORD = "password";
    private static final String ROLE = "JBossAdmin";

    @Deployment(name = DEP_SECURITY_DOMAIN_E)
    public static WebArchive deploymentE() {
        return createDeployment(DEP_SECURITY_DOMAIN_E);
    }

    @Deployment(name = DEP_SECURITY_DOMAIN_S)
    public static WebArchive deploymentS() {
        return createDeployment(DEP_SECURITY_DOMAIN_S);
    }

    private static WebArchive createDeployment(String domain) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, domain + ".war");
        war.addClasses(SimpleServlet.class);
        war.addClasses(SimpleSecuredServlet.class);
        war.addAsWebInfResource(RegexPrincipalTransformerTestCase.class.getPackage(), "principal-transformer-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(domain), "jboss-web.xml");
        return war;
    }

    /**
     * Test that even existing user name is transformed through used 'regex-principal-transformer'.
     */
    @Test
    @OperateOnDeployment(DEP_SECURITY_DOMAIN_E)
    public void testCorrectUserTransformToWrong(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareUrl(webAppURL);
        Utils.makeCallWithBasicAuthn(url, SOME_USER, PASSWORD, SC_UNAUTHORIZED);
    }

    /**
     * Test that if attribute 'replace-all' is set to false in 'regex-principal-transformer', then only first occurrence of
     * pattern is rewritten.
     */
    @Test
    @OperateOnDeployment(DEP_SECURITY_DOMAIN_E)
    public void testCorrectTransformReplaceFirst(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareUrl(webAppURL);
        Utils.makeCallWithBasicAuthn(url, "somruser", PASSWORD, SC_OK);
    }

    /**
     * Test that pattern does not occurs in user name, then the same user name is returned (no transformation is done).
     */
    @Test
    @OperateOnDeployment(DEP_SECURITY_DOMAIN_S)
    public void testNoTransformForPatternNotMatch(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareUrl(webAppURL);
        Utils.makeCallWithBasicAuthn(url, SOME_USER, PASSWORD, SC_OK);
    }

    /**
     * Test that if attribute 'replace-all' is set to true in 'regex-principal-transformer', then all occurrences of pattern are
     * rewritten.
     */
    @Test
    @OperateOnDeployment(DEP_SECURITY_DOMAIN_S)
    public void testCorrectTransformReplaceAll(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareUrl(webAppURL);
        Utils.makeCallWithBasicAuthn(url, "1omeu234er", PASSWORD, SC_OK);
    }

    private URL prepareUrl(URL url) throws MalformedURLException {
        return new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
    }

    static class SetupTask implements ServerSetupTask {

        private static final String ELYTRON_SECURITY_E = "elytronDomainE";
        private static final String ELYTRON_SECURITY_S = "elytronDomainS";
        private static final String PRINCIPAL_TRANSFORMER_E = "transformerE";
        private static final String PRINCIPAL_TRANSFORMER_S = "transformerS";
        private static final String PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY = "global";

        private PropertyFileBasedDomain domainE;
        private PropertyFileBasedDomain domainS;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                domainE = prepareSingleConfiguration(cli, ELYTRON_SECURITY_E, PRINCIPAL_TRANSFORMER_E, DEP_SECURITY_DOMAIN_E,
                        "/subsystem=elytron/regex-principal-transformer=%s:add(pattern=(r),replacement=e,replace-all=false)");
                domainS = prepareSingleConfiguration(cli, ELYTRON_SECURITY_S, PRINCIPAL_TRANSFORMER_S, DEP_SECURITY_DOMAIN_S,
                        "/subsystem=elytron/regex-principal-transformer=%s:add(pattern=(\\\\d+),replacement=s,replace-all=true)");
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                removeSingleConfiguration(cli, ELYTRON_SECURITY_S, PRINCIPAL_TRANSFORMER_S, DEP_SECURITY_DOMAIN_S, domainS);
                removeSingleConfiguration(cli, ELYTRON_SECURITY_E, PRINCIPAL_TRANSFORMER_E, DEP_SECURITY_DOMAIN_E, domainE);
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

        private PropertyFileBasedDomain prepareSingleConfiguration(CLIWrapper cli, String elytronName, String transformerName,
                String deploymentName, String addTransformerCli)
                throws Exception {
            PropertyFileBasedDomain domain = PropertyFileBasedDomain.builder().withName(elytronName)
                    .withUser(SOME_USER, PASSWORD, ROLE)
                    .build();
            domain.create(cli);
            cli.sendLine(String.format(addTransformerCli, transformerName));
            cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:write-attribute(name=realms[0].principal-transformer,value=%s)",
                    elytronName, transformerName));
            cli.sendLine(String.format(
                    "/subsystem=elytron/http-authentication-factory=%1$s:add(http-server-mechanism-factory=%2$s,security-domain=%1$s,"
                    + "mechanism-configurations=[{mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=\"%1$s\"}]}])",
                    elytronName, PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY));
            cli.sendLine(String.format(
                    "/subsystem=undertow/application-security-domain=%s:add(http-authentication-factory=%s)",
                    deploymentName, elytronName));
            return domain;
        }

        private void removeSingleConfiguration(CLIWrapper cli, String elytronName, String transformerName,
                String deploymentName, PropertyFileBasedDomain domain) throws Exception {
            cli.sendLine(String.format(
                    "/subsystem=elytron/security-domain=%s:undefine-attribute(name=realms[0].principal-transformer)",
                    elytronName));
            cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:remove()", deploymentName));
            cli.sendLine(String.format("/subsystem=elytron/http-authentication-factory=%s:remove()", elytronName));
            cli.sendLine(String.format("/subsystem=elytron/regex-principal-transformer=%s:remove()", transformerName));
            domain.remove(cli);
        }

    }
}
