/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat Middleware LLC, and individual contributors
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


import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.jboss.as.test.integration.security.common.Utils.makeCallWithTokenAuthn;
import static org.jboss.as.test.shared.CliUtils.asAbsolutePath;
import static org.jboss.as.test.shared.CliUtils.escapePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.cli.Util;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.DirContext;
import org.wildfly.test.security.common.elytron.FailoverRealm;
import org.wildfly.test.security.common.elytron.FileAuditLog;
import org.wildfly.test.security.common.elytron.FileSystemRealm;
import org.wildfly.test.security.common.elytron.JdbcSecurityRealm;
import org.wildfly.test.security.common.elytron.LdapRealm;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;
import org.wildfly.test.security.common.elytron.PropertiesRealm;
import org.wildfly.test.security.common.elytron.SimpleHttpAuthenticationFactory;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.TokenRealm;
import org.wildfly.test.security.common.elytron.UserWithAttributeValues;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * Tests the {@link FailoverRealm} within the Elytron subsystem. The tests cover filesystem-realm, jdbc-realm, ldap-realm,
 * properties-realm, and token-realm, trying the failover to a properties-realm when the primary (delegate) realm is unavailable
 * (throws RealmUnavailableException).
 *
 * It's also tested whether the SecurityRealmUnavailableEvent is emitted when expected.
 *
 * @author Ondrej Kotek
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({FailoverRealmTestCase.ServerSetup.class})
public class FailoverRealmTestCase {

    private static final String DEPLOYMENT_FIRST_AVAILABLE = "FailoverRealmDeployment-FirstAvailable";
    private static final String DEPLOYMENT_FILESYSTEM = "FailoverRealmDeployment-Filesystem";
    private static final String DEPLOYMENT_JDBC = "FailoverRealmDeployment-JDBC";
    private static final String DEPLOYMENT_LDAP = "FailoverRealmDeployment-LDAP";
    private static final String DEPLOYMENT_PROPERTIES = "FailoverRealmDeployment-properties";
    private static final String DEPLOYMENT_BOTH_UNAVAILABLE = "FailoverRealmDeployment-BothUnvailable";
    private static final String DEPLOYMENT_EVENT_DISABLED = "FailoverRealmDeployment-EventDisabled";
    private static final String DEPLOYMENT_BEARER_TOKEN = "FailoverRealmDeployment-BearerToken";
    private static final String INDEX_PAGE_CONTENT = "index page content";

    private static final Encoder B64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String JWT_HEADER_B64 = B64_ENCODER
            .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private static final File AUDIT_LOG_FILE = Paths.get("target",
            FailoverRealmTestCase.class.getSimpleName() + "-test-audit.log").toFile();

    @Deployment(name = DEPLOYMENT_FIRST_AVAILABLE)
    public static WebArchive deploymentForFirstRealmAvailable() {
        return deployment(DEPLOYMENT_FIRST_AVAILABLE, "failover-realm-web.xml");
    }

    @Deployment(name = DEPLOYMENT_FILESYSTEM)
    public static WebArchive deploymentForFilesystemRealm() {
        return deployment(DEPLOYMENT_FILESYSTEM, "failover-realm-web.xml");
    }

    @Deployment(name = DEPLOYMENT_JDBC)
    public static WebArchive deploymentForJdbcRealm() {
        return deployment(DEPLOYMENT_JDBC, "failover-realm-web.xml");
    }

    @Deployment(name = DEPLOYMENT_LDAP)
    public static WebArchive deploymentForLdapRealm() {
        return deployment(DEPLOYMENT_LDAP, "failover-realm-web.xml");
    }

    @Deployment(name = DEPLOYMENT_PROPERTIES)
    public static WebArchive deploymentForPropertiesRealmUnavailable() {
        return deployment(DEPLOYMENT_PROPERTIES, "failover-realm-web.xml");
    }

    @Deployment(name = DEPLOYMENT_BOTH_UNAVAILABLE)
    public static WebArchive deploymentForBothRealmsUnavailable() {
        return deployment(DEPLOYMENT_BOTH_UNAVAILABLE, "failover-realm-web.xml");
    }

