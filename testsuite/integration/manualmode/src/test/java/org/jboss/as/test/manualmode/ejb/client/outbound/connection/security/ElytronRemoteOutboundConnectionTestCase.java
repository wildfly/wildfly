package org.jboss.as.test.manualmode.ejb.client.outbound.connection.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLAIN_TEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SASL_AUTHENTICATION_FACTORY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL_CONTEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.Provider;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.naming.InitialContext;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.EJBClientConnection;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.protocol.remote.RemoteTransportProvider;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.sasl.SaslMechanismSelector;
import org.wildfly.test.api.Authentication;

/**
 * Test case pertaining to remote outbound connection authentication between two server instances using remote-outbound-connection
 * management resource with Elytron authentication context.
 *
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2017 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ElytronRemoteOutboundConnectionTestCase {

    private static final Logger log = Logger.getLogger(ElytronRemoteOutboundConnectionTestCase.class);

    private static final String INBOUND_CONNECTION_MODULE_NAME = "inbound-module";
    private static final String OUTBOUND_CONNECTION_MODULE_NAME = "outbound-module";

    private static final String INBOUND_CONNECTION_SERVER = "inbound-server";
    private static final String OUTBOUND_CONNECTION_SERVER = "outbound-server";

    private static final String EJB_SERVER_DEPLOYMENT = "ejb-server-deployment";
    private static final String EJB_CLIENT_DEPLOYMENT = "ejb-client-deployment";

    private static final String RESOURCE_PREFIX = "ejb-remote-tests";
    private static final String PROPERTIES_REALM = RESOURCE_PREFIX + "-properties-realm";
    private static final String SECURITY_DOMAIN = RESOURCE_PREFIX + "-security-domain";
    private static final String AUTHENTICATION_FACTORY = RESOURCE_PREFIX + "-sasl-authentication";
    private static final String APPLICATION_SECURITY_DOMAIN = RESOURCE_PREFIX;
    private static final String INBOUND_SOCKET_BINDING = RESOURCE_PREFIX + "-socket-binding";
    private static final String CONNECTOR = RESOURCE_PREFIX + "-connector";
    private static final String OUTBOUND_SOCKET_BINDING = RESOURCE_PREFIX + "-outbound-socket-binding";
    private static final String DEFAULT_AUTH_CONFIG = RESOURCE_PREFIX + "-default-auth-config";
    private static final String DEFAULT_AUTH_CONTEXT = RESOURCE_PREFIX + "-default-auth-context";
    private static final String OVERRIDING_AUTH_CONFIG = RESOURCE_PREFIX + "-overriding-auth-config";
    private static final String OVERRIDING_AUTH_CONTEXT = RESOURCE_PREFIX + "-overriding-auth-context";
    private static final String REMOTE_OUTBOUND_CONNECTION = RESOURCE_PREFIX + "-remote-outbound-connection";
    private static final String SERVER_KEY_STORE = RESOURCE_PREFIX + "-server-key-store";
    private static final String SERVER_KEY_MANAGER = RESOURCE_PREFIX + "-server-key-manager";
    private static final String SERVER_TRUST_STORE = RESOURCE_PREFIX + "-server-trust-store";
    private static final String SERVER_TRUST_MANAGER = RESOURCE_PREFIX + "-server-trust-manager";
    private static final String SERVER_SSL_CONTEXT = RESOURCE_PREFIX + "-server-ssl-context";
    private static final String DEFAULT_KEY_STORE = RESOURCE_PREFIX + "-default-key-store";
    private static final String DEFAULT_KEY_MANAGER = RESOURCE_PREFIX + "-default-key-manager";
    private static final String DEFAULT_TRUST_STORE = RESOURCE_PREFIX + "-default-trust-store";
    private static final String DEFAULT_TRUST_MANAGER = RESOURCE_PREFIX + "-default-trust-manager";
    private static final String DEFAULT_SERVER_SSL_CONTEXT = RESOURCE_PREFIX + "-default-server-ssl-context";
    private static final String OVERRIDING_KEY_STORE = RESOURCE_PREFIX + "-overriding-key-store";
    private static final String OVERRIDING_KEY_MANAGER = RESOURCE_PREFIX + "-overriding-key-manager";
    private static final String OVERRIDING_TRUST_STORE = RESOURCE_PREFIX + "-overriding-trust-store";
    private static final String OVERRIDING_TRUST_MANAGER = RESOURCE_PREFIX + "-overriding-trust-manager";
    private static final String OVERRIDING_SERVER_SSL_CONTEXT = RESOURCE_PREFIX + "-overriding-server-ssl-context";

    private static final String DEFAULT_USERNAME = "ejbRemoteTests";
    private static final String DEFAULT_PASSWORD = "ejbRemoteTestsPassword";
    private static final String OVERRIDING_USERNAME = "ejbRemoteTestsOverriding";
    private static final String OVERRIDING_PASSWORD = "ejbRemoteTestsPasswordOverriding";

    private static final String KEY_STORE_KEYPASS = SecurityTestConstants.KEYSTORE_PASSWORD;
    private static final File WORKDIR = new File(new File("").getAbsoluteFile().getAbsolutePath() + File.separatorChar + "target"
            + File.separatorChar + RESOURCE_PREFIX);
    private static final String SERVER_KEY_STORE_PATH = new File(WORKDIR.getAbsoluteFile(), SecurityTestConstants.SERVER_KEYSTORE)
            .getAbsolutePath();
    private static final String SERVER_TRUST_STORE_PATH = new File(WORKDIR.getAbsoluteFile(), SecurityTestConstants.SERVER_TRUSTSTORE)
            .getAbsolutePath();
    private static final String CLIENT_KEY_STORE_PATH = new File(WORKDIR.getAbsoluteFile(), SecurityTestConstants.CLIENT_KEYSTORE)
            .getAbsolutePath();
    private static final String CLIENT_TRUST_STORE_PATH = new File(WORKDIR.getAbsoluteFile(), SecurityTestConstants.CLIENT_TRUSTSTORE)
            .getAbsolutePath();
    private static final String UNTRUSTED_KEY_STORE_PATH = new File(WORKDIR.getAbsoluteFile(), SecurityTestConstants.UNTRUSTED_KEYSTORE)
            .getAbsolutePath();

    private static final String USERS_PATH = new File(ElytronRemoteOutboundConnectionTestCase.class.getResource("users.properties").
            getFile()).getAbsolutePath();
    private static final String ROLES_PATH = new File(ElytronRemoteOutboundConnectionTestCase.class.getResource("roles.properties").
            getFile()).getAbsolutePath();

    private static final int BARE_REMOTING_PORT = 54447;
    private static final String BARE_REMOTING_PROTOCOL = "remote";
    private static final int SSL_REMOTING_PORT = 54448;
    private static final String SSL_REMOTING_PROTOCOL = "remote";
    private static final int HTTP_REMOTING_PORT = 8080;
    private static final String HTTP_REMOTING_PROTOCOL = "http-remoting";
    private static final int HTTPS_REMOTING_PORT = 8443;
    private static final String HTTPS_REMOTING_PROTOCOL = "https-remoting";

    @ArquillianResource
    private static ContainerController containerController;

    private static ModelControllerClient serverSideMCC;
    private static ModelControllerClient clientSideMCC;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = EJB_SERVER_DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(INBOUND_CONNECTION_SERVER)
    public static Archive<?> createEjbServerDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, INBOUND_CONNECTION_MODULE_NAME + ".jar");
        ejbJar.addClass(WhoAmI.class)
                .addClass(ServerWhoAmI.class);
        return ejbJar;
    }

    @Deployment(name = EJB_CLIENT_DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(OUTBOUND_CONNECTION_SERVER)
    public static Archive<?> createEjbClientDeployment() {
        final JavaArchive ejbClientJar = ShrinkWrap.create(JavaArchive.class, OUTBOUND_CONNECTION_MODULE_NAME + ".jar");
        ejbClientJar.addClass(WhoAmI.class)
                .addClass(IntermediateWhoAmI.class)
                .addAsManifestResource(IntermediateWhoAmI.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        return ejbClientJar;
    }

    @BeforeClass
    public static void prepareSSLFiles() {
        WORKDIR.mkdirs();
        try {
            Utils.createKeyMaterial(WORKDIR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void startContainers() {
        if (!containerController.isStarted(INBOUND_CONNECTION_SERVER)){
            containerController.start(INBOUND_CONNECTION_SERVER);
        }
        if (!containerController.isStarted(OUTBOUND_CONNECTION_SERVER)){
            containerController.start(OUTBOUND_CONNECTION_SERVER);
        }

        serverSideMCC = getInboundConnectionServerMCC();
        clientSideMCC = getOutboundConnectionServerMCC();
    }

    @After
    public void cleanResources()throws Exception {
        deployer.undeploy(EJB_CLIENT_DEPLOYMENT);
        deployer.undeploy(EJB_SERVER_DEPLOYMENT);

        //==================================
        // Client-side server tear down
        //==================================
        boolean clientReloadRequired = false;
        ModelNode result;
        try {
            result = clientSideMCC.execute(Util.getReadAttributeOperation(PathAddress.pathAddress(SUBSYSTEM, "elytron"),
                    "default-authentication-context"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (result != null && result.hasDefined(RESULT)) {
            applyUpdate(clientSideMCC, Util.getUndefineAttributeOperation(PathAddress.pathAddress(SUBSYSTEM, "elytron"),
                    "default-authentication-context"));
            clientReloadRequired = true;
        }
        removeIfExists(clientSideMCC, getConnectionAddress(REMOTE_OUTBOUND_CONNECTION), !clientReloadRequired);
        removeIfExists(clientSideMCC, getAuthenticationContextAddress(DEFAULT_AUTH_CONTEXT), !clientReloadRequired);
        removeIfExists(clientSideMCC, getServerSSLContextAddress(DEFAULT_SERVER_SSL_CONTEXT), !clientReloadRequired);
        removeIfExists(clientSideMCC, getTrustManagerAddress(DEFAULT_TRUST_MANAGER), !clientReloadRequired);
        removeIfExists(clientSideMCC, getKeyStoreAddress(DEFAULT_TRUST_STORE), !clientReloadRequired);
        removeIfExists(clientSideMCC, getKeyManagerAddress(DEFAULT_KEY_MANAGER), !clientReloadRequired);
        removeIfExists(clientSideMCC, getKeyStoreAddress(DEFAULT_KEY_STORE), !clientReloadRequired);
        removeIfExists(clientSideMCC, getAuthenticationConfigurationAddress(DEFAULT_AUTH_CONFIG), !clientReloadRequired);
        removeIfExists(clientSideMCC, getAuthenticationContextAddress(OVERRIDING_AUTH_CONTEXT), !clientReloadRequired);
        removeIfExists(clientSideMCC, getServerSSLContextAddress(OVERRIDING_SERVER_SSL_CONTEXT), !clientReloadRequired);
        removeIfExists(clientSideMCC, getTrustManagerAddress(OVERRIDING_TRUST_MANAGER), !clientReloadRequired);
        removeIfExists(clientSideMCC, getKeyStoreAddress(OVERRIDING_TRUST_STORE), !clientReloadRequired);
        removeIfExists(clientSideMCC, getKeyManagerAddress(OVERRIDING_KEY_MANAGER), !clientReloadRequired);
        removeIfExists(clientSideMCC, getKeyStoreAddress(OVERRIDING_KEY_STORE), !clientReloadRequired);
        removeIfExists(clientSideMCC, getAuthenticationConfigurationAddress(OVERRIDING_AUTH_CONFIG), !clientReloadRequired);
        removeIfExists(clientSideMCC, getOutboundSocketBindingAddress(OUTBOUND_SOCKET_BINDING), !clientReloadRequired);
        if (clientReloadRequired) {
            executeBlockingReloadClientServer(clientSideMCC);
        }

        //==================================
        // Server-side server tear down
        //==================================
        if (!executeReadAttributeOpReturnResult(serverSideMCC, getHttpConnectorAddress("http-remoting-connector"), SASL_AUTHENTICATION_FACTORY)
                .equals("application-sasl-authentication")) {
            applyUpdate(serverSideMCC, Util.getWriteAttributeOperation(getHttpConnectorAddress("http-remoting-connector"),
                    SASL_AUTHENTICATION_FACTORY, "application-sasl-authentication"));
        }
        Operations.CompositeOperationBuilder compositeBuilder = Operations.CompositeOperationBuilder.create();

        String defaultHttpsListenerSSLContext = executeReadAttributeOpReturnResult(serverSideMCC, getDefaultHttpsListenerAddress(), SSL_CONTEXT);
        if (!(defaultHttpsListenerSSLContext == null) && !defaultHttpsListenerSSLContext.isEmpty()) {
            ModelNode update = Util.getUndefineAttributeOperation(getDefaultHttpsListenerAddress(), SSL_CONTEXT);
            update.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            compositeBuilder.addStep(update);
        }

        ModelNode update = Util.getWriteAttributeOperation(getDefaultHttpsListenerAddress(), SECURITY_REALM, "ApplicationRealm");
        update.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        compositeBuilder.addStep(update);

        applyUpdate(serverSideMCC, compositeBuilder.build().getOperation());

        removeIfExists(serverSideMCC, getConnectorAddress(CONNECTOR));
        removeIfExists(serverSideMCC, getHttpConnectorAddress(CONNECTOR));
        removeIfExists(serverSideMCC, getServerSSLContextAddress(SERVER_SSL_CONTEXT), false);
        removeIfExists(serverSideMCC, getTrustManagerAddress(SERVER_TRUST_MANAGER));
        removeIfExists(serverSideMCC, getKeyStoreAddress(SERVER_TRUST_STORE));
        removeIfExists(serverSideMCC, getKeyManagerAddress(SERVER_KEY_MANAGER));
        removeIfExists(serverSideMCC, getKeyStoreAddress(SERVER_KEY_STORE));
        removeIfExists(serverSideMCC, getSocketBindingAddress(INBOUND_SOCKET_BINDING));
        removeIfExists(serverSideMCC, getEjbApplicationSecurityDomainAddress(APPLICATION_SECURITY_DOMAIN));
        removeIfExists(serverSideMCC, getSaslAuthenticationFactoryAddress(AUTHENTICATION_FACTORY));
        removeIfExists(serverSideMCC, getElytronSecurityDomainAddress(SECURITY_DOMAIN));
        removeIfExists(serverSideMCC, getPropertiesRealmAddress(PROPERTIES_REALM));

        ServerReload.reloadIfRequired(serverSideMCC);

        try {
            clientSideMCC.close();
            serverSideMCC.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void shutdownContainers() {

        if (containerController.isStarted(INBOUND_CONNECTION_SERVER)) {
            containerController.stop(INBOUND_CONNECTION_SERVER);
        }
        if (containerController.isStarted(OUTBOUND_CONNECTION_SERVER)) {
            containerController.stop(OUTBOUND_CONNECTION_SERVER);
        }

        cleanFile(WORKDIR);
    }

    /**
     * Test verifying that the authentication context host configuration overwrites host configuration in socket binding in remote
     * outbound connection referenced from deployment.
     *
     * The test uses remoting protocol.
     */
    @Test
    public void testAuthenticationHostConfigWithBareRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundBareRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddressNode1(),
                54321));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(DEFAULT_AUTH_CONFIG, BARE_REMOTING_PROTOCOL, PROPERTIES_REALM,
                DEFAULT_USERNAME, DEFAULT_PASSWORD, TestSuiteEnvironment.getServerAddress(), BARE_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(DEFAULT_AUTH_CONTEXT, DEFAULT_AUTH_CONFIG));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, ""));
        applyUpdate(clientSideMCC, getWriteElytronDefaultAuthenticationContextOp(DEFAULT_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(DEFAULT_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that if no legacy security is used in remoting outbound connection referenced from deployment and no Elytron
     * authentication context is used, the connection will fall back to using Elytron default authentication context.
     *
     * The test uses remoting protocol.
     */
    @Test
    public void testElytronDefaultContextWithBareRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundBareRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddress(),
                BARE_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(DEFAULT_AUTH_CONFIG, BARE_REMOTING_PROTOCOL, PROPERTIES_REALM,
                DEFAULT_USERNAME, DEFAULT_PASSWORD));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(DEFAULT_AUTH_CONTEXT, DEFAULT_AUTH_CONFIG));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, ""));
        applyUpdate(clientSideMCC, getWriteElytronDefaultAuthenticationContextOp(DEFAULT_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(DEFAULT_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that the authentication context defined in remote outbound connection referenced from deployment overrides the
     * Elytron default authentication context.
     *
     * The test uses remoting protocol.
     */
    @Test
    public void testOverridingElytronDefaultContextWithBareRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundBareRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddress(),
                BARE_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(DEFAULT_AUTH_CONFIG, BARE_REMOTING_PROTOCOL, PROPERTIES_REALM,
                DEFAULT_USERNAME, DEFAULT_PASSWORD));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(DEFAULT_AUTH_CONTEXT, DEFAULT_AUTH_CONFIG));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(OVERRIDING_AUTH_CONFIG, BARE_REMOTING_PROTOCOL, PROPERTIES_REALM,
                OVERRIDING_USERNAME, OVERRIDING_PASSWORD));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(OVERRIDING_AUTH_CONTEXT, OVERRIDING_AUTH_CONFIG));
        applyUpdate(clientSideMCC, getWriteElytronDefaultAuthenticationContextOp(DEFAULT_AUTH_CONTEXT));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, OVERRIDING_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(OVERRIDING_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that authentication context defined in remote outbound connection referenced from deployment is sufficient and
     * no Elytron default authentication context is required.
     *
     * The test uses remoting protocol.
     */
    @Test
    public void testConnectionContextWithBareRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundBareRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddress(),
                BARE_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(OVERRIDING_AUTH_CONFIG, BARE_REMOTING_PROTOCOL, PROPERTIES_REALM,
                OVERRIDING_USERNAME, OVERRIDING_PASSWORD));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(OVERRIDING_AUTH_CONTEXT, OVERRIDING_AUTH_CONFIG));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, OVERRIDING_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(OVERRIDING_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that the authentication context host configuration overwrites host configuration in socket binding in remote
     * outbound connection referenced from deployment.
     *
     * The test uses remoting protocol with two-side SSL authentication being enforced.
     */
    @Test
    public void testAuthenticationHostConfigWithSSLRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundSSLRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddressNode1(),
                54321));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(DEFAULT_AUTH_CONFIG, SSL_REMOTING_PROTOCOL, PROPERTIES_REALM,
                DEFAULT_USERNAME, DEFAULT_PASSWORD, TestSuiteEnvironment.getServerAddress(), SSL_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(DEFAULT_KEY_STORE, CLIENT_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyManagerOp(DEFAULT_KEY_MANAGER, DEFAULT_KEY_STORE, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(DEFAULT_TRUST_STORE, CLIENT_TRUST_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddTrustManagerOp(DEFAULT_TRUST_MANAGER, DEFAULT_TRUST_STORE));
        applyUpdate(clientSideMCC, getAddServerSSLContextOp(DEFAULT_SERVER_SSL_CONTEXT, DEFAULT_KEY_MANAGER, DEFAULT_TRUST_MANAGER));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(DEFAULT_AUTH_CONTEXT, DEFAULT_AUTH_CONFIG, DEFAULT_SERVER_SSL_CONTEXT));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, ""));
        applyUpdate(clientSideMCC, getWriteElytronDefaultAuthenticationContextOp(DEFAULT_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(DEFAULT_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that if no legacy security is used in remoting outbound connection referenced from deployment and no Elytron
     * authentication context is used, the connection will fall back to using Elytron default authentication context.
     *
     * The test uses remoting protocol with two-side SSL authentication being enforced.
     */
    @Test
    public void testElytronDefaultContextWithSSLRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundSSLRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddress(),
                SSL_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(DEFAULT_AUTH_CONFIG, SSL_REMOTING_PROTOCOL, PROPERTIES_REALM,
                DEFAULT_USERNAME, DEFAULT_PASSWORD, TestSuiteEnvironment.getServerAddress(), SSL_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(DEFAULT_KEY_STORE, CLIENT_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyManagerOp(DEFAULT_KEY_MANAGER, DEFAULT_KEY_STORE, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(DEFAULT_TRUST_STORE, CLIENT_TRUST_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddTrustManagerOp(DEFAULT_TRUST_MANAGER, DEFAULT_TRUST_STORE));
        applyUpdate(clientSideMCC, getAddServerSSLContextOp(DEFAULT_SERVER_SSL_CONTEXT, DEFAULT_KEY_MANAGER, DEFAULT_TRUST_MANAGER));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(DEFAULT_AUTH_CONTEXT, DEFAULT_AUTH_CONFIG, DEFAULT_SERVER_SSL_CONTEXT));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, ""));
        applyUpdate(clientSideMCC, getWriteElytronDefaultAuthenticationContextOp(DEFAULT_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(DEFAULT_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that the authentication context defined in remote outbound connection referenced from deployment overrides the
     * Elytron default authentication context.
     *
     * The test uses remoting protocol with two-side SSL authentication being enforced.
     */
    @Test
    public void testOverridingElytronDefaultContextWithSSLRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundSSLRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddress(),
                SSL_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(DEFAULT_AUTH_CONFIG, SSL_REMOTING_PROTOCOL, PROPERTIES_REALM,
                DEFAULT_USERNAME, DEFAULT_PASSWORD, TestSuiteEnvironment.getServerAddress(), SSL_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(DEFAULT_KEY_STORE, UNTRUSTED_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyManagerOp(DEFAULT_KEY_MANAGER, DEFAULT_KEY_STORE, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(DEFAULT_TRUST_STORE, UNTRUSTED_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddTrustManagerOp(DEFAULT_TRUST_MANAGER, DEFAULT_TRUST_STORE));
        applyUpdate(clientSideMCC, getAddServerSSLContextOp(DEFAULT_SERVER_SSL_CONTEXT, DEFAULT_KEY_MANAGER, DEFAULT_TRUST_MANAGER));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(DEFAULT_AUTH_CONTEXT, DEFAULT_AUTH_CONFIG, DEFAULT_SERVER_SSL_CONTEXT));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(OVERRIDING_AUTH_CONFIG, SSL_REMOTING_PROTOCOL, PROPERTIES_REALM,
                OVERRIDING_USERNAME, OVERRIDING_PASSWORD, TestSuiteEnvironment.getServerAddress(), SSL_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(OVERRIDING_KEY_STORE, CLIENT_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyManagerOp(OVERRIDING_KEY_MANAGER, OVERRIDING_KEY_STORE, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(OVERRIDING_TRUST_STORE, CLIENT_TRUST_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddTrustManagerOp(OVERRIDING_TRUST_MANAGER, OVERRIDING_TRUST_STORE));
        applyUpdate(clientSideMCC, getAddServerSSLContextOp(OVERRIDING_SERVER_SSL_CONTEXT, OVERRIDING_KEY_MANAGER, OVERRIDING_TRUST_MANAGER));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(OVERRIDING_AUTH_CONTEXT, OVERRIDING_AUTH_CONFIG, OVERRIDING_SERVER_SSL_CONTEXT));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, OVERRIDING_AUTH_CONTEXT));
        applyUpdate(clientSideMCC, getWriteElytronDefaultAuthenticationContextOp(DEFAULT_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(OVERRIDING_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that authentication context defined in remote outbound connection referenced from deployment is sufficient and
     * no Elytron default authentication context is required.
     *
     * The test uses remoting protocol with two-side SSL authentication being enforced.
     */
    @Test
    public void testConnectionContextWithSSLRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundSSLRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddress(),
                SSL_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(OVERRIDING_AUTH_CONFIG, SSL_REMOTING_PROTOCOL, PROPERTIES_REALM,
                OVERRIDING_USERNAME, OVERRIDING_PASSWORD, TestSuiteEnvironment.getServerAddress(), SSL_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(OVERRIDING_KEY_STORE, CLIENT_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyManagerOp(OVERRIDING_KEY_MANAGER, OVERRIDING_KEY_STORE, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(OVERRIDING_TRUST_STORE, CLIENT_TRUST_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddTrustManagerOp(OVERRIDING_TRUST_MANAGER, OVERRIDING_TRUST_STORE));
        applyUpdate(clientSideMCC, getAddServerSSLContextOp(OVERRIDING_SERVER_SSL_CONTEXT, OVERRIDING_KEY_MANAGER, OVERRIDING_TRUST_MANAGER));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(OVERRIDING_AUTH_CONTEXT, OVERRIDING_AUTH_CONFIG, OVERRIDING_SERVER_SSL_CONTEXT));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, OVERRIDING_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(OVERRIDING_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that the authentication context host configuration overwrites host configuration in socket binding in remote
     * outbound connection referenced from deployment.
     *
     * The test uses http-remoting protocol.
     */
    @Test
    public void testAuthenticationHostConfigWithHttpRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundHttpRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddressNode1(),
                54321));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(DEFAULT_AUTH_CONFIG, HTTP_REMOTING_PROTOCOL, PROPERTIES_REALM,
                DEFAULT_USERNAME, DEFAULT_PASSWORD, TestSuiteEnvironment.getServerAddress(), HTTP_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(DEFAULT_AUTH_CONTEXT, DEFAULT_AUTH_CONFIG));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, ""));
        applyUpdate(clientSideMCC, getWriteElytronDefaultAuthenticationContextOp(DEFAULT_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(DEFAULT_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that if no legacy security is used in remoting outbound connection referenced from deployment and no Elytron
     * authentication context is used, the connection will fall back to using Elytron default authentication context.
     *
     * The test uses http-remoting protocol.
     */
    @Test
    public void testElytronDefaultContextWithHttpRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundHttpRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddress(),
                HTTP_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(DEFAULT_AUTH_CONFIG, HTTP_REMOTING_PROTOCOL, PROPERTIES_REALM,
                DEFAULT_USERNAME, DEFAULT_PASSWORD));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(DEFAULT_AUTH_CONTEXT, DEFAULT_AUTH_CONFIG));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, ""));
        applyUpdate(clientSideMCC, getWriteElytronDefaultAuthenticationContextOp(DEFAULT_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(DEFAULT_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that the authentication context defined in remote outbound connection referenced from deployment overrides the
     * Elytron default authentication context.
     *
     * The test uses http-remoting protocol.
     */
    @Test
    public void testOverridingElytronDefaultContextWithHttpRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundHttpRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddress(),
                HTTP_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(DEFAULT_AUTH_CONFIG, HTTP_REMOTING_PROTOCOL, PROPERTIES_REALM,
                DEFAULT_USERNAME, DEFAULT_PASSWORD));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(DEFAULT_AUTH_CONTEXT, DEFAULT_AUTH_CONFIG));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(OVERRIDING_AUTH_CONFIG, HTTP_REMOTING_PROTOCOL, PROPERTIES_REALM,
                OVERRIDING_USERNAME, OVERRIDING_PASSWORD));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(OVERRIDING_AUTH_CONTEXT, OVERRIDING_AUTH_CONFIG));
        applyUpdate(clientSideMCC, getWriteElytronDefaultAuthenticationContextOp(DEFAULT_AUTH_CONTEXT));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, OVERRIDING_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(OVERRIDING_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that authentication context defined in remote outbound connection referenced from deployment is sufficient and
     * no Elytron default authentication context is required.
     *
     * The test uses http-remoting protocol.
     */
    @Test
    public void testConnectionContextWithHttpRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundHttpRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddress(),
                HTTP_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(OVERRIDING_AUTH_CONFIG, HTTP_REMOTING_PROTOCOL, PROPERTIES_REALM,
                OVERRIDING_USERNAME, OVERRIDING_PASSWORD));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(OVERRIDING_AUTH_CONTEXT, OVERRIDING_AUTH_CONFIG));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, OVERRIDING_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(OVERRIDING_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that the authentication context host configuration overwrites host configuration in socket binding in remote
     * outbound connection referenced from deployment.
     *
     * The test uses https-remoting protocol with two-side SSL authentication being enforced.
     */
    @Test
    public void testAuthenticationHostConfigWithHttpsRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundHttpsRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddressNode1(),
                54321));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(DEFAULT_AUTH_CONFIG, HTTPS_REMOTING_PROTOCOL, PROPERTIES_REALM,
                DEFAULT_USERNAME, DEFAULT_PASSWORD, TestSuiteEnvironment.getServerAddress(), HTTPS_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(DEFAULT_KEY_STORE, CLIENT_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyManagerOp(DEFAULT_KEY_MANAGER, DEFAULT_KEY_STORE, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(DEFAULT_TRUST_STORE, CLIENT_TRUST_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddTrustManagerOp(DEFAULT_TRUST_MANAGER, DEFAULT_TRUST_STORE));
        applyUpdate(clientSideMCC, getAddServerSSLContextOp(DEFAULT_SERVER_SSL_CONTEXT, DEFAULT_KEY_MANAGER, DEFAULT_TRUST_MANAGER));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(DEFAULT_AUTH_CONTEXT, DEFAULT_AUTH_CONFIG, DEFAULT_SERVER_SSL_CONTEXT));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, ""));
        applyUpdate(clientSideMCC, getWriteElytronDefaultAuthenticationContextOp(DEFAULT_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(DEFAULT_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that if no legacy security is used in remoting outbound connection referenced from deployment and no Elytron
     * authentication context is used, the connection will fall back to using Elytron default authentication context.
     *
     * The test uses https-remoting protocol with two-side SSL authentication being enforced.
     */
    @Test
    public void testElytronDefaultContextWithHttpsRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundHttpsRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddress(),
                SSL_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(DEFAULT_AUTH_CONFIG, HTTPS_REMOTING_PROTOCOL, PROPERTIES_REALM,
                DEFAULT_USERNAME, DEFAULT_PASSWORD, TestSuiteEnvironment.getServerAddress(), HTTPS_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(DEFAULT_KEY_STORE, CLIENT_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyManagerOp(DEFAULT_KEY_MANAGER, DEFAULT_KEY_STORE, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(DEFAULT_TRUST_STORE, CLIENT_TRUST_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddTrustManagerOp(DEFAULT_TRUST_MANAGER, DEFAULT_TRUST_STORE));
        applyUpdate(clientSideMCC, getAddServerSSLContextOp(DEFAULT_SERVER_SSL_CONTEXT, DEFAULT_KEY_MANAGER, DEFAULT_TRUST_MANAGER));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(DEFAULT_AUTH_CONTEXT, DEFAULT_AUTH_CONFIG, DEFAULT_SERVER_SSL_CONTEXT));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, ""));
        applyUpdate(clientSideMCC, getWriteElytronDefaultAuthenticationContextOp(DEFAULT_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(DEFAULT_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that the authentication context defined in remote outbound connection referenced from deployment overrides the
     * Elytron default authentication context.
     *
     * The test uses https-remoting protocol with two-side SSL authentication being enforced.
     */
    @Test
    public void testOverridingElytronDefaultContextWithHttpsRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundHttpsRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddress(),
                HTTPS_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(DEFAULT_AUTH_CONFIG, HTTPS_REMOTING_PROTOCOL, PROPERTIES_REALM,
                DEFAULT_USERNAME, DEFAULT_PASSWORD, TestSuiteEnvironment.getServerAddress(), HTTPS_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(DEFAULT_KEY_STORE, UNTRUSTED_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyManagerOp(DEFAULT_KEY_MANAGER, DEFAULT_KEY_STORE, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(DEFAULT_TRUST_STORE, UNTRUSTED_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddTrustManagerOp(DEFAULT_TRUST_MANAGER, DEFAULT_TRUST_STORE));
        applyUpdate(clientSideMCC, getAddServerSSLContextOp(DEFAULT_SERVER_SSL_CONTEXT, DEFAULT_KEY_MANAGER, DEFAULT_TRUST_MANAGER));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(DEFAULT_AUTH_CONTEXT, DEFAULT_AUTH_CONFIG, DEFAULT_SERVER_SSL_CONTEXT));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(OVERRIDING_AUTH_CONFIG, HTTPS_REMOTING_PROTOCOL, PROPERTIES_REALM,
                OVERRIDING_USERNAME, OVERRIDING_PASSWORD, TestSuiteEnvironment.getServerAddress(), HTTPS_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(OVERRIDING_KEY_STORE, CLIENT_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyManagerOp(OVERRIDING_KEY_MANAGER, OVERRIDING_KEY_STORE, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(OVERRIDING_TRUST_STORE, CLIENT_TRUST_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddTrustManagerOp(OVERRIDING_TRUST_MANAGER, OVERRIDING_TRUST_STORE));
        applyUpdate(clientSideMCC, getAddServerSSLContextOp(OVERRIDING_SERVER_SSL_CONTEXT, OVERRIDING_KEY_MANAGER, OVERRIDING_TRUST_MANAGER));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(OVERRIDING_AUTH_CONTEXT, OVERRIDING_AUTH_CONFIG, OVERRIDING_SERVER_SSL_CONTEXT));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, OVERRIDING_AUTH_CONTEXT));
        applyUpdate(clientSideMCC, getWriteElytronDefaultAuthenticationContextOp(DEFAULT_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(OVERRIDING_USERNAME, callIntermediateWhoAmI());
    }

    /**
     * Test verifying that authentication context defined in remote outbound connection referenced from deployment is sufficient and
     * no Elytron default authentication context is required.
     *
     * The test uses https-remoting protocol with two-side SSL authentication being enforced.
     */
    @Test
    public void testConnectionContextWithHttpsRemoting() {
        //==================================
        // Server-side server setup
        //==================================
        configureServerSideForInboundHttpsRemoting(serverSideMCC);
        //==================================
        // Client-side server setup
        //==================================
        applyUpdate(clientSideMCC, getAddOutboundSocketBindingOp(OUTBOUND_SOCKET_BINDING, TestSuiteEnvironment.getServerAddress(),
                HTTPS_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddAuthenticationConfigurationOp(OVERRIDING_AUTH_CONFIG, HTTPS_REMOTING_PROTOCOL, PROPERTIES_REALM,
                OVERRIDING_USERNAME, OVERRIDING_PASSWORD, TestSuiteEnvironment.getServerAddress(), HTTPS_REMOTING_PORT));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(OVERRIDING_KEY_STORE, CLIENT_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyManagerOp(OVERRIDING_KEY_MANAGER, OVERRIDING_KEY_STORE, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddKeyStoreOp(OVERRIDING_TRUST_STORE, CLIENT_TRUST_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(clientSideMCC, getAddTrustManagerOp(OVERRIDING_TRUST_MANAGER, OVERRIDING_TRUST_STORE));
        applyUpdate(clientSideMCC, getAddServerSSLContextOp(OVERRIDING_SERVER_SSL_CONTEXT, OVERRIDING_KEY_MANAGER, OVERRIDING_TRUST_MANAGER));
        applyUpdate(clientSideMCC, getAddAuthenticationContextOp(OVERRIDING_AUTH_CONTEXT, OVERRIDING_AUTH_CONFIG, OVERRIDING_SERVER_SSL_CONTEXT));
        applyUpdate(clientSideMCC, getAddConnectionOp(REMOTE_OUTBOUND_CONNECTION, OUTBOUND_SOCKET_BINDING, OVERRIDING_AUTH_CONTEXT));
        executeBlockingReloadClientServer(clientSideMCC);

        deployer.deploy(EJB_SERVER_DEPLOYMENT);
        deployer.deploy(EJB_CLIENT_DEPLOYMENT);
        Assert.assertEquals(OVERRIDING_USERNAME, callIntermediateWhoAmI());
    }

    private static PathAddress getSocketBindingAddress(String socketBindingName) {
        return PathAddress.pathAddress()
                .append(SOCKET_BINDING_GROUP, "standard-sockets")
                .append(SOCKET_BINDING, socketBindingName);
    }

    private static ModelNode getAddSocketBindingOp(String socketBindingName, int port) {
        ModelNode addSocketBindingOp = Util.createAddOperation(getSocketBindingAddress(socketBindingName));
        addSocketBindingOp.get(PORT).set(port);
        return addSocketBindingOp;
    }

    private static PathAddress getPropertiesRealmAddress(String realmName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "elytron")
                .append("properties-realm", realmName);
    }

    private static ModelNode getAddPropertiesRealmOp(String realmName, String groupsPropertiesPath, String usersPropertiesPath,
                                                     boolean plainText) {
        ModelNode op = Util.createAddOperation(getPropertiesRealmAddress(realmName));
        op.get("groups-properties", PATH).set(groupsPropertiesPath);
        op.get("users-properties", PATH).set(usersPropertiesPath);
        op.get("users-properties", PLAIN_TEXT).set(plainText);
        return op;
    }

    private static PathAddress getElytronSecurityDomainAddress(String domainName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "elytron")
                .append("security-domain", domainName);
    }

    private static ModelNode getAddElytronSecurityDomainOp(String domainName, String realmName) {
        ModelNode op =  Util.createAddOperation(getElytronSecurityDomainAddress(domainName));
        ModelNode realm = new ModelNode();
        realm.get(REALM).set(realmName);
        realm.get("role-decoder").set("groups-to-roles");
        op.get("realms").setEmptyList().add(realm);
        op.get("default-realm").set(realmName);
        op.get("permission-mapper").set("default-permission-mapper");
        return op;
    }

    private static PathAddress getEjbApplicationSecurityDomainAddress(String ejbDomainName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "ejb3")
                .append("application-security-domain", ejbDomainName);
    }

    private static ModelNode getAddEjbApplicationSecurityDomainOp(String ejbDomainName, String securityDomainName) {
        ModelNode op = Util.createAddOperation(getEjbApplicationSecurityDomainAddress(ejbDomainName));
        op.get("security-domain").set(securityDomainName);
        return op;
    }

    private static PathAddress getSaslAuthenticationFactoryAddress(String factoryName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "elytron")
                .append("sasl-authentication-factory", factoryName);
    }

    private static ModelNode getAddSaslAuthenticationFactoryOp(String factoryName, String securityDomainName,
                                                               String securityRealmName) {
        ModelNode op = Util.createAddOperation(getSaslAuthenticationFactoryAddress(factoryName));
        op.get("sasl-server-factory").set("configured");
        op.get("security-domain").set(securityDomainName);
        ModelNode realmConfig = new ModelNode();
        realmConfig.get("realm-name").set(securityRealmName);
        ModelNode digestMechanism = new ModelNode();
        digestMechanism.get("mechanism-name").set("DIGEST-MD5");
        digestMechanism.get("mechanism-realm-configurations").setEmptyList().add(realmConfig);
        op.get("mechanism-configurations").setEmptyList().add(digestMechanism);
        return op;
    }

    private static PathAddress getConnectorAddress(String connectorName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "remoting")
                .append("connector", connectorName);
    }

    private static ModelNode getAddConnectorOp(String connectorName, String socketBindingName, String factoryName,
                                               String serverSSLContextName) {
        ModelNode addConnectorOp = Util.createAddOperation(getConnectorAddress(connectorName));
        addConnectorOp.get(SOCKET_BINDING).set(socketBindingName);
        addConnectorOp.get(SASL_AUTHENTICATION_FACTORY).set(factoryName);
        if (serverSSLContextName != null && !serverSSLContextName.isEmpty()) {
            addConnectorOp.get(SSL_CONTEXT).set(serverSSLContextName);
        }
        return addConnectorOp;
    }

    private static ModelNode getAddConnectorOp(String connectorName, String socketBindingName, String factoryName) {
        return getAddConnectorOp(connectorName, socketBindingName, factoryName, "");
    }

    private static PathAddress getOutboundSocketBindingAddress(String socketBindingName) {
        return PathAddress.pathAddress()
                .append(SOCKET_BINDING_GROUP, "standard-sockets")
                .append(REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING, socketBindingName);
    }

    private static PathAddress getHttpConnectorAddress(String connectorName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "remoting")
                .append("http-connector", connectorName);
    }

    private static ModelNode getAddHttpConnectorOp(String connectorName, String connectorRef, String factoryName) {
        ModelNode addHttpConnectorOp = Util.createAddOperation(getHttpConnectorAddress(connectorName));
        addHttpConnectorOp.get("connector-ref").set(connectorRef);
        addHttpConnectorOp.get(SASL_AUTHENTICATION_FACTORY).set(factoryName);
        return addHttpConnectorOp;
    }

    private static ModelNode getAddOutboundSocketBindingOp(String socketBindingName, String host, int port) {
        ModelNode addOutboundSocketBindingOp = Util.createAddOperation(getOutboundSocketBindingAddress(socketBindingName));
        addOutboundSocketBindingOp.get(PORT).set(port);
        addOutboundSocketBindingOp.get(HOST).set(host);
        return addOutboundSocketBindingOp;
    }

    private static PathAddress getAuthenticationConfigurationAddress(String configurationName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "elytron")
                .append("authentication-configuration", configurationName);
    }

    private static ModelNode getAddAuthenticationConfigurationOp(String configurationName, String protocol, String realm,
                                                                 String username, String password, String host, int port) {
        ModelNode addAuthenticationConfigurationOp = Util.createAddOperation(getAuthenticationConfigurationAddress(configurationName));
        addAuthenticationConfigurationOp.get("protocol").set(protocol);
        if (port != 0) {
            addAuthenticationConfigurationOp.get("port").set(port);
        }
        addAuthenticationConfigurationOp.get("authentication-name").set(username);
        if (host != null && !host.isEmpty()) {
            addAuthenticationConfigurationOp.get("host").set(host);
        }
        addAuthenticationConfigurationOp.get("sasl-mechanism-selector").set("DIGEST-MD5");
        addAuthenticationConfigurationOp.get(REALM).set(realm);
        ModelNode credentialReference = new ModelNode();
        credentialReference.get("clear-text").set(password);
        addAuthenticationConfigurationOp.get("credential-reference").set(credentialReference);
        return addAuthenticationConfigurationOp;
    }

    private static ModelNode getAddAuthenticationConfigurationOp(String configurationName, String protocol, String realm,
                                                                 String username, String password) {
        return getAddAuthenticationConfigurationOp(configurationName, protocol, realm, username, password, "", 0);
    }

    private static PathAddress getAuthenticationContextAddress(String contextName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "elytron")
                .append("authentication-context", contextName);
    }

    private static ModelNode getAddAuthenticationContextOp(String contextName, String configurationName, String serverSSLContextName) {
        ModelNode addAuthenticationContextOp = Util.createAddOperation(getAuthenticationContextAddress(contextName));
        ModelNode matchRule = new ModelNode();
        matchRule.get("authentication-configuration").set(configurationName);
        if (serverSSLContextName != null && !serverSSLContextName.isEmpty()) {
            matchRule.get(SSL_CONTEXT).set(serverSSLContextName);
        }
        addAuthenticationContextOp.get("match-rules").setEmptyList()
                .add(matchRule);
        return addAuthenticationContextOp;
    }

    private static ModelNode getAddAuthenticationContextOp(String contextName, String configurationName) {
        return getAddAuthenticationContextOp(contextName, configurationName, "");
    }

    private static ModelNode getWriteElytronDefaultAuthenticationContextOp(String authenticationContextName) {
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).setEmptyList()
                .add(SUBSYSTEM, "elytron");
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set("default-authentication-context");
        op.get(VALUE).set(authenticationContextName);
        return op;
    }

    private static PathAddress getConnectionAddress(String connectionName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "remoting")
                .append("remote-outbound-connection", connectionName);
    }

    private static ModelNode getAddConnectionOp(String connectionName, String outboundSocketBindingName,
                                                String authenticationContextName) {
        ModelNode addConnectionOp = Util.createAddOperation(getConnectionAddress(connectionName));
        addConnectionOp.get("outbound-socket-binding-ref").set(outboundSocketBindingName);
        if (authenticationContextName != null && !authenticationContextName.isEmpty()) {
            addConnectionOp.get("authentication-context").set(authenticationContextName);
        }
        return addConnectionOp;
    }

    private static PathAddress getKeyStoreAddress(String keyStoreName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "elytron")
                .append("key-store", keyStoreName);
    }

    private static ModelNode getAddKeyStoreOp(String keyStoreName, String path, String keyPass) {
        ModelNode addKeyStoreOp = Util.createAddOperation(getKeyStoreAddress(keyStoreName));
        addKeyStoreOp.get(PATH).set(path);
        ModelNode credentialReference = new ModelNode();
        credentialReference.get("clear-text").set(keyPass);
        addKeyStoreOp.get("credential-reference").set(credentialReference);
        addKeyStoreOp.get("type").set("JKS");
        return addKeyStoreOp;
    }

    private static PathAddress getKeyManagerAddress(String keyManagerName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "elytron")
                .append("key-manager", keyManagerName);
    }

    private static ModelNode getAddKeyManagerOp(String keyManagerName, String keyStoreName, String keyPass) {
        ModelNode addKeyStoreOp = Util.createAddOperation(getKeyManagerAddress(keyManagerName));
        addKeyStoreOp.get("key-store").set(keyStoreName);
        ModelNode credentialReference = new ModelNode();
        credentialReference.get("clear-text").set(keyPass);
        addKeyStoreOp.get("credential-reference").set(credentialReference);
        return addKeyStoreOp;
    }

    private static PathAddress getTrustManagerAddress(String trustManagerName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "elytron")
                .append("trust-manager", trustManagerName);
    }

    private static ModelNode getAddTrustManagerOp(String trustManagerName, String keyStoreName) {
        ModelNode addKeyStoreOp = Util.createAddOperation(getTrustManagerAddress(trustManagerName));
        addKeyStoreOp.get("key-store").set(keyStoreName);
        return addKeyStoreOp;
    }

    private static PathAddress getServerSSLContextAddress(String serverSSLContextName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "elytron")
                .append("server-ssl-context", serverSSLContextName);
    }

    private static ModelNode getAddServerSSLContextOp(String serverSSLContextName, String keyManagerName, String trustManagerName) {
        ModelNode addServerSSLContextOp = Util.createAddOperation(getServerSSLContextAddress(serverSSLContextName));
        addServerSSLContextOp.get("trust-manager").set(trustManagerName);
        addServerSSLContextOp.get("need-client-auth").set(true);
        addServerSSLContextOp.get("key-manager").set(keyManagerName);
        return addServerSSLContextOp;
    }

    private static PathAddress getDefaultHttpsListenerAddress() {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "undertow")
                .append(SERVER, "default-server")
                .append("https-listener", "https");
    }

    private static void configureServerSideForInboundBareRemoting(ModelControllerClient serverSideMCC) {
        applyUpdate(serverSideMCC, getAddPropertiesRealmOp(PROPERTIES_REALM, ROLES_PATH, USERS_PATH, true));
        applyUpdate(serverSideMCC, getAddElytronSecurityDomainOp(SECURITY_DOMAIN, PROPERTIES_REALM));
        applyUpdate(serverSideMCC, getAddSaslAuthenticationFactoryOp(AUTHENTICATION_FACTORY, SECURITY_DOMAIN, PROPERTIES_REALM));
        applyUpdate(serverSideMCC, getAddEjbApplicationSecurityDomainOp(APPLICATION_SECURITY_DOMAIN, SECURITY_DOMAIN));
        applyUpdate(serverSideMCC, getAddSocketBindingOp(INBOUND_SOCKET_BINDING, BARE_REMOTING_PORT));
        applyUpdate(serverSideMCC, getAddConnectorOp(CONNECTOR, INBOUND_SOCKET_BINDING, AUTHENTICATION_FACTORY));
    }

    private static void configureServerSideForInboundSSLRemoting(ModelControllerClient serverSideMCC) {
        applyUpdate(serverSideMCC, getAddPropertiesRealmOp(PROPERTIES_REALM, ROLES_PATH, USERS_PATH, true));
        applyUpdate(serverSideMCC, getAddElytronSecurityDomainOp(SECURITY_DOMAIN, PROPERTIES_REALM));
        applyUpdate(serverSideMCC, getAddSaslAuthenticationFactoryOp(AUTHENTICATION_FACTORY, SECURITY_DOMAIN, PROPERTIES_REALM));
        applyUpdate(serverSideMCC, getAddEjbApplicationSecurityDomainOp(APPLICATION_SECURITY_DOMAIN, SECURITY_DOMAIN));
        applyUpdate(serverSideMCC, getAddSocketBindingOp(INBOUND_SOCKET_BINDING, SSL_REMOTING_PORT));
        applyUpdate(serverSideMCC, getAddKeyStoreOp(SERVER_KEY_STORE, SERVER_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(serverSideMCC, getAddKeyManagerOp(SERVER_KEY_MANAGER, SERVER_KEY_STORE, KEY_STORE_KEYPASS));
        applyUpdate(serverSideMCC, getAddKeyStoreOp(SERVER_TRUST_STORE, SERVER_TRUST_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(serverSideMCC, getAddTrustManagerOp(SERVER_TRUST_MANAGER, SERVER_TRUST_STORE));
        applyUpdate(serverSideMCC, getAddServerSSLContextOp(SERVER_SSL_CONTEXT, SERVER_KEY_MANAGER, SERVER_TRUST_MANAGER));
        applyUpdate(serverSideMCC, getAddConnectorOp(CONNECTOR, INBOUND_SOCKET_BINDING, AUTHENTICATION_FACTORY, SERVER_SSL_CONTEXT));
    }

    private static void configureServerSideForInboundHttpRemoting(ModelControllerClient serverSideMCC) {
        applyUpdate(serverSideMCC, getAddPropertiesRealmOp(PROPERTIES_REALM, ROLES_PATH, USERS_PATH, true));
        applyUpdate(serverSideMCC, getAddElytronSecurityDomainOp(SECURITY_DOMAIN, PROPERTIES_REALM));
        applyUpdate(serverSideMCC, getAddSaslAuthenticationFactoryOp(AUTHENTICATION_FACTORY, SECURITY_DOMAIN, PROPERTIES_REALM));
        applyUpdate(serverSideMCC, getAddEjbApplicationSecurityDomainOp(APPLICATION_SECURITY_DOMAIN, SECURITY_DOMAIN));
        applyUpdate(serverSideMCC, Util.getWriteAttributeOperation(getHttpConnectorAddress("http-remoting-connector"),
                SASL_AUTHENTICATION_FACTORY, AUTHENTICATION_FACTORY));
        executeBlockingReloadServerSide(serverSideMCC);
    }

    private static void configureServerSideForInboundHttpsRemoting(ModelControllerClient serverSideMCC) {
        applyUpdate(serverSideMCC, getAddPropertiesRealmOp(PROPERTIES_REALM, ROLES_PATH, USERS_PATH, true));
        applyUpdate(serverSideMCC, getAddElytronSecurityDomainOp(SECURITY_DOMAIN, PROPERTIES_REALM));
        applyUpdate(serverSideMCC, getAddSaslAuthenticationFactoryOp(AUTHENTICATION_FACTORY, SECURITY_DOMAIN, PROPERTIES_REALM));
        applyUpdate(serverSideMCC, getAddEjbApplicationSecurityDomainOp(APPLICATION_SECURITY_DOMAIN, SECURITY_DOMAIN));
        applyUpdate(serverSideMCC, getAddKeyStoreOp(SERVER_KEY_STORE, SERVER_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(serverSideMCC, getAddKeyManagerOp(SERVER_KEY_MANAGER, SERVER_KEY_STORE, KEY_STORE_KEYPASS));
        applyUpdate(serverSideMCC, getAddKeyStoreOp(SERVER_TRUST_STORE, SERVER_TRUST_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(serverSideMCC, getAddTrustManagerOp(SERVER_TRUST_MANAGER, SERVER_TRUST_STORE));
        applyUpdate(serverSideMCC, getAddServerSSLContextOp(SERVER_SSL_CONTEXT, SERVER_KEY_MANAGER, SERVER_TRUST_MANAGER));
        applyUpdate(serverSideMCC,
                Operations.CompositeOperationBuilder.create()
                .addStep(Util.getUndefineAttributeOperation(getDefaultHttpsListenerAddress(), SECURITY_REALM))
                .addStep(Util.getWriteAttributeOperation(getDefaultHttpsListenerAddress(), SSL_CONTEXT, SERVER_SSL_CONTEXT))
                .build().getOperation()
        );
        applyUpdate(serverSideMCC, getAddHttpConnectorOp(CONNECTOR, "https", AUTHENTICATION_FACTORY));
        executeBlockingReloadServerSide(serverSideMCC);
    }

    private static ModelControllerClient getInboundConnectionServerMCC() {
        return TestSuiteEnvironment.getModelControllerClient();
    }

    private static ModelControllerClient getOutboundConnectionServerMCC() {
        try {
            return ModelControllerClient.Factory.create(
                    InetAddress.getByName(TestSuiteEnvironment.getServerAddressNode1()),
                    10090,
                    Authentication.getCallbackHandler());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static void applyUpdate(final ModelControllerClient client, ModelNode update, boolean allowReload) {
        if (allowReload) {
            update.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        }
        log.trace("Executing operation:\n" + update.toString());
        ModelNode result;
        try {
            result = client.execute(new OperationBuilder(update).build());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            log.trace("Operation result:\n" + result.toString());
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.toString());
        } else {
            throw new RuntimeException("Operation not successful, outcome:\n" + result.get("outcome"));
        }
    }

    private static String executeReadAttributeOpReturnResult(final ModelControllerClient client, PathAddress address,
                                                           String attributeName) {
        ModelNode op = Util.getReadAttributeOperation(address, attributeName);
        ModelNode result;
        try {
            result = client.execute(new OperationBuilder(op).build());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            log.trace("Operation result:\n" + result.toString());
            return result.get("result").asString();
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.toString());
        } else {
            throw new RuntimeException("Operation not successful, outcome:\n" + result.get("outcome"));
        }
    }

    private static void applyUpdate(final ModelControllerClient client, ModelNode update) {
        applyUpdate(client, update, false);
    }

    private static void executeBlockingReloadServerSide(final ModelControllerClient serverSideMCC) {
        String state;
        try {
            state = ServerReload.getContainerRunningState(serverSideMCC);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.trace("Executing reload on client side server with container state: [ " + state + " ]");
        ServerReload.executeReloadAndWaitForCompletion(serverSideMCC, ServerReload.TIMEOUT, false,
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort());
        try {
            state = ServerReload.getContainerRunningState(serverSideMCC);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.trace("Container state after reload on client side server: [ " + state + " ]");
    }

    private static void executeBlockingReloadClientServer(final ModelControllerClient clientSideMCC) {
        String state;
        try {
            state = ServerReload.getContainerRunningState(clientSideMCC);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.trace("Executing reload on client side server with container state: [ " + state + " ]");
        ServerReload.executeReloadAndWaitForCompletion(clientSideMCC, ServerReload.TIMEOUT, false,
                TestSuiteEnvironment.getServerAddressNode1(), 10090);
        try {
            state = ServerReload.getContainerRunningState(clientSideMCC);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.trace("Container state after reload on client side server: [ " + state + " ]");
    }

    private String callIntermediateWhoAmI() {
        AuthenticationConfiguration common = AuthenticationConfiguration.empty()
                .useProviders(() -> new Provider[] {new WildFlyElytronProvider()})
                .setSaslMechanismSelector(SaslMechanismSelector.ALL);
        AuthenticationContext authCtxEmpty = AuthenticationContext.empty();
        final AuthenticationContext authCtx = authCtxEmpty.with(MatchRule.ALL, common);

        final EJBClientContext.Builder ejbClientBuilder = new EJBClientContext.Builder();
        ejbClientBuilder.addTransportProvider(new RemoteTransportProvider());
        final EJBClientConnection.Builder connBuilder = new EJBClientConnection.Builder();
        connBuilder.setDestination(URI.create("remote+http://" + TestSuiteEnvironment.getServerAddressNode1() + ":8180"));
        ejbClientBuilder.addClientConnection(connBuilder.build());
        final EJBClientContext ejbCtx = ejbClientBuilder.build();

        AuthenticationContext.getContextManager().setThreadDefault(authCtx);
        EJBClientContext.getContextManager().setThreadDefault(ejbCtx);

        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());

        String result;

        try {
            InitialContext ctx = new InitialContext(props);
            String lookupName = "ejb:/outbound-module/IntermediateWhoAmI!org.jboss.as.test.manualmode.ejb.client.outbound.connection.security.WhoAmI";
            WhoAmI intermediate = (WhoAmI)ctx.lookup(lookupName);
            result = intermediate.whoAmI();
            ctx.close();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public void removeIfExists(ModelControllerClient client, PathAddress address, boolean allowResourceReload) {
        ModelNode rrResult;
        try {
            rrResult = client.execute(Util.createOperation(READ_RESOURCE_OPERATION, address));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (rrResult != null && Operations.isSuccessfulOutcome(rrResult)) {
            applyUpdate(client, Util.createRemoveOperation(address), allowResourceReload);
        }
    }

    public void removeIfExists(ModelControllerClient client, PathAddress address) {
        removeIfExists(client, address, false);
    }

    public static void cleanFile(File toClean) {
        if (toClean.exists()) {
            if (toClean.isDirectory()) {
                for (File child : toClean.listFiles()) {
                    cleanFile(child);
                }
            }
            toClean.delete();
        }
    }

}
