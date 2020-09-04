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
package org.wildfly.test.integration.elytron.realm;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.AnnotationUtils;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
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
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Smoke test for Elytron Ldap Realm. It tests only basic functionality of Ldap Realm. <br>
 *
 * Given: Deployed secured application deployment for printing roles<br>
 * and using BASIC authentication<br>
 * and using ldap-realm with default configuration.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({LdapRealmTestCase.LDAPServerSetupTask.class, LdapRealmTestCase.SetupTask.class})
public class LdapRealmTestCase {

    private static final String DEPLOYMENT = "ldapRealmDep";
    private static final String DEPLOYMENT_WITH_CHARSET = "ldapRealmDepCharset";
    private static final String DEPLOYMENT_HEX_CHARSET = "ldapRealmDepHexCharset";

    private static final int LDAP_PORT = 10389;

    private static final String USER_WITHOUT_ROLE = "userWithoutRole";
    private static final String USER_WITH_ONE_ROLE = "userWithOneRole";
    private static final String USER_WITH_MORE_ROLES = "userWithMoreRoles";
    private static final String USER_NOT_EXIST = "notExistUser";
    private static final String USER_WITH_CHARSET = "ssha512UserCharset";
    private static final String USER_HEX_CHARSET = "cryptUserCharsetHex";
    private static final String EMPTY_USER = "";
    private static final String CORRECT_PASSWORD = "Password1";
    private static final String WRONG_PASSWORD = "WrongPassword";
    private static final String EMPTY_PASSWORD = "";
    private static final String CHARSET_PASSWORD = "password密码";

    static final String[] ALL_TESTED_ROLES = {"TheDuke", "JBossAdmin"};

    static final String QUERY_ROLES;

    static {
        final List<NameValuePair> qparams = new ArrayList<>();
        for (final String role : ALL_TESTED_ROLES) {
            qparams.add(new BasicNameValuePair(RolePrintingServlet.PARAM_ROLE_NAME, role));
        }
        QUERY_ROLES = URLEncodedUtils.format(qparams, StandardCharsets.UTF_8);
    }

    @Deployment(name = DEPLOYMENT)
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClasses(RolePrintingServlet.class);
        war.addAsWebInfResource(LdapRealmTestCase.class.getPackage(), "ldap-realm-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(DEPLOYMENT), "jboss-web.xml");
        return war;
    }

