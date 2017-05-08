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
package org.wildfly.test.integration.elytron.roledecoders;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.plexus.util.StringUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.PropertiesRealm;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;

/**
 * Test case for Elytron Simple Role Decoder.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({SimpleRoleDecoderTestCase.SetupTask.class})
public class SimpleRoleDecoderTestCase {

    private static final String DECODE_FROM_ROLE_ATTRIBUTE_A = "decode-from-role-attribute-a";
    private static final String DECODE_FROM_ROLE_ATTRIBUTE_B = "decode-from-role-attribute-b";

    private static final String USER_WITH_ONE_ROLE = "userWithOneRole";
    private static final String USER_WITH_TWO_ROLES = "userWithTwoRoles";
    private static final String PASSWORD = "password";

    private static final String ROLE1 = "role1";
    private static final String ROLE2 = "role2";

    static final String[] ALL_TESTED_ROLES = {ROLE1, ROLE2};

    static final String QUERY_ROLES;

    static {
        final List<NameValuePair> qparams = new ArrayList<>();
        for (final String role : ALL_TESTED_ROLES) {
            qparams.add(new BasicNameValuePair(RolePrintingServlet.PARAM_ROLE_NAME, role));
        }
        QUERY_ROLES = URLEncodedUtils.format(qparams, "UTF-8");
    }

    @Deployment(name = DECODE_FROM_ROLE_ATTRIBUTE_A)
    public static WebArchive deploymentDecodeFromAttributeA() {
        return deployment(DECODE_FROM_ROLE_ATTRIBUTE_A);
    }

    @Deployment(name = DECODE_FROM_ROLE_ATTRIBUTE_B)
    public static WebArchive deploymentDecodeFromAttributeB() {
        return deployment(DECODE_FROM_ROLE_ATTRIBUTE_B);
    }

    private static WebArchive deployment(String name) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, name + ".war");
        war.addClasses(RolePrintingServlet.class);
        war.addAsWebInfResource(SimpleRoleDecoderTestCase.class.getPackage(), "simple-role-decoder-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(name), "jboss-web.xml");
        return war;
    }

    /**
     * Test whether role is decoded correctly from attribute which includes one value.
     */
    @Test
    @OperateOnDeployment(DECODE_FROM_ROLE_ATTRIBUTE_A)
    public void testDecodeOneRole(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ONE_ROLE, PASSWORD, ROLE1);
    }

    /**
     * Test whether both roles are decoded correctly from attribute which includes two values.
     */
    @Test
    @OperateOnDeployment(DECODE_FROM_ROLE_ATTRIBUTE_A)
    public void testDecodeMoreRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_TWO_ROLES, PASSWORD, ROLE1, ROLE2);
    }

    /**
     * Test whether no role is decoded when they are included in different attribute.
     */
    @Test
    @OperateOnDeployment(DECODE_FROM_ROLE_ATTRIBUTE_B)
    public void testDoNotDecodeRoleFromDifferentAttribute(@ArquillianResource URL webAppURL) throws Exception {
        assertNoRoleAssigned(webAppURL, USER_WITH_TWO_ROLES, PASSWORD);
    }

    private void testAssignedRoles(URL webAppURL, String username, String password, String... assignedRoles) throws Exception {
        final URL rolesPrintingURL = prepareRolesPrintingURL(webAppURL);
        final String rolesResponse = Utils.makeCallWithBasicAuthn(rolesPrintingURL, username, password, SC_OK);

        final List<String> assignedRolesList = Arrays.asList(assignedRoles);

        for (String role : ALL_TESTED_ROLES) {
            if (assignedRolesList.contains(role)) {
                assertInRole(rolesResponse, role);
            } else {
                assertNotInRole(rolesResponse, role);
            }
        }
    }

    private void assertNoRoleAssigned(URL webAppURL, String username, String password) throws Exception {
        final URL rolesPrintingURL = prepareRolesPrintingURL(webAppURL);
        Utils.makeCallWithBasicAuthn(rolesPrintingURL, username, password, SC_FORBIDDEN);
    }

    private URL prepareRolesPrintingURL(URL webAppURL) throws MalformedURLException {
        return new URL(webAppURL.toExternalForm() + RolePrintingServlet.SERVLET_PATH.substring(1) + "?" + QUERY_ROLES);
    }

    private void assertInRole(final String rolePrintResponse, String role) {
        if (!StringUtils.contains(rolePrintResponse, "," + role + ",")) {
            fail("Missing role '" + role + "' assignment");
        }
    }

    private void assertNotInRole(final String rolePrintResponse, String role) {
        if (StringUtils.contains(rolePrintResponse, "," + role + ",")) {
            fail("Unexpected role '" + role + "' assignment");
        }
    }

    static class SetupTask extends AbstractElytronSetupTask {

        private static final String PROPERTIES_REALM = "simple-role-decoder-properties-realm";

        private static final String ROLE_ATTRIBUTE_A = "role-attribute-a";
        private static final String ROLE_ATTRIBUTE_B = "role-attribute-b";

        @Override
        public void setup(ManagementClient mc, String string) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format(
                        "/subsystem=elytron/simple-role-decoder=%s:add(attribute=%s)",
                        DECODE_FROM_ROLE_ATTRIBUTE_A, ROLE_ATTRIBUTE_A));
                cli.sendLine(String.format(
                        "/subsystem=elytron/simple-role-decoder=%s:add(attribute=%s)",
                        DECODE_FROM_ROLE_ATTRIBUTE_B, ROLE_ATTRIBUTE_B));
            }
            super.setup(mc, string);
        }

        @Override
        public void tearDown(ManagementClient mc, String string) throws Exception {
            super.tearDown(mc, string);
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=elytron/simple-role-decoder=%s:remove()", DECODE_FROM_ROLE_ATTRIBUTE_B));
                cli.sendLine(String.format("/subsystem=elytron/simple-role-decoder=%s:remove()", DECODE_FROM_ROLE_ATTRIBUTE_A));
            }
            ServerReload.reloadIfRequired(mc.getControllerClient());
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            elements.add(PropertiesRealm.builder().withName(PROPERTIES_REALM)
                    .withGroupsAttribute(ROLE_ATTRIBUTE_A)
                    .withUser(USER_WITH_ONE_ROLE, PASSWORD, ROLE1)
                    .withUser(USER_WITH_TWO_ROLES, PASSWORD, ROLE1, ROLE2)
                    .build());
            addResourcesForAuthnWithSimpleRoleDecoder(elements, DECODE_FROM_ROLE_ATTRIBUTE_A, ROLE_ATTRIBUTE_A);
            addResourcesForAuthnWithSimpleRoleDecoder(elements, DECODE_FROM_ROLE_ATTRIBUTE_B, ROLE_ATTRIBUTE_B);

            return elements.toArray(new ConfigurableElement[elements.size()]);
        }

        private void addResourcesForAuthnWithSimpleRoleDecoder(List<ConfigurableElement> elements, String name, String attribute) {
            elements.add(
                    SimpleSecurityDomain.builder().withName(name)
                    .withDefaultRealm(PROPERTIES_REALM)
                    .withPermissionMapper("default-permission-mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm
                            .builder()
                            .withRealm(PROPERTIES_REALM)
                            .withRoleDecoder(name)
                            .build())
                    .build());
            elements.add(UndertowDomainMapper.builder()
                    .withName(name)
                    .withApplicationDomains(name)
                    .build());
        }

    }
}