    @Deployment(name = DEPLOYMENT_EVENT_DISABLED)
    public static WebArchive deploymentForEventDisabled() {
        return deployment(DEPLOYMENT_EVENT_DISABLED, "failover-realm-web.xml");
    }

    @Deployment(name = DEPLOYMENT_BEARER_TOKEN)
    public static WebArchive deploymentForBearerToken() {
        return deployment(DEPLOYMENT_BEARER_TOKEN, "failover-realm-bearer-token-web.xml");
    }

    private static WebArchive deployment(String name, String webXml) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, name + ".war");
        war.add(new StringAsset(INDEX_PAGE_CONTENT), "index.html");
        war.addAsWebInfResource(FailoverRealmTestCase.class.getPackage(), webXml, "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(name), "jboss-web.xml");
        return war;
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_FIRST_AVAILABLE)
    public void testFirstRealmAvailable(@ArquillianResource URL webAppUrl) throws Exception {
        testCredentialBasedRealmsNoFailover(webAppUrl);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_FILESYSTEM)
    public void testFilesystemRealmUnavailable(@ArquillianResource URL webAppUrl) throws Exception {
        assumeFalse(Util.isWindows()); // no easy and robust way to prepare the conditions on Windows
        testCredentialBasedRealmsFailover(webAppUrl);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_JDBC)
    public void testJdbcRealmUnavailable(@ArquillianResource URL webAppUrl) throws Exception {
        testCredentialBasedRealmsFailover(webAppUrl);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_LDAP)
    public void testLdapRealmUnavailable(@ArquillianResource URL webAppUrl) throws Exception {
        testCredentialBasedRealmsFailover(webAppUrl);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_PROPERTIES)
    public void testPropertiesRealmUnavailable(@ArquillianResource URL webAppUrl) throws Exception {
        cleanAuditLog();
        String result = Utils.makeCallWithBasicAuthn(webAppUrl, "user1", "password1", SC_OK);
        assertEquals(INDEX_PAGE_CONTENT, result);
        assertTrue("SecurityRealmUnavailableEvent should be logged", isSecurityRealmUnavailableEventLogged());

        cleanAuditLog();
        result = Utils.makeCallWithBasicAuthn(webAppUrl, "user1", "wrongPassword1", SC_UNAUTHORIZED);
        assertNotEquals(INDEX_PAGE_CONTENT, result);
        assertTrue("SecurityRealmUnavailableEvent should be logged", isSecurityRealmUnavailableEventLogged());

        // no failover, just user1 is known in the first realm (its unhashed password causes the failover)
        cleanAuditLog();
        result = Utils.makeCallWithBasicAuthn(webAppUrl, "non-existing-user1", "password1", SC_UNAUTHORIZED);
        assertNotEquals(INDEX_PAGE_CONTENT, result);
        assertFalse("SecurityRealmUnavailableEvent should not be logged", isSecurityRealmUnavailableEventLogged());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_BOTH_UNAVAILABLE)
    public void testBothCredentialBasedRealmsUnavailable(@ArquillianResource URL webAppUrl) throws Exception {
        cleanAuditLog();
        String result = Utils.makeCallWithBasicAuthn(webAppUrl, "user1", "password1", SC_INTERNAL_SERVER_ERROR);
        assertNotEquals(INDEX_PAGE_CONTENT, result);
        assertTrue("SecurityRealmUnavailableEvent should be logged", isSecurityRealmUnavailableEventLogged());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_EVENT_DISABLED)
    public void testEventDisabled(@ArquillianResource URL webAppUrl) throws Exception {
        // failovers happen but are not logged, like in case of no failovers
        testCredentialBasedRealmsNoFailover(webAppUrl);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_BEARER_TOKEN)
    public void testTokenRealmUnvailable(@ArquillianResource URL webAppUrl) throws Exception {
        cleanAuditLog();
        String result = makeCallWithTokenAuthn(webAppUrl, createJwtToken("userA", "issuer.wildfly.org"), SC_OK);
        assertEquals(INDEX_PAGE_CONTENT, result);
        assertTrue("SecurityRealmUnavailableEvent should be logged", isSecurityRealmUnavailableEventLogged());

        cleanAuditLog();
        result = makeCallWithTokenAuthn(webAppUrl, createJwtToken("userC", "unknown_issuer"), SC_UNAUTHORIZED);
        assertNotEquals(INDEX_PAGE_CONTENT, result);
        assertTrue("SecurityRealmUnavailableEvent should be logged", isSecurityRealmUnavailableEventLogged());
    }

    private void testCredentialBasedRealmsFailover(URL webAppUrl) throws Exception {
        cleanAuditLog();
        String result = Utils.makeCallWithBasicAuthn(webAppUrl, "user1", "password1", SC_OK);
        assertEquals(INDEX_PAGE_CONTENT, result);
        assertTrue("SecurityRealmUnavailableEvent should be logged", isSecurityRealmUnavailableEventLogged());

        cleanAuditLog();
        result = Utils.makeCallWithBasicAuthn(webAppUrl, "user1", "wrongPassword1", SC_UNAUTHORIZED);
        assertNotEquals(INDEX_PAGE_CONTENT, result);
        assertTrue("SecurityRealmUnavailableEvent should be logged", isSecurityRealmUnavailableEventLogged());

        cleanAuditLog();
        result = Utils.makeCallWithBasicAuthn(webAppUrl, "non-existing-user1", "password1", SC_UNAUTHORIZED);
        assertNotEquals(INDEX_PAGE_CONTENT, result);
        assertTrue("SecurityRealmUnavailableEvent should be logged", isSecurityRealmUnavailableEventLogged());
    }

    private void testCredentialBasedRealmsNoFailover(URL webAppUrl) throws Exception {
        cleanAuditLog();
        String result = Utils.makeCallWithBasicAuthn(webAppUrl, "user1", "password1", SC_OK);
        assertEquals(INDEX_PAGE_CONTENT, result);
        assertFalse("SecurityRealmUnavailableEvent should not be logged", isSecurityRealmUnavailableEventLogged());

        cleanAuditLog();
        result = Utils.makeCallWithBasicAuthn(webAppUrl, "user1", "wrongPassword1", SC_UNAUTHORIZED);
        assertNotEquals(INDEX_PAGE_CONTENT, result);
        assertFalse("SecurityRealmUnavailableEvent should not be logged", isSecurityRealmUnavailableEventLogged());

        cleanAuditLog();
        result = Utils.makeCallWithBasicAuthn(webAppUrl, "non-existing-user1", "password1", SC_UNAUTHORIZED);
        assertNotEquals(INDEX_PAGE_CONTENT, result);
        assertFalse("SecurityRealmUnavailableEvent should not be logged", isSecurityRealmUnavailableEventLogged());
    }

    private static boolean isSecurityRealmUnavailableEventLogged() throws Exception {
        List<String> lines = Files.readAllLines(AUDIT_LOG_FILE.toPath(), StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.contains("SecurityRealmUnavailableEvent")) {
                return true;
            }
        }
        return false;
    }

    private static void cleanAuditLog() throws Exception {
        try (PrintWriter writer = new PrintWriter(AUDIT_LOG_FILE)) {
            writer.print("");
        }
    }

    private static String createJwtToken(String userName, String issuer) {
        String jwtPayload = String.format("{"
                + "\"active\": true,"
                + "\"iss\": \"%1$s\","
                + "\"sub\": \"elytron@wildfly.org\","
                + "\"exp\": 2051222399,"
                + "\"aud\": \"%1$s\","
                + "\"groups\": [\"%2$s\"]"
                + "}", issuer, userName);
        return JWT_HEADER_B64 + "." + B64_ENCODER.encodeToString(jwtPayload.getBytes(StandardCharsets.UTF_8)) + ".";
    }

    static class ServerSetup extends AbstractElytronSetupTask {
        File unavailableFileSystemRealmDir;

        @Override
        protected ConfigurableElement[] getConfigurableElements() {

            ArrayList<ConfigurableElement> configurableElements = new ArrayList<>();

            // this audit log is checked for presence of SecurityRealmUnavailableEvent
            configurableElements.add(FileAuditLog.builder()
                    .withName("audit_log_for_failover_realm")
                    .withPath(asAbsolutePath(AUDIT_LOG_FILE))
                    .build());

            // this properties realm is used as a failover realm to test the other credential based failovers
            configurableElements.add(PropertiesRealm.builder()
                    .withName("properties_realm_1")
                    .withUser(UserWithAttributeValues.builder()
                            .withName("user1")
                            .withPassword("password1")
                            .withValues("user")
                            .build())
                    .build());

            // filesystem realm
            FileSystemRealm unavailableFileSystemRealm = FileSystemRealm.builder()
                    .withName("unavailable_filesystem_realm")
                    .build();
            String unavailableFileSystemRealmPath = unavailableFileSystemRealm.getPath().getPath();
             // prevent accessing the temporary directory for the realm
            unavailableFileSystemRealmDir = new File(escapePath(unavailableFileSystemRealmPath));
            unavailableFileSystemRealmDir.setExecutable(false, false);
            configurableElements.add(unavailableFileSystemRealm);
            configurableElements.add(FailoverRealm.builder("failover_realm_filesystem")
                    .withDelegateRealm("unavailable_filesystem_realm")
                    .withFailoverRealm("properties_realm_1")
                    .build());
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName("failover_realm_domain_filesystem")
                    .withDefaultRealm("failover_realm_filesystem")
                    .withPermissionMapper("default-permission-mapper")
                    .withSecurityEventListener("audit_log_for_failover_realm")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm("failover_realm_filesystem")
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                    .withName(DEPLOYMENT_FILESYSTEM)
                    .withSecurityDomain("failover_realm_domain_filesystem")
                    .build());

            // JDBC realm
            configurableElements.add(JdbcSecurityRealm.builder("unavailable_jdbc_realm")
                    .withPrincipalQuery("ExampleDS", "invalid SQL")
                            .withPasswordMapper("clear-password-mapper", null, 1, -1, -1)
                            .build()
                    .build());
            configurableElements.add(FailoverRealm.builder("failover_realm_jdbc")
                    .withDelegateRealm("unavailable_jdbc_realm")
                    .withFailoverRealm("properties_realm_1")
                    .build());
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName("failover_realm_domain_jdbc")
                    .withDefaultRealm("failover_realm_jdbc")
                    .withPermissionMapper("default-permission-mapper")
                    .withSecurityEventListener("audit_log_for_failover_realm")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm("failover_realm_jdbc")
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                    .withName(DEPLOYMENT_JDBC)
                    .withSecurityDomain("failover_realm_domain_jdbc")
                    .build());

            // LDAP realm
            configurableElements.add(DirContext.builder("unavailable_dir_context")
                    .withUrl("invalid_url")
                    .build());
            configurableElements.add(LdapRealm.builder("unavailable_ldap_realm")
                    .withDirContext("unavailable_dir_context")
                    .withIdentityMapping(LdapRealm.identityMappingBuilder()
                            .withRdnIdentifier("invalid")
                            .withUserPasswordMapper(new LdapRealm.UserPasswordMapperBuilder()
                                    .withFrom("invalid")
                                    .build())
                            .build())
                    .build());
            configurableElements.add(FailoverRealm.builder("failover_realm_ldap")
                    .withDelegateRealm("unavailable_ldap_realm")
                    .withFailoverRealm("properties_realm_1")
                    .build());
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName("failover_realm_domain_ldap")
                    .withDefaultRealm("failover_realm_ldap")
                    .withPermissionMapper("default-permission-mapper")
                    .withSecurityEventListener("audit_log_for_failover_realm")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm("failover_realm_ldap")
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                    .withName(DEPLOYMENT_LDAP)
                    .withSecurityDomain("failover_realm_domain_ldap")
                    .build());

            // properties realm
            configurableElements.add(PropertiesRealm.builder()
                    .withName("unavailable_properties_realm")
                    .withPlainText(false)
                    .withUser(UserWithAttributeValues.builder()
                            .withName("user1")
                            .withPassword("password1") // this is expected to be hashed but it isn't
                            .withValues("user")
                            .build())
                    .build());
            configurableElements.add(FailoverRealm.builder("failover_realm_properties")
                    .withDelegateRealm("unavailable_properties_realm")
                    .withFailoverRealm("properties_realm_1")
                    .build());
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName("failover_realm_domain_properties")
                    .withDefaultRealm("failover_realm_properties")
                    .withPermissionMapper("default-permission-mapper")
                    .withSecurityEventListener("audit_log_for_failover_realm")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm("failover_realm_properties")
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                    .withName(DEPLOYMENT_PROPERTIES)
                    .withSecurityDomain("failover_realm_domain_properties")
                    .build());

            // the delegate realm is available, the failover realm is not (LDAP)
            configurableElements.add(FailoverRealm.builder("failover_realm_first_available")
                    .withDelegateRealm("properties_realm_1")
                    .withFailoverRealm("unavailable_ldap_realm")
                    .build());
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName("failover_realm_domain_first_available")
                    .withDefaultRealm("failover_realm_first_available")
                    .withPermissionMapper("default-permission-mapper")
                    .withSecurityEventListener("audit_log_for_failover_realm")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm("failover_realm_first_available")
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                    .withName(DEPLOYMENT_FIRST_AVAILABLE)
                    .withSecurityDomain("failover_realm_domain_first_available")
                    .build());

            // both the credentaial based realms are not available
            configurableElements.add(FailoverRealm.builder("failover_realm_both_unavailable")
                    .withDelegateRealm("unavailable_jdbc_realm")
                    .withFailoverRealm("unavailable_ldap_realm")
                    .build());
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName("failover_realm_domain_both_unavailable")
                    .withDefaultRealm("failover_realm_both_unavailable")
                    .withPermissionMapper("default-permission-mapper")
                    .withSecurityEventListener("audit_log_for_failover_realm")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm("failover_realm_both_unavailable")
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                    .withName(DEPLOYMENT_BOTH_UNAVAILABLE)
                    .withSecurityDomain("failover_realm_domain_both_unavailable")
                    .build());

            // failover with SecurityRealmUnavailableEvent emitting disabled
            configurableElements.add(FailoverRealm.builder("failover_realm_event_disabled")
                    .withDelegateRealm("unavailable_jdbc_realm")
                    .withFailoverRealm("properties_realm_1")
                    .withEmitEvents(false)
                    .build());
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName("failover_realm_domain_event_disabled")
                    .withDefaultRealm("failover_realm_event_disabled")
                    .withPermissionMapper("default-permission-mapper")
                    .withSecurityEventListener("audit_log_for_failover_realm")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm("failover_realm_event_disabled")
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                    .withName(DEPLOYMENT_EVENT_DISABLED)
                    .withSecurityDomain("failover_realm_domain_event_disabled")
                    .build());

            // token realm
            configurableElements.add(TokenRealm.builder("token_realm")
                    .withJwt(TokenRealm.jwtBuilder().withIssuer("issuer.wildfly.org").build())
                    .withPrincipalClaim("aud")
                    .build());
            configurableElements.add(TokenRealm.builder("unavailable_token_realm")
                    .withOauth2Introspection(TokenRealm.oauth2IntrospectionBuilder()
                            .withClientId("failover_realm_client")
                            .withClientSecret("failover_realm_client_secret")
                            .withIntrospectionUrl("http://invalid")
                            .build())
                    .withPrincipalClaim("aud")
                    .build());
            configurableElements.add(FailoverRealm.builder("failover_realm_bearer_token")
                    .withDelegateRealm("unavailable_token_realm")
                    .withFailoverRealm("token_realm")
                    .build());
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName("failover_realm_domain_bearer_token")
                    .withDefaultRealm("failover_realm_bearer_token")
                    .withPermissionMapper("default-permission-mapper")
                    .withSecurityEventListener("audit_log_for_failover_realm")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm("failover_realm_bearer_token")
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            configurableElements.add(SimpleHttpAuthenticationFactory.builder()
                    .withName(DEPLOYMENT_BEARER_TOKEN)
                    .withHttpServerMechanismFactory("global")
                    .withSecurityDomain("failover_realm_domain_bearer_token")
                    .addMechanismConfiguration(MechanismConfiguration.builder()
                            .withMechanismName("BEARER_TOKEN")
                            .build())
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                    .withName(DEPLOYMENT_BEARER_TOKEN)
                    .httpAuthenticationFactory(DEPLOYMENT_BEARER_TOKEN)
                    .build());

            return configurableElements.toArray(new ConfigurableElement[configurableElements.size()]);
        }

        @Override
        protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
            super.tearDown(modelControllerClient);
            Files.deleteIfExists(AUDIT_LOG_FILE.toPath());
        }
    }
}
