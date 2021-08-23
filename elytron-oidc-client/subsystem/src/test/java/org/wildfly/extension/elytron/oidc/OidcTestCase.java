/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.elytron.oidc;

import static org.junit.Assert.assertEquals;
import static org.wildfly.extension.elytron.oidc.ElytronOidcSubsystemDefinition.ELYTRON_CAPABILITY_NAME;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Subsystem parsing test case.
 *
 * <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class OidcTestCase extends AbstractSubsystemTest {

    private OidcConfigService configService;
    private KernelServices services = null;

    public OidcTestCase() {
        super(ElytronOidcExtension.SUBSYSTEM_NAME, new ElytronOidcExtension());
    }

    @Before
    public void prepare() throws Throwable {
        if (services != null) return;
        String subsystemXml = "oidc.xml";
        services = super.createKernelServicesBuilder(new DefaultInitializer()).setSubsystemXmlResource(subsystemXml).build();
        if (! services.isSuccessfulBoot()) {
            Assert.fail(services.getBootError().toString());
        }
        configService = OidcConfigService.getInstance();
    }

    @Test
    public void testSecureDeploymentWithSecretCredential() throws Exception {
        String expectedJson =
                "{\"realm\" : \"master\", \"realm-public-key\" : \"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC4siLKUew0WYxdtq6/rwk4Uj/4amGFFnE/yzIxQVU0PUqz3QBRVkUWpDj0K6ZnS5nzJV/y6DHLEy7hjZTdRDphyF1sq09aDOYnVpzu8o2sIlMM8q5RnUyEfIyUZqwo8pSZDJ90fS0s+IDUJNCSIrAKO3w1lqZDHL6E/YFHXyzkvQIDAQAB\", \"auth-server-url\" : \"http://localhost:8080/auth\", \"truststore\" : \"truststore.jks\", \"truststore-password\" : \"secret\", \"ssl-required\" : \"EXTERNAL\", \"confidential-port\" : 443, \"allow-any-hostname\" : false, \"disable-trust-manager\" : true, \"connection-pool-size\" : 20, \"enable-cors\" : true, \"client-keystore\" : \"keys.jks\", \"client-keystore-password\" : \"secret\", \"client-key-password\" : \"secret\", \"cors-max-age\" : 600, \"cors-allowed-headers\" : \"X-Custom\", \"cors-allowed-methods\" : \"PUT,POST,DELETE,GET\", \"expose-token\" : false, \"always-refresh-token\" : false, \"register-node-at-startup\" : true, \"register-node-period\" : 60, \"token-store\" : \"session\", \"principal-attribute\" : \"sub\", \"proxy-url\" : \"http://localhost:9000\", \"resource\" : \"myAppId\", \"use-resource-role-mappings\" : true, \"turn-off-change-session-id-on-login\" : false, \"token-minimum-time-to-live\" : 10, \"min-time-between-jwks-requests\" : 20, \"public-key-cache-ttl\" : 3600, \"verify-token-audience\" : true, \"credentials\" : {\"secret\" : \"0aa31d98-e0aa-404c-b6e0-e771dba1e798\"}, \"redirect-rewrite-rules\" : {\"^/wsmaster/api/(.*)$\" : \"/api/$1/\"}}";
        assertEquals(expectedJson, configService.getJSON("myAppWithSecret"));
    }

    @Test
    public void testSecureDeploymentWithJwtCredential() throws Exception {
        String expectedJson =
                "{\"realm\" : \"master\", \"realm-public-key\" : \"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC4siLKUew0WYxdtq6/rwk4Uj/4amGFFnE/yzIxQVU0PUqz3QBRVkUWpDj0K6ZnS5nzJV/y6DHLEy7hjZTdRDphyF1sq09aDOYnVpzu8o2sIlMM8q5RnUyEfIyUZqwo8pSZDJ90fS0s+IDUJNCSIrAKO3w1lqZDHL6E/YFHXyzkvQIDAQAB\", \"auth-server-url\" : \"http://localhost:8080/auth\", \"truststore\" : \"truststore.jks\", \"truststore-password\" : \"secret\", \"ssl-required\" : \"EXTERNAL\", \"confidential-port\" : 443, \"allow-any-hostname\" : false, \"disable-trust-manager\" : true, \"connection-pool-size\" : 20, \"enable-cors\" : true, \"client-keystore\" : \"keys.jks\", \"client-keystore-password\" : \"secret\", \"client-key-password\" : \"secret\", \"cors-max-age\" : 600, \"cors-allowed-headers\" : \"X-Custom\", \"cors-allowed-methods\" : \"PUT,POST,DELETE,GET\", \"expose-token\" : false, \"always-refresh-token\" : false, \"register-node-at-startup\" : true, \"register-node-period\" : 60, \"token-store\" : \"session\", \"principal-attribute\" : \"sub\", \"proxy-url\" : \"http://localhost:9000\", \"resource\" : \"http-endpoint\", \"use-resource-role-mappings\" : true, \"adapter-state-cookie-path\" : \"/\", \"credentials\" : { \"jwt\" : {\"client-keystore-file\" : \"/tmp/keystore.jks\", \"client-keystore-type\" : \"jks\", \"client-keystore-password\" : \"keystorePassword\", \"client-key-password\" : \"keyPassword\", \"token-timeout\" : \"10\", \"client-key-alias\" : \"keyAlias\"} }, \"redirect-rewrite-rules\" : {\"^/wsmaster/api/(.*)$\" : \"/api/$1/\"}}";
        assertEquals(expectedJson, configService.getJSON("myAppWithJwt"));
    }

    @Test
    public void testSecureDeploymentWithSecretJwtCredential() throws Exception {
        String expectedJson =
                "{\"realm\" : \"master\", \"realm-public-key\" : \"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC4siLKUew0WYxdtq6/rwk4Uj/4amGFFnE/yzIxQVU0PUqz3QBRVkUWpDj0K6ZnS5nzJV/y6DHLEy7hjZTdRDphyF1sq09aDOYnVpzu8o2sIlMM8q5RnUyEfIyUZqwo8pSZDJ90fS0s+IDUJNCSIrAKO3w1lqZDHL6E/YFHXyzkvQIDAQAB\", \"auth-server-url\" : \"http://localhost:8080/auth\", \"truststore\" : \"truststore.jks\", \"truststore-password\" : \"secret\", \"ssl-required\" : \"EXTERNAL\", \"confidential-port\" : 443, \"allow-any-hostname\" : false, \"disable-trust-manager\" : true, \"connection-pool-size\" : 20, \"enable-cors\" : true, \"client-keystore\" : \"keys.jks\", \"client-keystore-password\" : \"secret\", \"client-key-password\" : \"secret\", \"cors-max-age\" : 600, \"cors-allowed-headers\" : \"X-Custom\", \"cors-allowed-methods\" : \"PUT,POST,DELETE,GET\", \"expose-token\" : false, \"always-refresh-token\" : false, \"register-node-at-startup\" : true, \"register-node-period\" : 60, \"token-store\" : \"session\", \"principal-attribute\" : \"sub\", \"proxy-url\" : \"http://localhost:9000\", \"resource\" : \"some-endpoint\", \"use-resource-role-mappings\" : true, \"adapter-state-cookie-path\" : \"/\", \"credentials\" : { \"secret-jwt\" : {\"secret\" : \"fd8f54e1-6994-413a-acf8-90bc67f05412\", \"algorithm\" : \"HS512\"} }, \"redirect-rewrite-rules\" : {\"^/wsmaster/api/(.*)$\" : \"/api/$1/\"}}";
        assertEquals(expectedJson, configService.getJSON("myAppWithSecretJwt"));
    }

    @Test
    public void testSecureDeploymentWithRealmInlined() throws Exception {
        String expectedJson =
                "{\"realm\" : \"demo\", \"resource\" : \"customer-portal\", \"auth-server-url\" : \"http://localhost:8081/auth\", \"ssl-required\" : \"external\", \"credentials\" : {\"secret\" : \"password\"}}";
        assertEquals(expectedJson, configService.getJSON("myAppWithRealmInline"));
    }

    @Test
    public void testSecureDeploymentWithProvider() throws Exception {
        String expectedJson =
                "{\"provider-url\" : \"https://accounts.google.com\", \"ssl-required\" : \"external\", \"principal-attribute\" : \"firstName\", \"client-id\" : \"customer-portal\", \"credentials\" : {\"secret\" : \"password\"}}";
        assertEquals(expectedJson, configService.getJSON("myAppWithProvider"));
    }

    private static class DefaultInitializer extends AdditionalInitialization {

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
            registerCapabilities(capabilityRegistry, ELYTRON_CAPABILITY_NAME);
        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.NORMAL;
        }

    }

}