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
 * Test case for 'aggregate-principal-transformer' Elytron subsystem resource.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({AggregatePrincipalTransformerTestCase.SetupTask.class})
public class AggregatePrincipalTransformerTestCase {

    private static final String DEPLOYMENT = "dep";
    private static final String AGGREGATE_PRINCIPAL_TRANSFORMER_TEST = "aggregatePrincipalTransformerTest";

    private static final String USER_WITH_DOMAIN1 = "user@domain1";
    private static final String USER_WITH_DOMAIN2 = "user@domain2";
    private static final String PASSWORD_FOR_FIRST_DOMAIN = "passwordDomain1";
    private static final String PASSWORD_FOR_SECOND_DOMAIN = "passwordDomain2";
    private static final String SOME_ROLE = "JBossAdmin";

    @Deployment(name = DEPLOYMENT)
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClasses(SimpleServlet.class);
        war.addClasses(SimpleSecuredServlet.class);
        war.addAsWebInfResource(AggregatePrincipalTransformerTestCase.class.getPackage(), "principal-transformer-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(AGGREGATE_PRINCIPAL_TRANSFORMER_TEST), "jboss-web.xml");
        return war;
    }

    /**
     * Test whether principal is transformer correctly when first transformer in 'aggregate-principal-transformer' returned
     * non-null principal. Principal returned by the first transformer should be used.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void firstTransformerReturnsNonNullResult(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareUrl(webAppURL);
        Utils.makeCallWithBasicAuthn(url, USER_WITH_DOMAIN1, PASSWORD_FOR_FIRST_DOMAIN, SC_OK);
    }

    /**
     * Test whether authentication fails when first transformer in 'aggregate-principal-transformer' returned non-null
     * principal, but user provided wrong password.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void firstTransformerReturnsNonNullResult_wrongPassword(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareUrl(webAppURL);
        Utils.makeCallWithBasicAuthn(url, USER_WITH_DOMAIN1, PASSWORD_FOR_SECOND_DOMAIN, SC_UNAUTHORIZED);
    }

    /**
     * Test whether principal is transformer correctly when first transformer in 'aggregate-principal-transformer' returned null
     * principal and second transformer non-null principal. Principal returned by the second transformer should be used.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void firstTransformerReturnsNullResultSecondNonNull(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareUrl(webAppURL);
        Utils.makeCallWithBasicAuthn(url, USER_WITH_DOMAIN2, PASSWORD_FOR_SECOND_DOMAIN, SC_OK);
    }

    /**
     * Test whether authentication fails when none of transformers in 'aggregate-principal-transformer' returned non-null
     * principal. Application should return HTTP status 401 to enable the new authentication attempt.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void bothTransformersReturnNullResult(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareUrl(webAppURL);
        Utils.makeCallWithBasicAuthn(url, "wrongUser", "wrongPassword", SC_UNAUTHORIZED);
    }

    private URL prepareUrl(URL url) throws MalformedURLException {
        return new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
    }

    static class SetupTask implements ServerSetupTask {

        private static final String USER_FROM_FIRST_DOMAIN = "userFromFirstDomain";
        private static final String USER_FROM_SECOND_DOMAIN = "userFromSecondDomain";

        private static final String PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY = "global";

        private static final String FIRST_DOMAIN = "firstDomain";
        private static final String SECOND_DOMAIN = "secondDomain";
        private static final String IS_FIRST_DOMAIN = "isFirstDomain";
        private static final String IS_SECOND_DOMAIN = "isSecondDomain";
        private static final String AGGREGATE_PRINCIPAL_TRANSFORMER = "aggregatePrincipalTransformer";

        private PropertyFileBasedDomain domainWithAggregatePrincipalTransformer;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=elytron/regex-validating-principal-transformer=%s:add(pattern=\"(.*)@domain1\")",
                        IS_FIRST_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/regex-validating-principal-transformer=%s:add(pattern=\"(.*)@domain2\")",
                        IS_SECOND_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/constant-principal-transformer=%1$s:add(constant=%1$s)",
                        USER_FROM_FIRST_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/constant-principal-transformer=%1$s:add(constant=%1$s)",
                        USER_FROM_SECOND_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/chained-principal-transformer=%s:add(principal-transformers=[%s,%s])",
                        FIRST_DOMAIN, IS_FIRST_DOMAIN, USER_FROM_FIRST_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/chained-principal-transformer=%s:add(principal-transformers=[%s,%s])",
                        SECOND_DOMAIN, IS_SECOND_DOMAIN, USER_FROM_SECOND_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/aggregate-principal-transformer=%s:add(principal-transformers=[%s,%s])",
                        AGGREGATE_PRINCIPAL_TRANSFORMER, FIRST_DOMAIN, SECOND_DOMAIN));
                domainWithAggregatePrincipalTransformer = prepareSingleConfiguration(cli, AGGREGATE_PRINCIPAL_TRANSFORMER_TEST,
                        AGGREGATE_PRINCIPAL_TRANSFORMER);
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                removeSingleConfiguration(cli, AGGREGATE_PRINCIPAL_TRANSFORMER_TEST, domainWithAggregatePrincipalTransformer);
                cli.sendLine(String.format("/subsystem=elytron/aggregate-principal-transformer=%s:remove()",
                        AGGREGATE_PRINCIPAL_TRANSFORMER));
                cli.sendLine(String.format("/subsystem=elytron/chained-principal-transformer=%s:remove()", SECOND_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/chained-principal-transformer=%s:remove()", FIRST_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/constant-principal-transformer=%s:remove()",
                        USER_FROM_SECOND_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/constant-principal-transformer=%s:remove()",
                        USER_FROM_FIRST_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/regex-validating-principal-transformer=%s:remove()",
                        IS_SECOND_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/regex-validating-principal-transformer=%s:remove()",
                        IS_FIRST_DOMAIN));
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

        private PropertyFileBasedDomain prepareSingleConfiguration(CLIWrapper cli, String elytronName, String transformerName)
                throws Exception {
            PropertyFileBasedDomain domain = PropertyFileBasedDomain.builder().withName(elytronName)
                    .withUser(USER_FROM_FIRST_DOMAIN, PASSWORD_FOR_FIRST_DOMAIN, SOME_ROLE)
                    .withUser(USER_FROM_SECOND_DOMAIN, PASSWORD_FOR_SECOND_DOMAIN, SOME_ROLE)
                    .build();
            domain.create(cli);
            cli.sendLine(String.format(
                    "/subsystem=elytron/security-domain=%s:write-attribute(name=realms[0].principal-transformer,value=%s)",
                    elytronName, transformerName));
            cli.sendLine(String.format(
                    "/subsystem=elytron/http-authentication-factory=%1$s:add(http-server-mechanism-factory=%2$s,security-domain=%1$s,"
                    + "mechanism-configurations=[{mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=\"%1$s\"}]}])",
                    elytronName, PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY));
            cli.sendLine(String.format(
                    "/subsystem=undertow/application-security-domain=%1$s:add(http-authentication-factory=%1$s)", elytronName));
            return domain;
        }

        private void removeSingleConfiguration(CLIWrapper cli, String elytronName, PropertyFileBasedDomain domain)
                throws Exception {
            cli.sendLine(String.format(
                    "/subsystem=elytron/security-domain=%s:undefine-attribute(name=realms[0].principal-transformer)",
                    elytronName));
            cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:remove()", elytronName));
            cli.sendLine(String.format("/subsystem=elytron/http-authentication-factory=%s:remove()", elytronName));
            domain.remove(cli);
        }
    }
}