    @Deployment(name = DEPLOYMENT_WITH_CHARSET)
    public static WebArchive deploymentWithCharset() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_WITH_CHARSET + ".war");
        war.addClasses(SimpleServlet.class);
        war.addClasses(SimpleSecuredServlet.class);
        war.addAsWebInfResource(LdapRealmTestCase.class.getPackage(), "ldap-realm-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(DEPLOYMENT_WITH_CHARSET), "jboss-web.xml");
        return war;
    }

    @Deployment(name = DEPLOYMENT_HEX_CHARSET)
    public static WebArchive deploymentEncodedWithCharset() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_HEX_CHARSET + ".war");
        war.addClasses(SimpleServlet.class);
        war.addClasses(SimpleSecuredServlet.class);
        war.addAsWebInfResource(LdapRealmTestCase.class.getPackage(), "ldap-realm-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(DEPLOYMENT_HEX_CHARSET), "jboss-web.xml");
        return war;
    }

    /**
     * Given: LDAP maps roles 'TheDuke' and 'JBossAdmin' to user 'userWithMoreRoles'. <br>
     * When user 'userWithMoreRoles' with correct password tries to authenticate, <br>
     * then authentication should succeed and just roles 'TheDuke' and 'JBossAdmin' should be assigned to user.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testCorrectUserCorrectPasswordTwoRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_MORE_ROLES, CORRECT_PASSWORD, "TheDuke", "JBossAdmin");
    }

    /**
     * Given: LDAP maps role 'JBossAdmin' to user 'userWithOneRole'. <br>
     * When user 'userWithOneRole' with correct password tries to authenticate, <br>
     * then authentication should succeed and just role 'JBossAdmin' should be assigned to user.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testCorrectUserCorrectPasswordOneRole(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ONE_ROLE, CORRECT_PASSWORD, "JBossAdmin");
    }

    /**
     *
     * Test LDAP realm correctly handles a password using a different character set to
     * to use when converting the password string to a byte array.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT_WITH_CHARSET)
    public void testCorrectUserCorrectPasswordWithCharset(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareURL(webAppURL);
        Utils.makeCallWithBasicAuthn(url, USER_WITH_CHARSET, CHARSET_PASSWORD, SC_OK);
    }

    /**
     *
     * Test LDAP realm correctly handles a password using a different character set to
     * to use when converting the password string to a byte array and hex encoding
     * as the string format for the password if they are not stored in plain text.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT_HEX_CHARSET)
    public void testCorrectUserCorrectPasswordWithCharsetAndEncoding(@ArquillianResource URL webAppURL) throws Exception {
        URL url = prepareURL(webAppURL);
        Utils.makeCallWithBasicAuthn(url, USER_HEX_CHARSET, CHARSET_PASSWORD, SC_OK);
    }

    /**
     * Given: LDAP maps no role to user 'userWithoutRole'. <br>
     * When user 'userWithoutRole' with correct password tries to authenticate, <br>
     * then authentication should succeed but no roles should be assigned to user (HTTP status 403 is returned).
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testCorrectUserCorrectPasswordNoRole(@ArquillianResource URL webAppURL) throws Exception {
        assertNoRoleAssigned(webAppURL, USER_WITHOUT_ROLE, CORRECT_PASSWORD);

    }

    /**
     * When exist user with wrong password tries to authenticate, <br>
     * then authentication should fail.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testCorrectUserWrongPassword(@ArquillianResource URL webAppURL) throws Exception {
        assertAuthenticationFailed(webAppURL, USER_WITH_ONE_ROLE, WRONG_PASSWORD);
    }

    /**
     * When exist user with empty password tries to authenticate, <br>
     * then authentication should fail.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testCorrectUserEmptyPassword(@ArquillianResource URL webAppURL) throws Exception {
        assertAuthenticationFailed(webAppURL, USER_WITH_ONE_ROLE, EMPTY_PASSWORD);
    }

    /**
     * When non-exist user with exist password tries to authenticate, <br>
     * then authentication should fail.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testWrongUserExistPassword(@ArquillianResource URL webAppURL) throws Exception {
        assertAuthenticationFailed(webAppURL, USER_NOT_EXIST, CORRECT_PASSWORD);
    }

    /**
     * When user with empty username with exist password tries to authenticate, <br>
     * then authentication should fail.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testEmptyUserExistPassword(@ArquillianResource URL webAppURL) throws Exception {
        assertAuthenticationFailed(webAppURL, EMPTY_USER, CORRECT_PASSWORD);
    }

    private URL prepareURL(URL url) throws MalformedURLException {
        return new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
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

    private void assertAuthenticationFailed(URL webAppURL, String username, String password) throws Exception {
        final URL rolesPrintingURL = prepareRolesPrintingURL(webAppURL);
        Utils.makeCallWithBasicAuthn(rolesPrintingURL, username, password, SC_UNAUTHORIZED);
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

    static class SetupTask implements ServerSetupTask {

        private static final String LDAP_REALM_RELATED_CONFIGURATION_NAME = "elytronLdapRealmRelatedConfiguration";
        private static final String LDAP_REALM_CHARSET_CONFIGURATION_NAME = "elytronLdapRealmCharsetConfiguration";
        private static final String LDAP_REALM_CHARSET_ENCODED_CONFIGURATION_NAME = "elytronLdapRealmCharsetEncodedConfiguration";
        private static final String PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY = "global";
        private static final String DIR_CONTEXT_NAME = "ldapRealmDirContext";
        private static final String LDAP_REALM_NAME = "simpleLdapRealm";
        private static final String LDAP_REALM_CHARSET_NAME = "ldapRealmCharset";
        private static final String LDAP_REALM_ENCODED_CHARSET_NAME = "ldapRealmCharsetEncoded";

        @Override
        public void setup(ManagementClient mc, String string) throws Exception {
            String hostname = "ldap://" + TestSuiteEnvironment.getServerAddress() + ":" + LDAP_PORT;
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format(
                        "/subsystem=elytron/dir-context=%s:add(url=\"%s\",principal=\"uid=admin,ou=system\",credential-reference={clear-text=secret})",
                        DIR_CONTEXT_NAME, hostname));
            }
            setUpTestDomain(LDAP_REALM_NAME, LDAP_REALM_RELATED_CONFIGURATION_NAME, DEPLOYMENT);
            setUpTestDomain(LDAP_REALM_CHARSET_NAME, LDAP_REALM_CHARSET_CONFIGURATION_NAME, DEPLOYMENT_WITH_CHARSET, "GB2312", "base64", false);
            setUpTestDomain(LDAP_REALM_ENCODED_CHARSET_NAME, LDAP_REALM_CHARSET_ENCODED_CONFIGURATION_NAME, DEPLOYMENT_HEX_CHARSET, "GB2312", "hex", false);

            ServerReload.reloadIfRequired(mc);
        }

        private void setUpTestDomain(String realmName, String domainName, String deployment) throws Exception {
            setUpTestDomain(realmName, domainName, deployment, "UTF-8", "base64", true);
        }

        private void setUpTestDomain(String realmName, String domainName, String deployment, String hashCharset, String hashEncoding, boolean testingRoles) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {

                if (testingRoles) {
                    cli.sendLine(String.format(
                            "/subsystem=elytron/ldap-realm=%s:add(dir-context=%s, hash-charset=%s, hash-encoding=%s, identity-mapping={rdn-identifier=uid,search-base-dn=\"ou=People,dc=jboss,dc=org\",user-password-mapper={from=userPassword}," +
                                    "attribute-mapping=[{filter-base-dn=\"ou=Roles,dc=jboss,dc=org\",filter=\"(member={1})\",from=cn,to=groups}]})",
                            realmName, DIR_CONTEXT_NAME, hashCharset, hashEncoding));
                    cli.sendLine(String.format(
                            "/subsystem=elytron/security-domain=%1$s:add(realms=[{realm=%2$s,role-decoder=groups-to-roles}],default-realm=%2$s,permission-mapper=default-permission-mapper)",
                            domainName, realmName));
                } else {
                    cli.sendLine(String.format(
                            "/subsystem=elytron/ldap-realm=%s:add(dir-context=%s, hash-charset=%s, hash-encoding=%s, identity-mapping={rdn-identifier=uid,search-base-dn=\"ou=People,dc=jboss,dc=org\",user-password-mapper={from=userPassword}," +
                                    "attribute-mapping=[{filter-base-dn=\"ou=Roles,dc=jboss,dc=org\",filter=\"(&(objectClass=groupOfNames)(member={1}))\",from=cn,to=Roles}]})",
                            realmName, DIR_CONTEXT_NAME, hashCharset, hashEncoding));

                    cli.sendLine("/subsystem=elytron/simple-role-decoder=from-roles-attribute:add(attribute=Roles)\n");
                    cli.sendLine(String.format(
                            "/subsystem=elytron/security-domain=%1$s:add(realms=[{realm=%2$s,role-decoder=from-roles-attribute}],default-realm=%2$s,permission-mapper=default-permission-mapper)",
                            domainName, realmName));
                }

                cli.sendLine(String.format(
                        "/subsystem=elytron/http-authentication-factory=%1$s:add(http-server-mechanism-factory=%2$s,security-domain=%1$s,"
                                + "mechanism-configurations=[{mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=\"%1$s\"}]}])",
                        domainName, PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY));
                cli.sendLine(String.format(
                        "/subsystem=undertow/application-security-domain=%s:add(http-authentication-factory=%s)",
                        deployment, domainName));
            }
        }

        @Override
        public void tearDown(ManagementClient mc, String string) throws Exception {
            tearDownDomain(DEPLOYMENT, LDAP_REALM_RELATED_CONFIGURATION_NAME, LDAP_REALM_NAME);
            tearDownDomain(DEPLOYMENT_WITH_CHARSET, LDAP_REALM_CHARSET_CONFIGURATION_NAME, LDAP_REALM_CHARSET_NAME);
            tearDownDomain(DEPLOYMENT_HEX_CHARSET, LDAP_REALM_CHARSET_ENCODED_CONFIGURATION_NAME, LDAP_REALM_ENCODED_CHARSET_NAME);
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=elytron/dir-context=%s:remove()", DIR_CONTEXT_NAME));
            }
            ServerReload.reloadIfRequired(mc);
        }

        private void tearDownDomain(String deployment, String domainName, String realmName) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:remove()", deployment));
                cli.sendLine(String.format("/subsystem=elytron/http-authentication-factory=%s:remove()",
                        domainName));
                cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:remove()",
                        domainName));
                cli.sendLine(String.format("/subsystem=elytron/ldap-realm=%s:remove()", realmName));
            }
        }

    }

    /**
     * A server setup task which configures and starts LDAP server.
     */
    //@formatter:off
    @CreateDS(
            name = "JBossDS-LdapRealmTestCase",
            factory = org.jboss.as.test.integration.ldap.InMemoryDirectoryServiceFactory.class,
            partitions
            = {
                @CreatePartition(
                        name = "jboss",
                        suffix = "dc=jboss,dc=org",
                        contextEntry = @ContextEntry(
                                entryLdif
                                = "dn: dc=jboss,dc=org\n"
                                + "dc: jboss\n"
                                + "objectClass: top\n"
                                + "objectClass: domain\n\n"),
                        indexes
                        = {
                            @CreateIndex(attribute = "objectClass"),
                            @CreateIndex(attribute = "dc"),
                            @CreateIndex(attribute = "ou")
                        })
            },
            additionalInterceptors = {KeyDerivationInterceptor.class})
    @CreateLdapServer(
            transports
            = {
                @CreateTransport(protocol = "LDAP", port = LDAP_PORT, address = "0.0.0.0")
            },
            certificatePassword = "secret")
    //@formatter:on
    static class LDAPServerSetupTask implements ServerSetupTask {

        private DirectoryService directoryService;
        private LdapServer ldapServer;

        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            directoryService = DSAnnotationProcessor.getDirectoryService();
            final SchemaManager schemaManager = directoryService.getSchemaManager();
            try {
                for (LdifEntry ldifEntry : new LdifReader(
                        LdapRealmTestCase.class.getResourceAsStream(LdapRealmTestCase.class.getSimpleName() + ".ldif"))) {
                    directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                    (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
            Utils.fixApacheDSTransportAddress(createLdapServer, Utils.getSecondaryTestAddress(managementClient, false));
            ldapServer = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService);
            ldapServer.start();
        }

        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ldapServer.stop();
            directoryService.shutdown();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
        }
    }
}
