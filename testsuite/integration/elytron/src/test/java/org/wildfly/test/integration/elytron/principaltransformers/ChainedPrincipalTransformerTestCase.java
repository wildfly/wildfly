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
 * Test case for 'chained-principal-transformer' Elytron subsystem resource. This test cases uses also
 * 'regex-principal-transformer' - it means that any issue in 'regex-principal-transformer' can affect this test case.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ChainedPrincipalTransformerTestCase.SetupTask.class})
public class ChainedPrincipalTransformerTestCase {

    private static final String DEP_SECURITY_DOMAIN_TWO = "chained-principal-transformer-two";
    private static final String DEP_SECURITY_DOMAIN_THREE = "chained-principal-transformer-three";
    private static final String DEP_SECURITY_DOMAIN_TRANSFORM_TRANSFORMED = "chained-principal-transformer-transform-transformed";

    private static final String USER1 = "userAB";
    private static final String USER2 = "userBAC";
    private static final String USER3 = "transformerUser";
    private static final String PASSWORD1 = "password1";
    private static final String PASSWORD2 = "password2";
    private static final String PASSWORD3 = "password3";
    private static final String ROLE = "JBossAdmin";

    @Deployment(name = DEP_SECURITY_DOMAIN_TWO)
    public static WebArchive deploymentTwoTransformersInChain() {
        return createDeployment(DEP_SECURITY_DOMAIN_TWO);
    }

    @Deployment(name = DEP_SECURITY_DOMAIN_THREE)
    public static WebArchive deploymentThreeTransformersInChain() {
        return createDeployment(DEP_SECURITY_DOMAIN_THREE);
    }

    @Deployment(name = DEP_SECURITY_DOMAIN_TRANSFORM_TRANSFORMED)
    public static WebArchive deploymentTransformTransformed() {
        return createDeployment(DEP_SECURITY_DOMAIN_TRANSFORM_TRANSFORMED);
    }

