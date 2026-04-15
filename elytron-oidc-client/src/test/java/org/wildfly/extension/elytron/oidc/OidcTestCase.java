/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.junit.Assert.assertEquals;
import static org.wildfly.extension.elytron.oidc.ElytronOidcSubsystemDefinition.ELYTRON_CAPABILITY_NAME;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.version.Stability;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Subsystem parsing test case.
 *
 * <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class OidcTestCase extends AbstractSubsystemSchemaTest<ElytronOidcSubsystemSchema> {

    private OidcConfigService configService;
    private KernelServices services = null;

    public OidcTestCase() {
        super(ElytronOidcExtension.SUBSYSTEM_NAME, new ElytronOidcExtension(), ElytronOidcSubsystemSchema.VERSION_3_0_PREVIEW, ElytronOidcSubsystemSchema.CURRENT.get(Stability.PREVIEW));
    }

    @Before
    public void prepare() throws Throwable {
        if (services != null) return;
        String subsystemXml = "oidc.xml";
        services = super.createKernelServicesBuilder(new DefaultInitializer(this.getSubsystemSchema().getStability())).setSubsystemXmlResource(subsystemXml).build();
        if (! services.isSuccessfulBoot()) {
            Assert.fail(services.getBootError().toString());
        }
        configService = OidcConfigService.getInstance();
    }

    @Test
    public void testSecureDeploymentWithSecretCredential() throws Exception {
        String expectedJson =
                "{\"realm\" : \"main\", \"realm-public-key\" : \"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC4siLKUew0WYxdtq6/rwk4Uj/4amGFFnE/yzIxQVU0PUqz3QBRVkUWpDj0K6ZnS5nzJV/y6DHLEy7hjZTdRDphyF1sq09aDOYnVpzu8o2sIlMM8q5RnUyEfIyUZqwo8pSZDJ90fS0s+IDUJNCSIrAKO3w1lqZDHL6E/YFHXyzkvQIDAQAB\", \"auth-server-url\" : \"http://localhost:8080/auth\", \"truststore\" : \"truststore.jks\", \"truststore-password\" : \"secret\", \"ssl-required\" : \"EXTERNAL\", \"confidential-port\" : 443, \"allow-any-hostname\" : false, \"disable-trust-manager\" : true, \"connection-pool-size\" : 20, \"enable-cors\" : true, \"client-keystore\" : \"keys.jks\", \"client-keystore-password\" : \"secret\", \"client-key-password\" : \"secret\", \"cors-max-age\" : 600, \"cors-allowed-headers\" : \"X-Custom\", \"cors-allowed-methods\" : \"PUT,POST,DELETE,GET\", \"expose-token\" : false, \"always-refresh-token\" : false, \"register-node-at-startup\" : true, \"register-node-period\" : 60, \"token-store\" : \"session\", \"principal-attribute\" : \"sub\", \"proxy-url\" : \"http://localhost:9000\", \"resource\" : \"myAppId\", \"use-resource-role-mappings\" : true, \"turn-off-change-session-id-on-login\" : false, \"token-minimum-time-to-live\" : 10, \"min-time-between-jwks-requests\" : 20, \"public-key-cache-ttl\" : 3600, \"verify-token-audience\" : true, \"credentials\" : {\"secret\" : \"0aa31d98-e0aa-404c-b6e0-e771dba1e798\"}, \"redirect-rewrite-rules\" : {\"^/wsmain/api/(.*)$\" : \"/api/$1/\"}}";
        assertEquals(expectedJson, configService.getJSON("myAppWithSecret.war"));
    }

    @Test
    public void testSecureDeploymentWithJwtCredential() throws Exception {
        String expectedJson =
                "{\"realm\" : \"main\", \"realm-public-key\" : \"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC4siLKUew0WYxdtq6/rwk4Uj/4amGFFnE/yzIxQVU0PUqz3QBRVkUWpDj0K6ZnS5nzJV/y6DHLEy7hjZTdRDphyF1sq09aDOYnVpzu8o2sIlMM8q5RnUyEfIyUZqwo8pSZDJ90fS0s+IDUJNCSIrAKO3w1lqZDHL6E/YFHXyzkvQIDAQAB\", \"auth-server-url\" : \"http://localhost:8080/auth\", \"truststore\" : \"truststore.jks\", \"truststore-password\" : \"secret\", \"ssl-required\" : \"EXTERNAL\", \"confidential-port\" : 443, \"allow-any-hostname\" : false, \"disable-trust-manager\" : true, \"connection-pool-size\" : 20, \"enable-cors\" : true, \"client-keystore\" : \"keys.jks\", \"client-keystore-password\" : \"secret\", \"client-key-password\" : \"secret\", \"cors-max-age\" : 600, \"cors-allowed-headers\" : \"X-Custom\", \"cors-allowed-methods\" : \"PUT,POST,DELETE,GET\", \"expose-token\" : false, \"always-refresh-token\" : false, \"register-node-at-startup\" : true, \"register-node-period\" : 60, \"token-store\" : \"session\", \"principal-attribute\" : \"sub\", \"proxy-url\" : \"http://localhost:9000\", \"resource\" : \"http-endpoint\", \"use-resource-role-mappings\" : true, \"adapter-state-cookie-path\" : \"/\", \"credentials\" : { \"jwt\" : {\"client-keystore-file\" : \"/tmp/keystore.jks\", \"client-keystore-type\" : \"jks\", \"client-keystore-password\" : \"keystorePassword\", \"client-key-password\" : \"keyPassword\", \"token-timeout\" : \"10\", \"client-key-alias\" : \"keyAlias\"} }, \"redirect-rewrite-rules\" : {\"^/wsmain/api/(.*)$\" : \"/api/$1/\"}}";
        assertEquals(expectedJson, configService.getJSON("myAppWithJwt.war"));
    }

    @Test
    public void testSecureDeploymentWithSecretJwtCredential() throws Exception {
        String expectedJson =
                "{\"realm\" : \"main\", \"realm-public-key\" : \"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC4siLKUew0WYxdtq6/rwk4Uj/4amGFFnE/yzIxQVU0PUqz3QBRVkUWpDj0K6ZnS5nzJV/y6DHLEy7hjZTdRDphyF1sq09aDOYnVpzu8o2sIlMM8q5RnUyEfIyUZqwo8pSZDJ90fS0s+IDUJNCSIrAKO3w1lqZDHL6E/YFHXyzkvQIDAQAB\", \"auth-server-url\" : \"http://localhost:8080/auth\", \"truststore\" : \"truststore.jks\", \"truststore-password\" : \"secret\", \"ssl-required\" : \"EXTERNAL\", \"confidential-port\" : 443, \"allow-any-hostname\" : false, \"disable-trust-manager\" : true, \"connection-pool-size\" : 20, \"enable-cors\" : true, \"client-keystore\" : \"keys.jks\", \"client-keystore-password\" : \"secret\", \"client-key-password\" : \"secret\", \"cors-max-age\" : 600, \"cors-allowed-headers\" : \"X-Custom\", \"cors-allowed-methods\" : \"PUT,POST,DELETE,GET\", \"expose-token\" : false, \"always-refresh-token\" : false, \"register-node-at-startup\" : true, \"register-node-period\" : 60, \"token-store\" : \"session\", \"principal-attribute\" : \"sub\", \"proxy-url\" : \"http://localhost:9000\", \"resource\" : \"some-endpoint\", \"use-resource-role-mappings\" : true, \"adapter-state-cookie-path\" : \"/\", \"credentials\" : { \"secret-jwt\" : {\"secret\" : \"fd8f54e1-6994-413a-acf8-90bc67f05412\", \"algorithm\" : \"HS512\"} }, \"redirect-rewrite-rules\" : {\"^/wsmain/api/(.*)$\" : \"/api/$1/\"}}";
        assertEquals(expectedJson, configService.getJSON("myAppWithSecretJwt.war"));
    }

    @Test
    public void testSecureDeploymentWithRealmInlined() throws Exception {
        String expectedJson =
                "{\"realm\" : \"demo\", \"resource\" : \"customer-portal\", \"auth-server-url\" : \"http://localhost:8081/auth\", \"ssl-required\" : \"EXTERNAL\", \"credentials\" : {\"secret\" : \"password\"}}";
        assertEquals(expectedJson, configService.getJSON("myAppWithRealmInline.war"));
    }

    @Test
    public void testSecureDeploymentWithProvider() throws Exception {
        String expectedJson =
                "{\"provider-url\" : \"https://accounts.google.com\", \"ssl-required\" : \"EXTERNAL\", \"principal-attribute\" : \"firstName\", \"client-id\" : \"customer-portal\", \"credentials\" : {\"secret\" : \"password\"}}";
        assertEquals(expectedJson, configService.getJSON("myAppWithProvider.war"));
    }

    @Test
    public void testSecureServerWithProvider() throws Exception {
        String expectedJson =
                "{\"provider-url\" : \"http://localhost:8080/realms/WildFly\", \"client-id\" : \"wildfly-console\", \"public-client\" : true, \"ssl-required\" : \"EXTERNAL\"}";
        assertEquals(expectedJson, configService.getJSON("another-wildfly-console"));
    }

    @Test
    public void testSecureDeploymentWithScopes() throws Exception {
        String expectedJson =
                "{\"provider-url\" : \"http://localhost:8080/realms/WildFly\", \"client-id\" : \"wildfly-console\", \"public-client\" : true, \"scope\" : \"profile email phone\", \"ssl-required\" : \"EXTERNAL\"}";
        assertEquals(expectedJson, configService.getJSON("wildfly-with-scope"));
    }

    @Test
    public void testSecureServerWithRealm() throws Exception {
        String expectedJson =
                "{\"realm\" : \"jboss-infra\", \"realm-public-key\" : \"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqKoq+a9MgXepmsPJDmo45qswuChW9pWjanX68oIBuI4hGvhQxFHryCow230A+sr7tFdMQMt8f1l/ysmV/fYAuW29WaoY4kI4Ou1yYPuwywKSsxT6PooTs83hKyZ1h4LZMj5DkLGDDDyVRHob2WmPaYg9RGVRw3iGGsD/p+Yb+L/gnBYQnZZ7lYqmN7h36p5CkzzlgXQA1Ha8sQxL+rJNH8+sZm0vBrKsoII3Of7TqHGsm1RwFV3XCuGJ7S61AbjJMXL5DQgJl9Z5scvxGAyoRLKC294UgMnQdzyBTMPw2GybxkRKmiK2KjQKmcopmrJp/Bt6fBR6ZkGSs9qUlxGHgwIDAQAB\", \"auth-server-url\" : \"http://localhost:8180/auth\", \"resource\" : \"wildfly-console\", \"public-client\" : true, \"adapter-state-cookie-path\" : \"/\", \"ssl-required\" : \"EXTERNAL\", \"confidential-port\" : 443, \"proxy-url\" : \"http://localhost:9000\"}";
        assertEquals(expectedJson, configService.getJSON("wildfly-console"));
    }

    @Test
    public void testSecureServerWithScopes() throws Exception {
        String expectedJson =
                "{\"client-id\" : \"wildfly-console\", \"public-client\" : true, \"scope\" : \"profile email phone\", \"provider-url\" : \"http://localhost:8080/realms/WildFly\", \"ssl-required\" : \"EXTERNAL\"}";
        assertEquals(expectedJson, configService.getJSON("wildfly-server-with-scope"));
    }

    @Test
    public void testSecureServerWithRequest() throws Exception {
        String expectedJson =
                "{\"client-id\" : \"wildfly-console\", \"public-client\" : false, \"provider-url\" : \"http://localhost:8080/realms/WildFly\", \"ssl-required\" : \"EXTERNAL\", \"authentication-request-format\" : \"request\", \"request-object-signing-algorithm\" : \"RS-256\", \"request-object-encryption-enc-value\" : \"A128CBC-HS256\", \"request-object-encryption-alg-value\" : \"RSA-OAEP\", \"request-object-signing-keystore-file\" : \"jwt.keystore\", \"request-object-signing-keystore-password\" : \"password\", \"request-object-signing-key-alias\" : \"alias\", \"request-object-signing-key-password\" : \"password\", \"request-object-signing-keystore-type\" : \"JKS\", \"credentials\" : {\"secret\" : \"password\"}}";
        assertEquals(expectedJson, configService.getJSON("wildfly-server-with-request"));
    }

    @Test
    public void testSecureServerWithRequestUri() throws Exception {
        String expectedJson =
                "{\"client-id\" : \"wildfly-console\", \"public-client\" : false, \"provider-url\" : \"http://localhost:8080/realms/WildFly\", \"ssl-required\" : \"EXTERNAL\", \"authentication-request-format\" : \"request_uri\", \"request-object-signing-algorithm\" : \"RS-256\", \"request-object-encryption-enc-value\" : \"A128CBC-HS256\", \"request-object-encryption-alg-value\" : \"RSA-OAEP\", \"request-object-signing-keystore-file\" : \"jwt.keystore\", \"request-object-signing-keystore-password\" : \"password\", \"request-object-signing-key-alias\" : \"alias\", \"request-object-signing-key-password\" : \"password\", \"request-object-signing-keystore-type\" : \"JKS\", \"credentials\" : {\"secret\" : \"password\"}}";
        assertEquals(expectedJson, configService.getJSON("wildfly-server-with-request-uri"));
    }

    @Test
    public void testSecureDeploymentWithRequest() throws Exception {
        String expectedJson =
                "{\"client-id\" : \"wildfly-console\", \"public-client\" : false, \"provider-url\" : \"http://localhost:8080/realms/WildFly\", \"ssl-required\" : \"EXTERNAL\", \"authentication-request-format\" : \"request\", \"request-object-signing-algorithm\" : \"RS-256\", \"request-object-encryption-enc-value\" : \"A128CBC-HS256\", \"request-object-encryption-alg-value\" : \"RSA-OAEP\", \"request-object-signing-keystore-file\" : \"jwt.keystore\", \"request-object-signing-keystore-password\" : \"password\", \"request-object-signing-key-alias\" : \"alias\", \"request-object-signing-key-password\" : \"password\", \"request-object-signing-keystore-type\" : \"JKS\", \"credentials\" : {\"secret\" : \"password\"}}";
        assertEquals(expectedJson, configService.getJSON("wildfly-with-request"));
    }

    @Test
    public void testSecureDeploymentWithRequestUri() throws Exception {
        String expectedJson =
                "{\"client-id\" : \"wildfly-console\", \"public-client\" : false, \"provider-url\" : \"http://localhost:8080/realms/WildFly\", \"ssl-required\" : \"EXTERNAL\", \"authentication-request-format\" : \"request_uri\", \"request-object-signing-algorithm\" : \"RS-256\", \"request-object-encryption-enc-value\" : \"A128CBC-HS256\", \"request-object-encryption-alg-value\" : \"RSA-OAEP\", \"request-object-signing-keystore-file\" : \"jwt.keystore\", \"request-object-signing-keystore-password\" : \"password\", \"request-object-signing-key-alias\" : \"alias\", \"request-object-signing-key-password\" : \"password\", \"request-object-signing-keystore-type\" : \"JKS\", \"credentials\" : {\"secret\" : \"password\"}}";
        assertEquals(expectedJson, configService.getJSON("wildfly-with-request-uri"));
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) {
        //
    }

    protected static class DefaultInitializer extends AdditionalInitialization {

        private final Stability stability;

        public DefaultInitializer(Stability stability) {
            this.stability = stability;
        }
        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
            registerCapabilities(capabilityRegistry, ELYTRON_CAPABILITY_NAME);
        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.NORMAL;
        }

        @Override
        public Stability getStability() {
            return stability;
        }

    }

}