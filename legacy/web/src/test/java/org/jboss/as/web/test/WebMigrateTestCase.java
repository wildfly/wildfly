/*
* JBoss, Home of Professional Open Source.
* Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web.test;

import static org.jboss.as.controller.capability.RuntimeCapability.buildDynamicCapabilityName;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TRUSTSTORE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import io.undertow.predicate.PredicateParser;
import io.undertow.server.handlers.builder.PredicatedHandlersParser;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.access.management.ManagementSecurityIdentitySupplier;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.audit.EnvironmentNameReader;
import org.jboss.as.domain.management.security.KeystoreAttributes;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.web.WebExtension;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.extension.io.IOExtension;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;

/**
 * @author Stuart Douglas
 */
public class WebMigrateTestCase extends AbstractSubsystemTest {

    public static final String UNDERTOW_SUBSYSTEM_NAME = "undertow";

    private static final char[] GENERATED_KEYSTORE_PASSWORD = "changeit".toCharArray();

    private static final String CLIENT_ALIAS = "client";
    private static final String CLIENT_2_ALIAS = "client2";
    private static final String TEST_ALIAS = "test";

    private static final String WORKING_DIRECTORY_LOCATION = "./target/test-classes";

    private static final File KEY_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, "server.keystore");
    private static final File TRUST_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, "jsse.keystore");

    private static final String TEST_CLIENT_DN = "CN=Test Client, OU=JBoss, O=Red Hat, L=Raleigh, ST=North Carolina, C=US";
    private static final String TEST_CLIENT_2_DN = "CN=Test Client 2, OU=JBoss, O=Red Hat, L=Raleigh, ST=North Carolina, C=US";
    private static final String AS_7_DN = "CN=AS7, OU=JBoss, O=Red Hat, L=Raleigh, ST=North Carolina, C=US";

    private static final String SHA_1_RSA = "SHA1withRSA";

    private static KeyStore loadKeyStore() throws Exception{
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        return ks;
    }

    private static SelfSignedX509CertificateAndSigningKey createSelfSigned(String DN) {
        return SelfSignedX509CertificateAndSigningKey.builder()
                .setDn(new X500Principal(DN))
                .setKeyAlgorithmName("RSA")
                .setSignatureAlgorithmName(SHA_1_RSA)
                .build();
    }

    private static void addKeyEntry(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey, KeyStore keyStore) throws Exception {
        X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        keyStore.setKeyEntry(TEST_ALIAS, selfSignedX509CertificateAndSigningKey.getSigningKey(), GENERATED_KEYSTORE_PASSWORD, new X509Certificate[]{certificate});
    }

    private static void addCertEntry(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey, String alias, KeyStore keyStore) throws Exception {
        X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        keyStore.setCertificateEntry(alias, certificate);
    }

    private static void createTemporaryKeyStoreFile(KeyStore keyStore, File outputFile) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(outputFile)){
            keyStore.store(fos, GENERATED_KEYSTORE_PASSWORD);
        }
    }

    private static void setUpKeyStores() throws Exception {
        File workingDir = new File(WORKING_DIRECTORY_LOCATION);
        if (workingDir.exists() == false) {
            workingDir.mkdirs();
        }

        KeyStore keyStore = loadKeyStore();
        KeyStore trustStore = loadKeyStore();

        SelfSignedX509CertificateAndSigningKey testClientSelfSignedX509CertificateAndSigningKey = createSelfSigned(TEST_CLIENT_DN);
        SelfSignedX509CertificateAndSigningKey testClient2SelfSignedX509CertificateAndSigningKey = createSelfSigned(TEST_CLIENT_2_DN);
        SelfSignedX509CertificateAndSigningKey aS7SelfSignedX509CertificateAndSigningKey = createSelfSigned(AS_7_DN);

        addCertEntry(testClient2SelfSignedX509CertificateAndSigningKey, CLIENT_2_ALIAS, keyStore);
        addCertEntry(testClientSelfSignedX509CertificateAndSigningKey, CLIENT_ALIAS, keyStore);
        addKeyEntry(aS7SelfSignedX509CertificateAndSigningKey, keyStore);

        addCertEntry(testClient2SelfSignedX509CertificateAndSigningKey, CLIENT_2_ALIAS, trustStore);
        addCertEntry(testClientSelfSignedX509CertificateAndSigningKey, CLIENT_ALIAS, trustStore);

        createTemporaryKeyStoreFile(keyStore, KEY_STORE_FILE);
        createTemporaryKeyStoreFile(trustStore, TRUST_STORE_FILE);
    }

    private static void deleteKeyStoreFiles() {
        File[] testFiles = {
                KEY_STORE_FILE,
                TRUST_STORE_FILE
        };
        for (File file : testFiles) {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        setUpKeyStores();
    }

    @AfterClass
    public static void afterTests() {
        deleteKeyStoreFiles();
    }

    public WebMigrateTestCase() {
        super(WebExtension.SUBSYSTEM_NAME, new WebExtension());
    }

    @Test
    public void testMigrateOperation() throws Exception {
        String subsystemXml = readResource("subsystem-migrate-2.2.0.xml");
        NewSubsystemAdditionalInitialization additionalInitialization = new NewSubsystemAdditionalInitialization();
        KernelServices services = createKernelServicesBuilder(additionalInitialization).setSubsystemXml(subsystemXml).build();

        ModelNode model = services.readWholeModel();
        assertFalse(additionalInitialization.extensionAdded);
        assertTrue(model.get(SUBSYSTEM, WebExtension.SUBSYSTEM_NAME).isDefined());
        assertFalse(model.get(SUBSYSTEM, UNDERTOW_SUBSYSTEM_NAME).isDefined());

        ModelNode migrateOp = new ModelNode();
        migrateOp.get(OP).set("migrate");
        migrateOp.get(OP_ADDR).add(SUBSYSTEM, WebExtension.SUBSYSTEM_NAME);

        checkOutcome(services.executeOperation(migrateOp));

        model = services.readWholeModel();

        assertTrue(additionalInitialization.extensionAdded);
        assertFalse(model.get(SUBSYSTEM, WebExtension.SUBSYSTEM_NAME).isDefined());
        assertTrue(model.get(SUBSYSTEM, UNDERTOW_SUBSYSTEM_NAME).isDefined());

        //make sure we have an IO subsystem
        ModelNode ioSubsystem = model.get(SUBSYSTEM, "io");
        assertTrue(ioSubsystem.isDefined());
        assertTrue(ioSubsystem.get("worker", "default").isDefined());
        assertTrue(ioSubsystem.get("buffer-pool", "default").isDefined());

        ModelNode newSubsystem = model.get(SUBSYSTEM, UNDERTOW_SUBSYSTEM_NAME);
        ModelNode newServer = newSubsystem.get("server", "default-server");
        assertNotNull(newServer);
        assertTrue(newServer.isDefined());
        assertEquals("default-host", newServer.get(Constants.DEFAULT_HOST).asString());


        //servlet container
        ModelNode servletContainer = newSubsystem.get(Constants.SERVLET_CONTAINER, "default");
        assertNotNull(servletContainer);
        assertTrue(servletContainer.isDefined());
        assertEquals("${prop.default-session-timeout:30}", servletContainer.get(Constants.DEFAULT_SESSION_TIMEOUT).asString());
        assertEquals("${prop.listings:true}", servletContainer.get(Constants.DIRECTORY_LISTING).asString());

        //jsp settings
        ModelNode jsp = servletContainer.get(Constants.SETTING, Constants.JSP);
        assertNotNull(jsp);
        assertEquals("${prop.recompile-on-fail:true}", jsp.get(Constants.RECOMPILE_ON_FAIL).asString());

        //welcome file
        ModelNode welcome = servletContainer.get(Constants.WELCOME_FILE, "toto");
        assertTrue(welcome.isDefined());

        //mime mapping
        ModelNode mimeMapping = servletContainer.get(Constants.MIME_MAPPING, "ogx");
        assertTrue(mimeMapping.isDefined());
        assertEquals("application/ogg", mimeMapping.get(Constants.VALUE).asString());

        //http connector
        ModelNode connector = newServer.get(Constants.HTTP_LISTENER, "http");
        assertTrue(connector.isDefined());
        assertEquals("http", connector.get(Constants.SOCKET_BINDING).asString());
        assertEquals("${prop.enabled:true}", connector.get(Constants.ENABLED).asString());
        assertEquals("${prop.enable-lookups:false}", connector.get(Constants.RESOLVE_PEER_ADDRESS).asString());
        assertEquals("${prop.max-post-size:2097153}", connector.get(Constants.MAX_POST_SIZE).asString());
        assertEquals("https", connector.get(Constants.REDIRECT_SOCKET).asString());

        //https connector
        ModelNode httpsConnector = newServer.get(Constants.HTTPS_LISTENER, "https");
        String realmName = httpsConnector.get(Constants.SECURITY_REALM).asString();
        assertTrue(realmName, realmName.startsWith("jbossweb-migration-security-realm"));
        assertEquals("${prop.session-cache-size:512}", httpsConnector.get(Constants.SSL_SESSION_CACHE_SIZE).asString());
        assertEquals("REQUESTED", httpsConnector.get(Constants.VERIFY_CLIENT).asString());

        //realm name is dynamic
        ModelNode realm = model.get(CORE_SERVICE, MANAGEMENT).get(SECURITY_REALM, realmName);

        //trust store
        ModelNode trustStore = realm.get(AUTHENTICATION, TRUSTSTORE);
        assertEquals("${file-base}/jsse.keystore", trustStore.get(KeystoreAttributes.KEYSTORE_PATH.getName()).asString());
        //Valves
        ModelNode filters = newSubsystem.get(Constants.CONFIGURATION, Constants.FILTER);
        ModelNode dumpFilter = filters.get("expression-filter", "request-dumper");
        assertEquals("dump-request", dumpFilter.get("expression").asString());
        validateExpressionFilter(dumpFilter);

        ModelNode remoteAddrFilter = filters.get("expression-filter", "remote-addr");
        assertEquals("access-control(acl={'192.168.1.20 deny', '127.0.0.1 allow', '127.0.0.2 allow'} , attribute=%a)", remoteAddrFilter.get("expression").asString());
        validateExpressionFilter(remoteAddrFilter);

        ModelNode stuckFilter = filters.get("expression-filter", "stuck");
        assertEquals("stuck-thread-detector(threshhold='1000')", stuckFilter.get("expression").asString());
        validateExpressionFilter(stuckFilter);

        ModelNode proxyFilter = filters.get("expression-filter", "proxy");
        assertEquals("regex(pattern=\"proxy1|proxy2\", value=%{i,x-forwarded-for}, full-match=true) and regex(pattern=\"192\\.168\\.0\\.10|192\\.168\\.0\\.11\", value=%{i,x-forwarded-for}, full-match=true) -> proxy-peer-address", proxyFilter.get("expression").asString());
        validateExpressionFilter(proxyFilter);

        ModelNode crawler = servletContainer.get(Constants.SETTING, Constants.CRAWLER_SESSION_MANAGEMENT);
        assertTrue(crawler.isDefined());
        assertEquals(1, crawler.get(Constants.SESSION_TIMEOUT).asInt());
        assertEquals("Google", crawler.get(Constants.USER_AGENTS).asString());


        //virtual host
        ModelNode virtualHost = newServer.get(Constants.HOST, "default-host");
        //welcome content
        assertEquals("welcome-content", virtualHost.get("location", "/").get(Constants.HANDLER).asString());

        assertEquals("localhost", virtualHost.get("alias").asList().get(0).asString());
        assertTrue(virtualHost.hasDefined(Constants.FILTER_REF, "request-dumper"));
        assertTrue(virtualHost.hasDefined(Constants.FILTER_REF, "remote-addr"));
        assertTrue(virtualHost.hasDefined(Constants.FILTER_REF, "proxy"));
        assertTrue(virtualHost.hasDefined(Constants.FILTER_REF, "stuck"));
        assertFalse(virtualHost.hasDefined(Constants.FILTER_REF, "myvalve"));

        ModelNode accessLog = virtualHost.get(Constants.SETTING, Constants.ACCESS_LOG);

        assertEquals("prefix", accessLog.get(Constants.PREFIX).asString());
        assertEquals("true", accessLog.get(Constants.ROTATE).asString());
        assertEquals("extended", accessLog.get(Constants.PATTERN).asString());
        assertEquals("toto", accessLog.get(Constants.DIRECTORY).asString());
        assertEquals("jboss.server.base.dir", accessLog.get(Constants.RELATIVE_TO).asString());

        //sso
        ModelNode sso = virtualHost.get(Constants.SETTING, Constants.SINGLE_SIGN_ON);
        assertEquals("${prop.domain:myDomain}", sso.get(Constants.DOMAIN).asString());
        assertEquals("${prop.http-only:true}", sso.get(Constants.HTTP_ONLY).asString());

        //global access log valve
        virtualHost = newServer.get(Constants.HOST, "vs1");
        assertTrue(virtualHost.hasDefined(Constants.FILTER_REF, "request-dumper"));
        assertTrue(virtualHost.hasDefined(Constants.FILTER_REF, "remote-addr"));
        assertFalse(virtualHost.hasDefined(Constants.FILTER_REF, "myvalve"));
        assertTrue(virtualHost.hasDefined(Constants.FILTER_REF, "proxy"));
        assertTrue(virtualHost.hasDefined(Constants.FILTER_REF, "stuck"));
        accessLog = virtualHost.get(Constants.SETTING, Constants.ACCESS_LOG);

        assertEquals("myapp_access_log.", accessLog.get(Constants.PREFIX).asString());
        assertEquals(".log", accessLog.get(Constants.SUFFIX).asString());
        assertEquals("true", accessLog.get(Constants.ROTATE).asString());
        assertEquals("common", accessLog.get(Constants.PATTERN).asString());
        assertEquals("${jboss.server.log.dir}", accessLog.get(Constants.DIRECTORY).asString());
        assertEquals("exists(%{r,log-enabled})", accessLog.get(Constants.PREDICATE).asString());

        //proxy valve
        virtualHost = newServer.get(Constants.HOST, "vs1");
        assertTrue(virtualHost.hasDefined(Constants.FILTER_REF, "request-dumper"));
        assertTrue(virtualHost.hasDefined(Constants.FILTER_REF, "remote-addr"));
        assertFalse(virtualHost.hasDefined(Constants.FILTER_REF, "myvalve"));
        assertTrue(virtualHost.hasDefined(Constants.FILTER_REF, "proxy"));
        assertTrue(virtualHost.hasDefined(Constants.FILTER_REF, "stuck"));

        assertEquals("myapp_access_log.", accessLog.get(Constants.PREFIX).asString());
        assertEquals(".log", accessLog.get(Constants.SUFFIX).asString());
        assertEquals("true", accessLog.get(Constants.ROTATE).asString());
        assertEquals("common", accessLog.get(Constants.PATTERN).asString());
        assertEquals("${jboss.server.log.dir}", accessLog.get(Constants.DIRECTORY).asString());
        assertEquals("exists(%{r,log-enabled})", accessLog.get(Constants.PREDICATE).asString());

    }

    private void validateExpressionFilter(ModelNode filter) {
        PredicatedHandlersParser.parse(filter.get("expression").asString(), PredicateParser.class.getClassLoader());
    }

    private static class NewSubsystemAdditionalInitialization extends AdditionalInitialization {

        UndertowExtension undertow = new UndertowExtension();
        IOExtension io = new IOExtension();
        boolean extensionAdded = false;

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {

            final OperationDefinition removeExtension = new SimpleOperationDefinitionBuilder("remove", new StandardResourceDescriptionResolver("test", "test", getClass().getClassLoader()))
                    .build();


            PathElement webExtension = PathElement.pathElement(EXTENSION, "org.jboss.as.web");
            rootRegistration.registerSubModel(new SimpleResourceDefinition(webExtension, ControllerResolver.getResolver(EXTENSION)))
                    .registerOperationHandler(removeExtension, new ReloadRequiredRemoveStepHandler());
            rootResource.registerChild(webExtension, Resource.Factory.create());


            rootRegistration.registerSubModel(new SimpleResourceDefinition(PathElement.pathElement(EXTENSION),
                    ControllerResolver.getResolver(EXTENSION), new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    if (!extensionAdded) {
                        extensionAdded = true;
                        undertow.initialize(extensionRegistry.getExtensionContext("org.wildfly.extension.undertow",
                                rootRegistration, ExtensionRegistryType.SERVER));
                        io.initialize(extensionRegistry.getExtensionContext("org.wildfly.extension.io",
                                rootRegistration, ExtensionRegistryType.SERVER));
                    }
                }
            }, null));
            rootRegistration.registerSubModel(CoreManagementResourceDefinition.forStandaloneServer(new DelegatingConfigurableAuthorizer(), new ManagementSecurityIdentitySupplier(),
                    null, null, new EnvironmentNameReader() {
                public boolean isServer() {
                    return true;
                }

                public String getServerName() {
                    return "Test";
                }

                public String getHostName() {
                    return null;
                }

                public String getProductName() {
                    return null;
                }
            }, null));
            rootResource.registerChild(CoreManagementResourceDefinition.PATH_ELEMENT, Resource.Factory.create());
            System.setProperty("file-base", new File(getClass().getClassLoader().getResource("server.keystore").getFile()).getParentFile().getAbsolutePath());

            Map<String, Class> capabilities = new HashMap<>();
            final String SOCKET_CAPABILITY = "org.wildfly.network.socket-binding";

            capabilities.put(buildDynamicCapabilityName(SOCKET_CAPABILITY, "http"), SocketBinding.class);
            capabilities.put(buildDynamicCapabilityName(SOCKET_CAPABILITY, "https"), SocketBinding.class);

            registerServiceCapabilities(capabilityRegistry, capabilities);
        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.ADMIN_ONLY;
        }

        @Override
        protected ProcessType getProcessType() {
            return ProcessType.SELF_CONTAINED;
        }

        @Override
        protected void setupController(ControllerInitializer controllerInitializer) {
            controllerInitializer.addPath("jboss.controller.temp.dir", System.getProperty("java.io.tmpdir"), null);
        }

    }
}
