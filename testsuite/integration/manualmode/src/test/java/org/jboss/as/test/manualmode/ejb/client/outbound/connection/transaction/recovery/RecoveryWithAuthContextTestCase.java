/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.client.outbound.connection.transaction.recovery;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLAIN_TEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SASL_AUTHENTICATION_FACTORY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.shared.PermissionUtils.createFilePermission;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.File;
import java.util.Arrays;
import java.util.PropertyPermission;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.transactions.RecoveryExecutor;
import org.jboss.as.test.integration.transactions.RemoteLookups;
import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingletonRemote;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * when the system tries to recover the auth context is AuthenticationContext.captureCurrent()
 * this leads to a context that it is unusable to establish a connection.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RecoveryWithAuthContextTestCase {

    private static final Logger log = Logger.getLogger(RecoveryWithAuthContextTestCase.class);

    private static final String INBOUND_SERVER  = "inbound-server";
    private static final String OUTBOUND_SERVER = "outbound-server";
    private static final String CLIENT_DEPLOYMENT = "recovery-auth-context-client";
    private static final String SERVER_DEPLOYMENT = "recovery-auth-context-server";

    private static final String OUTBOUND_SERVER_HOME    = "outbound-server";
    private static final String WFTC_DATA_DIRECTORY     = "ejb-xa-recovery";

    // Elytron / remoting constants for the inbound server
    private static final String PROPERTIES_REALM   = "RecoveryAuthContextRealm";
    private static final String SECURITY_DOMAIN    = "RecoveryAuthContextDomain";
    private static final String SASL_FACTORY       = "RecoveryAuthContextSaslFactory";
    private static final String EJB_APP_DOMAIN     = "RecoveryAuthContextDomain";
    private static final String INBOUND_CONNECTOR  = "recovery-auth-context-connector";
    private static final String INBOUND_SOCKET     = "recovery-auth-context-socket";
    private static final int    REMOTING_PORT       = 19447;
    private static final String REMOTING_PROTOCOL  = "remote";

    // Elytron / remoting constants for the outbound server
    private static final String AUTH_CONFIG         = "RecoveryAuthContextConfig";
    private static final String AUTH_CONTEXT        = "RecoveryAuthContextCtx";
    private static final String OUTBOUND_SOCKET     = "recovery-auth-context-inbound-binding";
    private static final String OUTBOUND_CONNECTION = "inbound-server-connection";

    private static final String USERNAME = "ejbuser";
    private static final String PASSWORD = "ejbpassword";

    private static final String USERS_PATH = new File(
            RecoveryWithAuthContextTestCase.class.getResource("users.properties").getFile()).getAbsolutePath();
    private static final String ROLES_PATH = new File(
            RecoveryWithAuthContextTestCase.class.getResource("roles.properties").getFile()).getAbsolutePath();

    @ArquillianResource
    private static ContainerController containerController;

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    @TargetsContainer(INBOUND_SERVER)
    private ManagementClient inboundClient;

    @ArquillianResource
    @TargetsContainer(OUTBOUND_SERVER)
    private ManagementClient outboundClient;

    private AutoCloseable inboundSnapshot;
    private AutoCloseable outboundSnapshot;

    @Deployment(name = SERVER_DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(INBOUND_SERVER)
    public static Archive<?> serverDeployment() {
        return ShrinkWrap.create(JavaArchive.class, SERVER_DEPLOYMENT + ".jar")
                .addClasses(TransactionalBean.class, TransactionalBeanRemote.class)
                .addPackages(true, TestXAResource.class.getPackage())
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission("jboss.server.data.dir", "read"),
                        createFilePermission("read,write", "basedir",
                                Arrays.asList("target", "inbound-server", "standalone", "data")),
                        createFilePermission("read,write", "basedir",
                                Arrays.asList("target", "inbound-server", "standalone", "data", "PersistentTestXAResource"))
                ), "permissions.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.jts\n"), "MANIFEST.MF");
    }

    @Deployment(name = CLIENT_DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(OUTBOUND_SERVER)
    public static Archive<?> clientDeployment() {
        return ShrinkWrap.create(JavaArchive.class, CLIENT_DEPLOYMENT + ".jar")
                .addClasses(ClientBean.class, ClientBeanRemote.class, TransactionalBeanRemote.class)
                .addPackage(TestXAResource.class.getPackage())
                .addAsManifestResource(RecoveryWithAuthContextTestCase.class.getPackage(),
                        "jboss-ejb-client.xml", "jboss-ejb-client.xml")
                .addAsManifestResource(createPermissionsXmlAsset(
                        new RuntimePermission("exitVM", "none"),
                        createFilePermission("read,write", "basedir",
                                Arrays.asList("target", OUTBOUND_SERVER_HOME, "standalone", "data", WFTC_DATA_DIRECTORY)),
                        createFilePermission("read,write", "basedir",
                                Arrays.asList("target", OUTBOUND_SERVER_HOME, "standalone", "data", WFTC_DATA_DIRECTORY, "-"))
                ), "permissions.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.jts\n"), "MANIFEST.MF");
    }

    @Before
    public void setUp() throws Exception {
        if (!containerController.isStarted(INBOUND_SERVER)) {
            containerController.start(INBOUND_SERVER);
        }
        if (!containerController.isStarted(OUTBOUND_SERVER)) {
            containerController.start(OUTBOUND_SERVER);
        }

        inboundSnapshot  = ServerSnapshot.takeSnapshot(inboundClient);
        outboundSnapshot = ServerSnapshot.takeSnapshot(outboundClient);

        configureInboundServer(inboundClient.getControllerClient());
        configureOutboundServer(outboundClient.getControllerClient());

        deployer.deploy(SERVER_DEPLOYMENT);
        deployer.deploy(CLIENT_DEPLOYMENT);

        RemoteLookups.lookupEjbStateless(inboundClient, SERVER_DEPLOYMENT,
                TransactionCheckerSingleton.class, TransactionCheckerSingletonRemote.class).resetAll();
    }

    @After
    public void tearDown() throws Exception {
        try {
            deployer.undeploy(SERVER_DEPLOYMENT);
        } catch (Exception ignored) {
        }
        try {
            deployer.undeploy(CLIENT_DEPLOYMENT);
        } catch (Exception ignored) {
            // outbound server may have crashed; deployment is no longer present
        }
        try {
            if (inboundSnapshot != null) inboundSnapshot.close();
        } finally {
            if (outboundSnapshot != null) outboundSnapshot.close();
        }
    }

    @AfterClass
    public static void stopContainers() {
        if (containerController.isStarted(INBOUND_SERVER)) {
            containerController.stop(INBOUND_SERVER);
        }
        if (containerController.isStarted(OUTBOUND_SERVER)) {
            containerController.stop(OUTBOUND_SERVER);
        }
    }

    // client bean (outbound) remote calls transaction bean (in inbound)
    // kill outbound (client bean... the one calling the transaction bean)
    // check the transaction in the inbound.
    // run the recovery in the outbound (auth should work)
    @Test
    public void testRecoveryFailsWithoutAuthContext() throws Exception {
        try {
            ClientBeanRemote clientBean = RemoteLookups.lookupEjbStateless(
                    outboundClient, CLIENT_DEPLOYMENT, ClientBean.class, ClientBeanRemote.class);
            clientBean.beginTransactionAndCrash(SERVER_DEPLOYMENT);
            Assert.fail("Expected the outbound server JVM to crash during 2PC prepare");
        } catch (Throwable expected) {
            log.debugf(expected, "Exception expected — outbound server JVM crashed during prepare");
        }

        // Synchronize Arquillian state after the crash
        try {
            containerController.kill(OUTBOUND_SERVER);
        } catch (Exception ignore) {
            log.debug("Arquillian kill of " + OUTBOUND_SERVER + " failed; process is already down", ignore);
        }

        containerController.start(OUTBOUND_SERVER);

        TransactionCheckerSingletonRemote serverChecker = RemoteLookups.lookupEjbStateless(
                inboundClient, SERVER_DEPLOYMENT,
                TransactionCheckerSingleton.class, TransactionCheckerSingletonRemote.class);
        Assert.assertEquals("Expected no rollback before recovery runs", 0, serverChecker.getRolledback());

        // we enforce recovery transaction.
        // from the recovery executor javadoc:
        // Returning from this method could not necessary means that whole recovery cycle was fully finished.
        // To be sure that the whole recovery cycle is finished is recommended to run this method twice (one by one).
        // After the second call it's ensured that one(!) recovery cycle is fully finished.
        RecoveryExecutor recoveryExecutor = new RecoveryExecutor(outboundClient);
        recoveryExecutor.runTransactionRecovery();
        recoveryExecutor.runTransactionRecovery();

        Assert.assertEquals(
                "should have run once",
                1, serverChecker.getRolledback());
        assertEmptyOutboundWftcDataDirectory();
    }

    // -------------------------------------------------------------------------
    // Server configuration helpers
    // -------------------------------------------------------------------------

    private void configureInboundServer(ModelControllerClient mcc) throws Exception {
        applyUpdate(mcc, addPropertiesRealmOp(PROPERTIES_REALM, ROLES_PATH, USERS_PATH));
        applyUpdate(mcc, addSecurityDomainOp(SECURITY_DOMAIN, PROPERTIES_REALM));
        applyUpdate(mcc, addSaslAuthenticationFactoryOp(SASL_FACTORY, SECURITY_DOMAIN, PROPERTIES_REALM));
        applyUpdate(mcc, addEjbApplicationSecurityDomainOp(EJB_APP_DOMAIN, SECURITY_DOMAIN));
        applyUpdate(mcc, addSocketBindingOp(INBOUND_SOCKET, REMOTING_PORT));
        applyUpdate(mcc, addConnectorOp(INBOUND_CONNECTOR, INBOUND_SOCKET, SASL_FACTORY));
        ServerReload.executeReloadAndWaitForCompletion(inboundClient);
    }

    private void configureOutboundServer(ModelControllerClient mcc) throws Exception {
        applyUpdate(mcc, addOutboundSocketBindingOp(OUTBOUND_SOCKET, TestSuiteEnvironment.getServerAddress(), REMOTING_PORT));
        applyUpdate(mcc, addAuthenticationConfigurationOp(AUTH_CONFIG, REMOTING_PROTOCOL, PROPERTIES_REALM, USERNAME, PASSWORD));
        applyUpdate(mcc, addAuthenticationContextOp(AUTH_CONTEXT, AUTH_CONFIG));
        applyUpdate(mcc, addOutboundConnectionOp(OUTBOUND_CONNECTION, OUTBOUND_SOCKET, AUTH_CONTEXT));
        applyUpdate(mcc, enableRecoveryListenerOp());
        applyUpdate(mcc, setRecoveryAuthContextOp(AUTH_CONTEXT));
        ServerReload.executeReloadAndWaitForCompletion(outboundClient);
    }

    private static ModelNode setRecoveryAuthContextOp(String authContextName) {
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).setEmptyList().add(SUBSYSTEM, "transactions");
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set("recovery-authentication-context");
        op.get(VALUE).set(authContextName);
        return op;
    }

    // -------------------------------------------------------------------------
    // Management operation builders
    // -------------------------------------------------------------------------

    private static ModelNode addPropertiesRealmOp(String realmName, String groupsPath, String usersPath) {
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM, "elytron")
                .append("properties-realm", realmName));
        op.get("groups-properties", PATH).set(groupsPath);
        op.get("users-properties", PATH).set(usersPath);
        op.get("users-properties", PLAIN_TEXT).set(true);
        return op;
    }

    private static ModelNode addSecurityDomainOp(String domainName, String realmName) {
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM, "elytron")
                .append("security-domain", domainName));
        ModelNode realm = new ModelNode();
        realm.get(REALM).set(realmName);
        realm.get("role-decoder").set("groups-to-roles");
        op.get("realms").setEmptyList().add(realm);
        op.get("default-realm").set(realmName);
        op.get("permission-mapper").set("default-permission-mapper");
        return op;
    }

    private static ModelNode addSaslAuthenticationFactoryOp(String factoryName, String domainName, String realmName) {
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM, "elytron")
                .append("sasl-authentication-factory", factoryName));
        op.get("sasl-server-factory").set("configured");
        op.get("security-domain").set(domainName);
        ModelNode realmConfig = new ModelNode();
        realmConfig.get("realm-name").set(realmName);
        ModelNode mechanism = new ModelNode();
        mechanism.get("mechanism-name").set("DIGEST-MD5");
        mechanism.get("mechanism-realm-configurations").setEmptyList().add(realmConfig);
        op.get("mechanism-configurations").setEmptyList().add(mechanism);
        return op;
    }

    private static ModelNode addEjbApplicationSecurityDomainOp(String ejbDomainName, String securityDomainName) {
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM, "ejb3")
                .append("application-security-domain", ejbDomainName));
        op.get("security-domain").set(securityDomainName);
        return op;
    }

    private static ModelNode addSocketBindingOp(String name, int port) {
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SOCKET_BINDING_GROUP, "standard-sockets")
                .append(SOCKET_BINDING, name));
        op.get(PORT).set(port);
        return op;
    }

    private static ModelNode addConnectorOp(String connectorName, String socketName, String factoryName) {
        ModelNode addConnector = Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM, "remoting")
                .append("connector", connectorName));
        addConnector.get(SOCKET_BINDING).set(socketName);
        addConnector.get(SASL_AUTHENTICATION_FACTORY).set(factoryName);
        addConnector.get(PROTOCOL).set(REMOTING_PROTOCOL);

        ModelNode addToEjb = Util.createOperation("list-add", PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, "ejb3"),
                PathElement.pathElement(SERVICE, ModelDescriptionConstants.REMOTE)));
        addToEjb.get(NAME).set("connectors");
        addToEjb.get(VALUE).set(connectorName);

        return Operations.CompositeOperationBuilder.create()
                .addStep(addConnector)
                .addStep(addToEjb)
                .build().getOperation();
    }

    private static ModelNode addOutboundSocketBindingOp(String name, String host, int port) {
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SOCKET_BINDING_GROUP, "standard-sockets")
                .append(REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING, name));
        op.get(HOST).set(host);
        op.get(PORT).set(port);
        return op;
    }

    private static ModelNode addAuthenticationConfigurationOp(String name, String protocol, String realm,
                                                              String username, String password) {
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM, "elytron")
                .append("authentication-configuration", name));
        op.get("protocol").set(protocol);
        op.get("authentication-name").set(username);
        op.get("sasl-mechanism-selector").set("DIGEST-MD5");
        op.get(REALM).set(realm);
        ModelNode cred = new ModelNode();
        cred.get("clear-text").set(password);
        op.get("credential-reference").set(cred);
        return op;
    }

    private static ModelNode addAuthenticationContextOp(String contextName, String configName) {
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM, "elytron")
                .append("authentication-context", contextName));
        ModelNode rule = new ModelNode();
        rule.get("authentication-configuration").set(configName);
        op.get("match-rules").setEmptyList().add(rule);
        return op;
    }

    private static ModelNode addOutboundConnectionOp(String name, String socketBinding, String authContext) {
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM, "remoting")
                .append("remote-outbound-connection", name));
        op.get("outbound-socket-binding-ref").set(socketBinding);
        op.get("authentication-context").set(authContext);
        return op;
    }

    private static ModelNode enableRecoveryListenerOp() {
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).setEmptyList().add(SUBSYSTEM, "transactions");
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set("recovery-listener");
        op.get(VALUE).set(true);
        return op;
    }

    private static void applyUpdate(ModelControllerClient client, ModelNode update) throws Exception {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Management operation failed: " + Operations.getFailureDescription(result));
        }
    }

    // -------------------------------------------------------------------------
    // Assertions
    // -------------------------------------------------------------------------

    private void assertEmptyOutboundWftcDataDirectory() {
        String basedir = System.getProperty("basedir");
        File wftcDir = new File(basedir, "target/" + OUTBOUND_SERVER_HOME + "/standalone/data/" + WFTC_DATA_DIRECTORY);
        if (wftcDir.exists()) {
            File[] files = wftcDir.listFiles();
            Assert.assertTrue(
                    "WFTC data directory should be empty after successful recovery but contains: "
                            + Arrays.toString(files),
                    files == null || files.length == 0);
        }
    }
}
