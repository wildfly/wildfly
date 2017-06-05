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
package org.wildfly.test.integration.elytron.securitydomain;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
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
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.elytron.rolemappers.AddPrefixRoleMapperTestCase;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.PropertiesRealm;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;

/**
 * Test case for 'security-domain' Elytron subsystem resource. Testing of most of functionality and attributes of Elytron
 * security-domain subsystem resource is covered by another test cases in {@link org.wildfly.test.integration.elytron.*}
 * package. This test case covers only scenarios which are not covered by different test cases.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({SecurityDomainTestCase.SetupTask.class})
public class SecurityDomainTestCase {

    private static final String DEFAULT_REALM = "default-realm-test";
    private static final String ROLE_MAPPER_PRIORITY = "role-mapper-priority-test";
    private static final String PRINCIPAL_TRANSFORMER_ORDER = "principal-transformer-order-test";

    private static final String ROLE_MAPPER_FOR_SECURITY_DOMAIN = "rm1";
    private static final String ROLE_MAPPER_FOR_REALM = "rm2";

    private static final String PRE_REALM_TRANSFORMER_NAME = "suf1";
    private static final String POST_REALM_TRANSFORMER_NAME = "suf2";
    private static final String REALM_TRANSFORMER_NAME = "suf3";

    private static final String USER_IN_SIMPLE_REALM = "userInSimpleRealm";
    private static final String PASSWORD_FOR_USER_IN_SIMPLE_REALM = "passwordSimpleRealm";
    private static final String USER_IN_ROLE_MAPPER_REALM = "userInRoleMapperRealm";
    private static final String PASSWORD_FOR_USER_IN_ROLE_MAPPER_REALM = "passwordRoleMapperRealm";
    private static final String USER_BEFORE_TRANSFORMATION = "user";
    private static final String TRANSFORMED_USER = "user" + PRE_REALM_TRANSFORMER_NAME + POST_REALM_TRANSFORMER_NAME
            + REALM_TRANSFORMER_NAME;
    private static final String PASSWORD_FOR_TRANSFORMED_USER = "passwordForTransformedUser";

    private static final String SIMPLE_ROLE = "Role";
    private static final String ROLE_MAPPER_PRIORITY_ROLE = ROLE_MAPPER_FOR_SECURITY_DOMAIN + ROLE_MAPPER_FOR_REALM
            + SIMPLE_ROLE;
    private static final String ROLE_MAPPER_PRIORITY_WRONG_PRIORITY_ROLE = ROLE_MAPPER_FOR_REALM
            + ROLE_MAPPER_FOR_SECURITY_DOMAIN + SIMPLE_ROLE;

    private static final String[] ALL_TESTED_ROLES = {SIMPLE_ROLE, ROLE_MAPPER_PRIORITY_ROLE,
        ROLE_MAPPER_PRIORITY_WRONG_PRIORITY_ROLE};
    private static final String queryRoles;

    static {
        final List<NameValuePair> qparams = new ArrayList<>();
        for (final String role : ALL_TESTED_ROLES) {
            qparams.add(new BasicNameValuePair(RolePrintingServlet.PARAM_ROLE_NAME, role));
        }
        queryRoles = URLEncodedUtils.format(qparams, "UTF-8");
    }

    @Deployment(name = DEFAULT_REALM)
    public static WebArchive defaultRealmDeployment() {
        return deployment(DEFAULT_REALM);
    }

    @Deployment(name = ROLE_MAPPER_PRIORITY)
    public static WebArchive roleMapperPriorityDeployment() {
        return deployment(ROLE_MAPPER_PRIORITY);
    }

    @Deployment(name = PRINCIPAL_TRANSFORMER_ORDER)
    public static WebArchive principalTransformerOrderDeployment() {
        return deployment(PRINCIPAL_TRANSFORMER_ORDER);
    }

    public static WebArchive deployment(String deploymentName) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName + ".war");
        war.addClasses(RolePrintingServlet.class);
        war.addAsWebInfResource(SecurityDomainTestCase.class.getPackage(), "security-domain-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(deploymentName), "jboss-web.xml");
        return war;
    }

    /**
     * Test whether default realm is correctly chosen from provided realms in security domain.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEFAULT_REALM)
    public void testDefaultRealmIsUsed(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareRolesPrintingURL(webAppURL);
        testAssignedRoles(url, USER_IN_SIMPLE_REALM, PASSWORD_FOR_USER_IN_SIMPLE_REALM, SIMPLE_ROLE);
    }

    /**
     * Test whether non-default realm is not used even if correct user with correct password from that non-default realm try to
     * access application secured by given security domain.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEFAULT_REALM)
    public void testUserFromNonDefaultRealmIsDenied(@ArquillianResource URL webAppURL) throws Exception {
        assertAuthenticationFailed(webAppURL, USER_IN_ROLE_MAPPER_REALM, PASSWORD_FOR_USER_IN_ROLE_MAPPER_REALM);
    }

    /**
     * Test whether role-mapper in realm is applied before role-mapper in security-domain.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(ROLE_MAPPER_PRIORITY)
    public void testRoleMapperPriorityForRealm(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareRolesPrintingURL(webAppURL);
        testAssignedRoles(url, USER_IN_ROLE_MAPPER_REALM, PASSWORD_FOR_USER_IN_ROLE_MAPPER_REALM, ROLE_MAPPER_PRIORITY_ROLE);
    }

    /**
     * Test whether principal transformers are applied in order: pre-realm-principal-transformer,
     * post-realm-principal-transformer, realm.principal-transformer.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(PRINCIPAL_TRANSFORMER_ORDER)
    public void testPrincipalTransformerOrder(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareRolesPrintingURL(webAppURL);
        testAssignedRoles(url, USER_BEFORE_TRANSFORMATION, PASSWORD_FOR_TRANSFORMED_USER, SIMPLE_ROLE);
    }

    private URL prepareRolesPrintingURL(URL webAppURL) throws MalformedURLException {
        return new URL(webAppURL.toExternalForm() + RolePrintingServlet.SERVLET_PATH.substring(1) + "?" + queryRoles);
    }

    private void assertAuthenticationFailed(URL webAppURL, String username, String password) throws Exception {
        final URL rolesPrintingURL = prepareRolesPrintingURL(webAppURL);
        Utils.makeCallWithBasicAuthn(rolesPrintingURL, username, password, SC_UNAUTHORIZED);
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

        private static final String SIMPLE_REALM = "simpleRealm";
        private static final String REALM_FOR_ROLE_MAPPER_PRIORITY = "realmForRoleMapperPriority";
        private static final String REALM_FOR_PRINCIPAL_TRANSFORMER_ORDER = "realmForPrincipalTransformerOrder";

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            elements.add(new AddPrefixRoleMapperTestCase.ServerSetup.AddPrefixRoleMappers(
                    String.format("%1$s:add(prefix=%1$s)", ROLE_MAPPER_FOR_SECURITY_DOMAIN),
                    String.format("%1$s:add(prefix=%1$s)", ROLE_MAPPER_FOR_REALM)
            ));
            elements.add(new PrincipalTransformers(PRE_REALM_TRANSFORMER_NAME, POST_REALM_TRANSFORMER_NAME,
                    REALM_TRANSFORMER_NAME
            ));

            elements.add(PropertiesRealm.builder().withName(SIMPLE_REALM)
                    .withUser(USER_IN_SIMPLE_REALM, PASSWORD_FOR_USER_IN_SIMPLE_REALM, SIMPLE_ROLE)
                    .build());
            elements.add(PropertiesRealm.builder().withName(REALM_FOR_ROLE_MAPPER_PRIORITY)
                    .withUser(USER_IN_ROLE_MAPPER_REALM, PASSWORD_FOR_USER_IN_ROLE_MAPPER_REALM, SIMPLE_ROLE)
                    .build());
            elements.add(PropertiesRealm.builder().withName(REALM_FOR_PRINCIPAL_TRANSFORMER_ORDER)
                    .withUser(TRANSFORMED_USER, PASSWORD_FOR_TRANSFORMED_USER, SIMPLE_ROLE)
                    .build());

            elements.add(SimpleSecurityDomain.builder().withName(DEFAULT_REALM)
                    .withDefaultRealm(SIMPLE_REALM)
                    .withPermissionMapper("default-permission-mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm(REALM_FOR_ROLE_MAPPER_PRIORITY)
                            .withRoleDecoder("groups-to-roles")
                            .build(),
                            SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm(SIMPLE_REALM)
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            elements.add(undertowDomainMapper(DEFAULT_REALM));

            elements.add(SimpleSecurityDomain.builder().withName(ROLE_MAPPER_PRIORITY)
                    .withDefaultRealm(REALM_FOR_ROLE_MAPPER_PRIORITY)
                    .withPermissionMapper("default-permission-mapper")
                    .withRoleMapper(ROLE_MAPPER_FOR_SECURITY_DOMAIN)
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm(REALM_FOR_ROLE_MAPPER_PRIORITY)
                            .withRoleDecoder("groups-to-roles")
                            .withRoleMapper(ROLE_MAPPER_FOR_REALM)
                            .build())
                    .build());
            elements.add(undertowDomainMapper(ROLE_MAPPER_PRIORITY));

            elements.add(SimpleSecurityDomain.builder().withName(PRINCIPAL_TRANSFORMER_ORDER)
                    .withDefaultRealm(REALM_FOR_PRINCIPAL_TRANSFORMER_ORDER)
                    .withPermissionMapper("default-permission-mapper")
                    .withPreRealmPrincipalTransformer(PRE_REALM_TRANSFORMER_NAME)
                    .withPostRealmPrincipalTransformer(POST_REALM_TRANSFORMER_NAME)
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm(REALM_FOR_PRINCIPAL_TRANSFORMER_ORDER)
                            .withRoleDecoder("groups-to-roles")
                            .withPrincipalTransformer(REALM_TRANSFORMER_NAME)
                            .build())
                    .build());
            elements.add(undertowDomainMapper(PRINCIPAL_TRANSFORMER_ORDER));

            return elements.toArray(new ConfigurableElement[elements.size()]);
        }

        private ConfigurableElement undertowDomainMapper(String name) {
            return UndertowDomainMapper.builder()
                    .withName(name)
                    .withApplicationDomains(name)
                    .build();
        }

        public static class PrincipalTransformers implements ConfigurableElement {

            private final String[] suffixsToAdd;

            public PrincipalTransformers(String... suffixsToAdd) {
                this.suffixsToAdd = suffixsToAdd;
            }

            @Override
            public void create(CLIWrapper cli) throws Exception {
                for (String sfx : suffixsToAdd) {
                    cli.sendLine("/subsystem=elytron/regex-principal-transformer=" + sfx + ":add(pattern=$,replacement=" + sfx + ")");
                }
            }

            @Override
            public void remove(CLIWrapper cli) throws Exception {
                for (String sfx : suffixsToAdd) {
                    cli.sendLine("/subsystem=elytron/regex-principal-transformer=" + sfx + ":remove()");
                }
            }

            @Override
            public String getName() {
                return "regex-principal-transformer";
            }
        }
    }
}
