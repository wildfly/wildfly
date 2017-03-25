/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
import org.jboss.as.test.integration.security.common.RolesPrintingServletUtils;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.PropertyFileBasedDomain;

/**
 * Test case for 'constant-principal-transformer' Elytron subsystem resource.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ConstantPrincipalTransformerTestCase.SetupTask.class})
public class ConstantPrincipalTransformerTestCase {

    private static final String DEP_SECURITY_DOMAIN = "contant-principal-transformer-domain";

    private static final String USER1 = "user1";
    private static final String USER2 = "user2";
    private static final String PASSWORD1 = "password1";
    private static final String PASSWORD2 = "password2";

    private static final String ROLE1 = "role1";
    private static final String ROLE2 = "role2";
    private final String[] allPossibleRoles = {ROLE1, ROLE2};

    @Deployment(name = DEP_SECURITY_DOMAIN)
    public static WebArchive securityDomainWithMappingModuleDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEP_SECURITY_DOMAIN + ".war");
        war.addClasses(RolePrintingServlet.class);
        war.addAsWebInfResource(ConstantPrincipalTransformerTestCase.class.getPackage(), "principal-transformer-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(DEP_SECURITY_DOMAIN), "jboss-web.xml");
        return war;
    }

    /**
     * Test whether name of any user passed to security domain is transformed into constant defined in used
     * 'constant-principal-transformer'. Test also checks that authentication passes for correct password. It also checks that
     * correct role is assigned to transformed user.
     */
    @Test
    @OperateOnDeployment(DEP_SECURITY_DOMAIN)
    public void testPassingAnyUserAndCorrectPassword(@ArquillianResource URL webAppURL) throws Exception {
        URL prepareRolePrintingUrl = RolesPrintingServletUtils.prepareRolePrintingUrl(webAppURL, allPossibleRoles);
        String responseBody = Utils.makeCallWithBasicAuthn(prepareRolePrintingUrl, "anyUser", PASSWORD1, SC_OK);

        String[] expectedRoles = {ROLE1};
        RolesPrintingServletUtils.assertExpectedRoles(responseBody, allPossibleRoles, expectedRoles);
    }

    /**
     * Test checks that authentication fails for incorrect password if 'constant-principal-transformer' is used.
     */
    @Test
    @OperateOnDeployment(DEP_SECURITY_DOMAIN)
    public void testPassingWrongPassword(@ArquillianResource URL webAppURL) throws Exception {
        URL prepareRolePrintingUrl = RolesPrintingServletUtils.prepareRolePrintingUrl(webAppURL, allPossibleRoles);
        Utils.makeCallWithBasicAuthn(prepareRolePrintingUrl, USER1, "wrongPassword", SC_UNAUTHORIZED);
    }

    /**
     * Test that even existing user name is transformed into constant defined in used 'constant-principal-transformer'. It
     * checks that if password of existing user is passed to authentication, than authentication fails.
     */
    @Test
    @OperateOnDeployment(DEP_SECURITY_DOMAIN)
    public void testPassingExistingUserAndHisPassword(@ArquillianResource URL webAppURL) throws Exception {
        URL prepareRolePrintingUrl = RolesPrintingServletUtils.prepareRolePrintingUrl(webAppURL, allPossibleRoles);
        Utils.makeCallWithBasicAuthn(prepareRolePrintingUrl, USER2, PASSWORD2, SC_UNAUTHORIZED);
    }

    /**
     * Test that even existing user name is transformed into constant defined in used 'constant-principal-transformer'. It
     * checks that if password of transformed user is passed to authentication, than authentication passes. It also checks that
     * correct role is assigned to transformed user.
     */
    @Test
    @OperateOnDeployment(DEP_SECURITY_DOMAIN)
    public void testPassingExistingUserAndCorrectPassword(@ArquillianResource URL webAppURL) throws Exception {
        URL prepareRolePrintingUrl = RolesPrintingServletUtils.prepareRolePrintingUrl(webAppURL, allPossibleRoles);
        String responseBody = Utils.makeCallWithBasicAuthn(prepareRolePrintingUrl, USER2, PASSWORD1, SC_OK);

        String[] expectedRoles = {ROLE1};
        RolesPrintingServletUtils.assertExpectedRoles(responseBody, allPossibleRoles, expectedRoles);
    }

    static class SetupTask implements ServerSetupTask {

        private static final String ELYTRON_SECURITY = "elytronDomain";
        private static final String PRINCIPAL_TRANSFORMER = "transformer";
        private static final String PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY = "global";

        PropertyFileBasedDomain domain;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                domain = PropertyFileBasedDomain.builder().withName(ELYTRON_SECURITY)
                        .withUser(USER1, PASSWORD1, ROLE1)
                        .withUser(USER2, PASSWORD2, ROLE2)
                        .build();
                domain.create(cli);
                cli.sendLine(String.format(
                        "/subsystem=elytron/constant-principal-transformer=%s:add(constant=%s)",
                        PRINCIPAL_TRANSFORMER, USER1));
                cli.sendLine(String.format(
                        "/subsystem=elytron/security-domain=%s:write-attribute(name=realms[0].principal-transformer,value=%s)",
                        ELYTRON_SECURITY, PRINCIPAL_TRANSFORMER));
                cli.sendLine(String.format(
                        "/subsystem=elytron/http-authentication-factory=%1$s:add(http-server-mechanism-factory=%2$s,security-domain=%1$s,"
                        + "mechanism-configurations=[{mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=\"%1$s\"}]}])",
                        ELYTRON_SECURITY, PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY));
                cli.sendLine(String.format(
                        "/subsystem=undertow/application-security-domain=%s:add(http-authentication-factory=%s)",
                        DEP_SECURITY_DOMAIN, ELYTRON_SECURITY));
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format(
                        "/subsystem=elytron/security-domain=%s:undefine-attribute(name=realms[0].principal-transformer)",
                        ELYTRON_SECURITY));
                cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:remove()", DEP_SECURITY_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/http-authentication-factory=%s:remove()", ELYTRON_SECURITY));
                cli.sendLine(String.format("/subsystem=elytron/constant-principal-transformer=%s:remove()", PRINCIPAL_TRANSFORMER));
                domain.remove(cli);
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

    }
}
