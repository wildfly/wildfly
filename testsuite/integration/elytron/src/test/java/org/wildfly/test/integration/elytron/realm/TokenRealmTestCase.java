/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.realm;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.jboss.as.test.integration.security.common.Utils.makeCallWithTokenAuthn;
import static org.jboss.as.test.shared.CliUtils.asAbsolutePath;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.wildfly.security.auth.permission.LoginPermission;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.FileAuditLog;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;
import org.wildfly.test.security.common.elytron.PermissionRef;
import org.wildfly.test.security.common.elytron.RegexPrincipalTransformer;
import org.wildfly.test.security.common.elytron.SimpleHttpAuthenticationFactory;
import org.wildfly.test.security.common.elytron.SimplePermissionMapper;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.TokenRealm;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

/**
 * Authentication tests for Elytron Token Realm.
 *
 * @author Ashwin Mehendale <Ashwin.Mehendale@ibm.com>
 */

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({
        TokenRealmTestCase.ServerSetupTask.class,
        StabilityServerSetupSnapshotRestoreTasks.Community.class
})
public class TokenRealmTestCase {

    private static final String DEPLOYMENT = "TokenRealmDeployment";
    private static final String DEPLOYMENT_PRINCIPAL_TRANSFORMER = "TokenRealmPrincipalTransformerDeployment";
    private static final String INDEX_PAGE_CONTENT_REQUESTED = "index page content";
    private static final String ALLOWED_PRINCIPAL = "elytron@wildfly.org";
    private static final String DENIED_PRINCIPAL = "intruder@wildfly.org";
    private static final String PERMISSION_MAPPER = "token_realm_permission_mapper";
    private static final String PERMISSION_MAPPER_PRINCIPAL_TRANSFORMER = "token_realm_principal_transformer_permission_mapper";
    private static final String PRINCIPAL_TRANSFORMER = "elytron-custom-principal-transformer";
    private static final String TOKEN_REALM_NAME = "token_realm";
    private static final String TOKEN_REALM_DOMAIN = "token_realm_domain";
    private static final String TOKEN_REALM_PRINCIPAL_TRANSFORMER_NAME = "token_realm_principal_transformer";
    private static final String TOKEN_REALM_PRINCIPAL_TRANSFORMER_DOMAIN = "token_realm_principal_transformer_domain";
    private static final Encoder B64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String JWT_HEADER_B64 = B64_ENCODER.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    private static final String JWT_ISSUER_1 = "issuer1.wildfly.org";

    private static final File AUDIT_LOG_FILE = Paths.get("target", TokenRealmTestCase.class.getSimpleName() + "-test-audit.log").toFile();

    @Deployment(name = DEPLOYMENT)
    public static WebArchive deploymentForEvidence() {
        return deployment(DEPLOYMENT,"token-realm-web.xml");
    }

    @Deployment(name = DEPLOYMENT_PRINCIPAL_TRANSFORMER)
    public static WebArchive deploymentForEvidenceWithPrincipalTransformer() {
        return deployment(DEPLOYMENT_PRINCIPAL_TRANSFORMER,"token-realm-web.xml");
    }