    private static WebArchive createDeployment(String domain) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, domain + ".war");
        war.addClasses(SimpleServlet.class);
        war.addClasses(SimpleSecuredServlet.class);
        war.addAsWebInfResource(ChainedPrincipalTransformerTestCase.class.getPackage(), "principal-transformer-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(domain), "jboss-web.xml");
        return war;
    }

    /**
     * Test that in case when two principal transformers are used in 'chained-principal-transformer', then transformations
     * happens in expected order.
     */
    @Test
    @OperateOnDeployment(DEP_SECURITY_DOMAIN_TWO)
    public void testTwoTransformersInChain(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareUrl(webAppURL);
        Utils.makeCallWithBasicAuthn(url, "user", PASSWORD1, SC_OK);
    }

    /**
     * Test that in case when three principal transformers are used in 'chained-principal-transformer', then transformations
     * happens in expected order.
     */
    @Test
    @OperateOnDeployment(DEP_SECURITY_DOMAIN_THREE)
    public void testThreeTransformersInChain(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareUrl(webAppURL);
        Utils.makeCallWithBasicAuthn(url, "user", PASSWORD2, SC_OK);
    }

    /**
     * Test that transformation from second transformer is applied on transformed name obtained from first transformer.
     */
    @Test
    @OperateOnDeployment(DEP_SECURITY_DOMAIN_TRANSFORM_TRANSFORMED)
    public void testTransformTransformed(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareUrl(webAppURL);
        Utils.makeCallWithBasicAuthn(url, "transformerUTRANSFORMEDer", PASSWORD3, SC_OK);
    }

    private URL prepareUrl(URL url) throws MalformedURLException {
        return new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
    }

    static class SetupTask implements ServerSetupTask {

        private static final String ELYTRON_SECURITY_TWO = "elytronDomainTwo";
        private static final String ELYTRON_SECURITY_THREE = "elytronDomainThree";
        private static final String ELYTRON_SECURITY_TRANSFORM_TRANSFORMED = "elytronDomainTransformTransformed";
        private static final String PRINCIPAL_TRANSFORMER_TWO = "transformerTwo";
        private static final String PRINCIPAL_TRANSFORMER_THREE = "transformerThree";
        private static final String PRINCIPAL_TRANSFORMER_TRANSFORM_TRANSFORMED = "transformerTransformTransformed";
        private static final String REGEX_TRANSFORMER_A = "regexTransformerA";
        private static final String REGEX_TRANSFORMER_B = "regexTransformerB";
        private static final String REGEX_TRANSFORMER_C = "regexTransformerC";
        private static final String REGEX_TRANSFORMED_FIRST = "regexTransformer1";
        private static final String REGEX_TRANSFORMED_SECOND = "regexTransformer2";
        private static final String PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY = "global";

        private PropertyFileBasedDomain chainedTwoTransformers;
        private PropertyFileBasedDomain chainedThreeTransformers;
        private PropertyFileBasedDomain transformTransformed;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=elytron/regex-principal-transformer=%s:add(pattern=$,replacement=A)",
                        REGEX_TRANSFORMER_A));
                cli.sendLine(String.format("/subsystem=elytron/regex-principal-transformer=%s:add(pattern=$,replacement=B)",
                        REGEX_TRANSFORMER_B));
                cli.sendLine(String.format("/subsystem=elytron/regex-principal-transformer=%s:add(pattern=$,replacement=C)",
                        REGEX_TRANSFORMER_C));
                cli.sendLine(String.format("/subsystem=elytron/regex-principal-transformer=%s:add(pattern=TRANSFORM,replacement=CHANG)",
                        REGEX_TRANSFORMED_FIRST));
                cli.sendLine(String.format("/subsystem=elytron/regex-principal-transformer=%s:add(pattern=CHANGED,replacement=s)",
                        REGEX_TRANSFORMED_SECOND));

                chainedTwoTransformers = prepareSingleConfiguration(cli, ELYTRON_SECURITY_TWO, PRINCIPAL_TRANSFORMER_TWO,
                        DEP_SECURITY_DOMAIN_TWO,
                        "/subsystem=elytron/chained-principal-transformer=%s:add(principal-transformers=[" + REGEX_TRANSFORMER_A
                        + "," + REGEX_TRANSFORMER_B + "])");
                chainedThreeTransformers = prepareSingleConfiguration(cli, ELYTRON_SECURITY_THREE, PRINCIPAL_TRANSFORMER_THREE,
                        DEP_SECURITY_DOMAIN_THREE,
                        "/subsystem=elytron/chained-principal-transformer=%s:add(principal-transformers=[" + REGEX_TRANSFORMER_B
                        + "," + REGEX_TRANSFORMER_A + "," + REGEX_TRANSFORMER_C + "])");
                transformTransformed = prepareSingleConfiguration(cli, ELYTRON_SECURITY_TRANSFORM_TRANSFORMED,
                        PRINCIPAL_TRANSFORMER_TRANSFORM_TRANSFORMED, DEP_SECURITY_DOMAIN_TRANSFORM_TRANSFORMED,
                        "/subsystem=elytron/chained-principal-transformer=%s:add(principal-transformers=[" + REGEX_TRANSFORMED_FIRST
                        + "," + REGEX_TRANSFORMED_SECOND + "])");
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                removeSingleConfiguration(cli, ELYTRON_SECURITY_TWO, PRINCIPAL_TRANSFORMER_TWO,
                        DEP_SECURITY_DOMAIN_TWO, chainedTwoTransformers);
                removeSingleConfiguration(cli, ELYTRON_SECURITY_THREE, PRINCIPAL_TRANSFORMER_THREE,
                        DEP_SECURITY_DOMAIN_THREE, chainedThreeTransformers);
                removeSingleConfiguration(cli, ELYTRON_SECURITY_TRANSFORM_TRANSFORMED, PRINCIPAL_TRANSFORMER_TRANSFORM_TRANSFORMED,
                        DEP_SECURITY_DOMAIN_TRANSFORM_TRANSFORMED, transformTransformed);

                cli.sendLine(String.format("/subsystem=elytron/regex-principal-transformer=%s:remove()", REGEX_TRANSFORMED_SECOND));
                cli.sendLine(String.format("/subsystem=elytron/regex-principal-transformer=%s:remove()", REGEX_TRANSFORMED_FIRST));
                cli.sendLine(String.format("/subsystem=elytron/regex-principal-transformer=%s:remove()", REGEX_TRANSFORMER_C));
                cli.sendLine(String.format("/subsystem=elytron/regex-principal-transformer=%s:remove()", REGEX_TRANSFORMER_B));
                cli.sendLine(String.format("/subsystem=elytron/regex-principal-transformer=%s:remove()", REGEX_TRANSFORMER_A));
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

        private PropertyFileBasedDomain prepareSingleConfiguration(CLIWrapper cli, String elytronName, String transformerName,
                String deploymentName, String addTransformerCli)
                throws Exception {
            PropertyFileBasedDomain domain = PropertyFileBasedDomain.builder().withName(elytronName)
                    .withUser(USER1, PASSWORD1, ROLE)
                    .withUser(USER2, PASSWORD2, ROLE)
                    .withUser(USER3, PASSWORD3, ROLE)
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
            cli.sendLine(String.format("/subsystem=elytron/chained-principal-transformer=%s:remove()", transformerName));
            domain.remove(cli);
        }
    }
}
