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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.DirContext;
import org.wildfly.test.security.common.elytron.DistributedRealm;
import org.wildfly.test.security.common.elytron.LdapRealm;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;
import org.wildfly.test.security.common.elytron.PropertiesRealm;
import org.wildfly.test.security.common.elytron.SimpleHttpAuthenticationFactory;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.TokenRealm;
import org.wildfly.test.security.common.elytron.UserWithAttributeValues;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * Tests the {@link DistributedRealm} within the Elytron subsystem. The tests cover both the credential and evidence
 * based authn, trying all identities distributed among the realms backing a distributed-realm by invoking deployed applications.
 *
 * For the credential based authn testing, there are three properties-realm containing identities with corresponding numbers.
 * For the evidence based authn testing, there are two JWT token-realm that differ in accepted issuers.
 * To test the ability to distinguish between the authn types, these realms are mixed into one distributed-realm
 * that backs secured deployments for both the authn types testing (BASIC and BEARER_TOKEN methods are used).
 *
 * To test the behavior when a realm is not available, there is also a deployment backed by a distributed-realm with
 * an unavailable LDAP realm among the properties-realm.
 *
 * @author Ondrej Kotek
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({DistributedRealmTestCase.ServerSetup.class})
public class DistributedRealmTestCase {

    private static final String DEPLOYMENT_FOR_CREDENTIALS = "DistributedRealmDeployment-Credentials";
    private static final String DEPLOYMENT_FOR_EVIDENCE = "DistributedRealmDeployment-Evidence";
    private static final String DEPLOYMENT_FOR_UNAVAILABLE_REALM = "DistributedRealmDeployment-UnavailableRealm";
    private static final String INDEX_PAGE_CONTENT = "index page content";

    private static final Encoder B64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String JWT_HEADER_B64 = B64_ENCODER
            .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    private static final String JWT_ISSUER_1 = "issuer1.wildfly.org";
    private static final String JWT_ISSUER_2 = "issuer2.wildfly.org";

    @Deployment(name = DEPLOYMENT_FOR_CREDENTIALS)
    public static WebArchive deployment() {
        return deployment(DEPLOYMENT_FOR_CREDENTIALS, "distributed-realm-web.xml");
    }

    @Deployment(name = DEPLOYMENT_FOR_EVIDENCE)
    public static WebArchive deploymentForEvidence() {
        return deployment(DEPLOYMENT_FOR_EVIDENCE, "distributed-realm-bearer-token-web.xml");
    }

    @Deployment(name = DEPLOYMENT_FOR_UNAVAILABLE_REALM)
    public static WebArchive deploymentForUnavailableRealm() {
        return deployment(DEPLOYMENT_FOR_UNAVAILABLE_REALM, "distributed-realm-web.xml");
    }