    private static WebArchive deployment(String name, String webXml) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, name + ".war");
        war.add(new StringAsset(INDEX_PAGE_CONTENT_REQUESTED),"index.html");
        war.addAsWebInfResource(TokenRealmTestCase.class.getPackage(), webXml, "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(name),"jboss-web.xml");
        return war;
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testTokenRealm(@ArquillianResource URL webAppUrl) throws Exception {
        String result = makeCallWithTokenAuthn(webAppUrl, createJwtToken(ALLOWED_PRINCIPAL, "userA", JWT_ISSUER_1), SC_OK);
        assertEquals(INDEX_PAGE_CONTENT_REQUESTED, result);

        result = makeCallWithTokenAuthn(webAppUrl, createJwtToken(DENIED_PRINCIPAL, "userA", JWT_ISSUER_1), SC_FORBIDDEN);
        assertNotEquals(INDEX_PAGE_CONTENT_REQUESTED, result);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_PRINCIPAL_TRANSFORMER)
    public void testTokenRealmWithPrincipalTransformer(@ArquillianResource URL webAppUrl) throws Exception {
        String result = makeCallWithTokenAuthn(webAppUrl, createJwtToken(ALLOWED_PRINCIPAL, "userA", JWT_ISSUER_1), SC_OK);
        assertEquals(INDEX_PAGE_CONTENT_REQUESTED, result);

        result = makeCallWithTokenAuthn(webAppUrl, createJwtToken(DENIED_PRINCIPAL, "userA", JWT_ISSUER_1), SC_FORBIDDEN);
        assertNotEquals(INDEX_PAGE_CONTENT_REQUESTED, result);
    }

    private String createJwtToken(String principal, String userName, String issuer) {
        String jwtPayload = String.format("{" //
                + "\"iss\": \"%1$s\"," //
                + "\"sub\": \"%3$s\"," //
                + "\"exp\": 2051222399," //
                + "\"aud\": \"%1$s\"," //
                + "\"groups\": [\"%2$s\"]" //
                + "}", issuer, userName, principal);
        return JWT_HEADER_B64 + "." + B64_ENCODER.encodeToString(jwtPayload.getBytes(StandardCharsets.UTF_8)) + ".";
    }

    static class ServerSetupTask extends AbstractElytronSetupTask {
        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ArrayList<ConfigurableElement> configurableElements = new ArrayList<>();

            configurableElements.add(FileAuditLog.builder()
                .withName("audit_log_for_token_realm")
                .withPath(asAbsolutePath(AUDIT_LOG_FILE))
                .build());

            //Build token realm without principal transformer
            configurableElements.add(TokenRealm.builder(TOKEN_REALM_NAME)
                .withJwt(TokenRealm.jwtBuilder().withIssuer(JWT_ISSUER_1).build())
                .withPrincipalClaim("sub")
                .build());

            //Grant only ALLOWED_PRINCIPAL LoginPermission
            configurableElements.add(SimplePermissionMapper.builder()
                    .withName(PERMISSION_MAPPER)
                    .permissionMapping(SimplePermissionMapper.PermissionMapping.builder()
                            .withPrincipals("\"" + ALLOWED_PRINCIPAL + "\"")
                            .withPermissions(PermissionRef.builder()
                                    .className(LoginPermission.class.getName())
                                    .build())
                            .build())
                    .build());

            //Configure a security domain
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName(TOKEN_REALM_DOMAIN)
                    .withDefaultRealm(TOKEN_REALM_NAME)
                    .withPermissionMapper(PERMISSION_MAPPER)
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm(TOKEN_REALM_NAME)
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());

            //Create an http auth factory
            configurableElements.add(SimpleHttpAuthenticationFactory.builder()
                    .withName(DEPLOYMENT)
                    .withHttpServerMechanismFactory("global")
                    .withSecurityDomain(TOKEN_REALM_DOMAIN)
                    .addMechanismConfiguration(MechanismConfiguration.builder()
                            .withMechanismName("BEARER_TOKEN")
                            .build())
                    .build());

            //Configure an application security domain
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                    .withName(DEPLOYMENT)
                    .httpAuthenticationFactory(DEPLOYMENT)
                    .build());

            //Prepare principal transformer
            configurableElements.add(RegexPrincipalTransformer.builder(PRINCIPAL_TRANSFORMER)
                    .withPattern(ALLOWED_PRINCIPAL)
                    .withReplacement(ALLOWED_PRINCIPAL.toUpperCase())
                    .build());

            //Build token realm with a principal transforer
            configurableElements.add(TokenRealm.builder(TOKEN_REALM_PRINCIPAL_TRANSFORMER_NAME)
                .withJwt(TokenRealm.jwtBuilder().withIssuer(JWT_ISSUER_1).build())
                .withPrincipalClaim("sub")
                .withPrincipalTransformer(PRINCIPAL_TRANSFORMER)
                .build());

            //Grant only ALLOWED_PRINCIPAL LoginPermission
            configurableElements.add(SimplePermissionMapper.builder()
                    .withName(PERMISSION_MAPPER_PRINCIPAL_TRANSFORMER)
                    .permissionMapping(SimplePermissionMapper.PermissionMapping.builder()
                            .withPrincipals("\"" + ALLOWED_PRINCIPAL.toUpperCase() + "\"")
                            .withPermissions(PermissionRef.builder()
                                    .className(LoginPermission.class.getName())
                                    .build())
                            .build())
                    .build());

            //Configure security domain
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName(TOKEN_REALM_PRINCIPAL_TRANSFORMER_DOMAIN)
                    .withDefaultRealm(TOKEN_REALM_PRINCIPAL_TRANSFORMER_NAME)
                    .withPermissionMapper(PERMISSION_MAPPER_PRINCIPAL_TRANSFORMER)
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm(TOKEN_REALM_PRINCIPAL_TRANSFORMER_NAME)
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());

            //Create an http auth factory
            configurableElements.add(SimpleHttpAuthenticationFactory.builder()
                    .withName(DEPLOYMENT_PRINCIPAL_TRANSFORMER)
                    .withHttpServerMechanismFactory("global")
                    .withSecurityDomain(TOKEN_REALM_PRINCIPAL_TRANSFORMER_DOMAIN)
                    .addMechanismConfiguration(MechanismConfiguration.builder()
                            .withMechanismName("BEARER_TOKEN")
                            .build())
                    .build());

            //Configure an application security domain
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                    .withName(DEPLOYMENT_PRINCIPAL_TRANSFORMER)
                    .httpAuthenticationFactory(DEPLOYMENT_PRINCIPAL_TRANSFORMER)
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