    private static WebArchive deployment(String name, String webXml) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, name + ".war");
        war.add(new StringAsset(INDEX_PAGE_CONTENT), "index.html");
        war.addAsWebInfResource(DistributedRealmTestCase.class.getPackage(), webXml, "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(name), "jboss-web.xml");
        return war;
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_FOR_CREDENTIALS)
    public void testIdentityPerRealm_Success(@ArquillianResource URL webAppUrl) throws Exception {
        String result = Utils.makeCallWithBasicAuthn(webAppUrl, "user1", "password1", SC_OK);
        assertEquals(INDEX_PAGE_CONTENT, result);

        result = Utils.makeCallWithBasicAuthn(webAppUrl, "user2", "password2", SC_OK);
        assertEquals(INDEX_PAGE_CONTENT, result);

        result = Utils.makeCallWithBasicAuthn(webAppUrl, "user3", "password3", SC_OK);
        assertEquals(INDEX_PAGE_CONTENT, result);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_FOR_CREDENTIALS)
    public void testIdentityPerRealm_WrongPassword(@ArquillianResource URL webAppUrl) throws Exception {
        String result = Utils.makeCallWithBasicAuthn(webAppUrl, "user1", "wrongPassword1", SC_UNAUTHORIZED);
        assertNotEquals(INDEX_PAGE_CONTENT, result);

        result = Utils.makeCallWithBasicAuthn(webAppUrl, "user2", "wrongPassword2", SC_UNAUTHORIZED);
        assertNotEquals(INDEX_PAGE_CONTENT, result);

        result = Utils.makeCallWithBasicAuthn(webAppUrl, "user3", "wrongPassword3", SC_UNAUTHORIZED);
        assertNotEquals(INDEX_PAGE_CONTENT, result);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_FOR_CREDENTIALS)
    public void testWrongUserName(@ArquillianResource URL webAppUrl) throws Exception {
        String result = Utils.makeCallWithBasicAuthn(webAppUrl, "non-existing-user1", "password1", SC_UNAUTHORIZED);
        assertNotEquals(INDEX_PAGE_CONTENT, result);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_FOR_CREDENTIALS)
    public void testIdentityInTwoRealms_FirstRealmUsed(@ArquillianResource URL webAppUrl) throws Exception {
        String result = Utils.makeCallWithBasicAuthn(webAppUrl, "user12", "passwordInRealm1", SC_OK);
        assertEquals(INDEX_PAGE_CONTENT, result);

        result = Utils.makeCallWithBasicAuthn(webAppUrl, "user12", "passwordInRealm2", SC_UNAUTHORIZED);
        assertNotEquals(INDEX_PAGE_CONTENT, result);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_FOR_UNAVAILABLE_REALM)
    public void testIdentityPerRealm_UnavailableRealm(@ArquillianResource URL webAppUrl) throws Exception {
        // user1 and user2 are in the realms that are before  the unavailable realm
        String result = Utils.makeCallWithBasicAuthn(webAppUrl, "user1", "password1", SC_OK);
        assertEquals(INDEX_PAGE_CONTENT, result);
        result = Utils.makeCallWithBasicAuthn(webAppUrl, "user2", "password2", SC_OK);
        assertEquals(INDEX_PAGE_CONTENT, result);

        // user3 is in the realm that is after the unavailable realm
        result = Utils.makeCallWithBasicAuthn(webAppUrl, "user3", "password3", SC_INTERNAL_SERVER_ERROR);
        assertNotEquals(INDEX_PAGE_CONTENT, result);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_FOR_EVIDENCE)
    public void testIdentityPerRealm_Evidence(@ArquillianResource URL webAppUrl) throws Exception {
        String result = makeCallWithTokenAuthn(webAppUrl, createJwtToken("userA", JWT_ISSUER_1), SC_OK);
        assertEquals(INDEX_PAGE_CONTENT, result);

        result = makeCallWithTokenAuthn(webAppUrl, createJwtToken("userB", JWT_ISSUER_2), SC_OK);
        assertEquals(INDEX_PAGE_CONTENT, result);

        result = makeCallWithTokenAuthn(webAppUrl, createJwtToken("userC", "unknown_issuer"), SC_UNAUTHORIZED);
        assertNotEquals(INDEX_PAGE_CONTENT, result);
    }

    private String createJwtToken(String userName, String issuer) {
        String jwtPayload = String.format("{" //
                + "\"iss\": \"%1$s\"," //
                + "\"sub\": \"elytron@wildfly.org\"," //
                + "\"exp\": 2051222399," //
                + "\"aud\": \"%1$s\"," //
                + "\"groups\": [\"%2$s\"]" //
                + "}", issuer, userName);
        return JWT_HEADER_B64 + "." + B64_ENCODER.encodeToString(jwtPayload.getBytes(StandardCharsets.UTF_8)) + ".";
    }

    static class ServerSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ArrayList<ConfigurableElement> configurableElements = new ArrayList<>();

            // properties-realm for credential based authn, used by all distributed-realm
            configurableElements.add(PropertiesRealm.builder()
                    .withName("properties_realm_1")
                    .withUser(UserWithAttributeValues.builder()
                            .withName("user1")
                            .withPassword("password1")
                            .withValues("user")
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName("user12")
                            .withPassword("passwordInRealm1")
                            .withValues("user")
                            .build())
                    .build());
            configurableElements.add(PropertiesRealm.builder()
                    .withName("properties_realm_2")
                    .withUser(UserWithAttributeValues.builder()
                            .withName("user2")
                            .withPassword("password2")
                            .withValues("user")
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName("user12")
                            .withPassword("passwordInRealm2")
                            .withValues("user")
                            .build())
                    .build());
            configurableElements.add(PropertiesRealm.builder()
                    .withName("properties_realm_3")
                    .withUser(UserWithAttributeValues.builder()
                            .withName("user3")
                            .withPassword("password3")
                            .withValues("user")
                            .build())
                    .build());

            // token-realm for evidence based authn, differ just in accepted JWT issuers
            configurableElements.add(TokenRealm.builder("token_realm1")
                    .withJwt(TokenRealm.jwtBuilder().withIssuer(JWT_ISSUER_1).build())
                    .withPrincipalClaim("aud")
                    .build());
            configurableElements.add(TokenRealm.builder("token_realm2")
                    .withJwt(TokenRealm.jwtBuilder().withIssuer(JWT_ISSUER_2).build())
                    .withPrincipalClaim("aud")
                    .build());

            // the credential and evidence based realms are mixed into one distributed-realm that is used for both the authn types
            configurableElements.add(DistributedRealm.builder("distributed_realm")
                    .withRealms("properties_realm_1", "token_realm1", "properties_realm_2", "token_realm2", "properties_realm_3")
                    .build());
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName("distributed_realm_domain")
                    .withDefaultRealm("distributed_realm")
                    .withPermissionMapper("default-permission-mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm("distributed_realm")
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                    .withName(DEPLOYMENT_FOR_CREDENTIALS)
                    .withSecurityDomain("distributed_realm_domain")
                    .build());
            configurableElements.add(SimpleHttpAuthenticationFactory.builder()
                    .withName(DEPLOYMENT_FOR_EVIDENCE)
                    .withHttpServerMechanismFactory("global")
                    .withSecurityDomain("distributed_realm_domain")
                    .addMechanismConfiguration(MechanismConfiguration.builder()
                            .withMechanismName("BEARER_TOKEN")
                            .build())
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                    .withName(DEPLOYMENT_FOR_EVIDENCE)
                    .httpAuthenticationFactory(DEPLOYMENT_FOR_EVIDENCE)
                    .build());

            // a deployment backed by a distributed-realm with an unavailable ldap-realm (RealmUnavailableException)
            configurableElements.add(DirContext.builder("unavailable_dir_context")
                    .withUrl("invalid_url")
                    .build());
            configurableElements.add(LdapRealm.builder("unavailable_ldap_realm")
                    .withDirContext("unavailable_dir_context")
                    .withIdentityMapping(LdapRealm.identityMappingBuilder()
                            .withRdnIdentifier("invalid")
                            .build())
                    .build());
            configurableElements.add(DistributedRealm.builder("distributed_realm_with_unavailable_realm")
                    .withRealms("properties_realm_1", "properties_realm_2", "unavailable_ldap_realm", "properties_realm_3")
                    .build());
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName("distributed_realm_domain_with_unavailable_realm")
                    .withDefaultRealm("distributed_realm_with_unavailable_realm")
                    .withPermissionMapper("default-permission-mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm("distributed_realm_with_unavailable_realm")
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                    .withName(DEPLOYMENT_FOR_UNAVAILABLE_REALM)
                    .withSecurityDomain("distributed_realm_domain_with_unavailable_realm")
                    .build());

            return configurableElements.toArray(new ConfigurableElement[configurableElements.size()]);
        }
    }
}
