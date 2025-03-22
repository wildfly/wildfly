/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.ssl;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.manualmode.ejb.client.outbound.connection.security.ElytronRemoteOutboundConnectionTestCase;
import org.jboss.as.test.manualmode.ejb.ssl.beans.StatelessBean;
import org.jboss.as.test.manualmode.ejb.ssl.beans.StatelessBeanRemote;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLAIN_TEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SASL_AUTHENTICATION_FACTORY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL_CONTEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SSLEJBRemoteRemotingClientTestCase {

    private static final Logger log = Logger.getLogger(SSLEJBRemoteRemotingClientTestCase.class);
    private static final String MODULE_NAME = "ssl-remote-remoting-ejb-client-test";
    private static boolean serverConfigDone = false;

    private static final String RESOURCE_PREFIX = "ejb-remote-tests";

    private static final String INBOUND_SOCKET_BINDING = RESOURCE_PREFIX + "-socket-binding";
    private static final String INBOUND_SOCKET_BINDING_TLS = RESOURCE_PREFIX + "-socket-binding-tls";

    private static final int REMOTING_PORT = 4446;
    private static final int TLS_REMOTING_PORT = 4447;

    private static final String PROPERTIES_REALM = RESOURCE_PREFIX + "-properties-realm";
    private static final String SECURITY_DOMAIN = RESOURCE_PREFIX + "-security-domain";
    private static final String AUTHENTICATION_FACTORY = RESOURCE_PREFIX + "-sasl-authentication";
    private static final String APPLICATION_SECURITY_DOMAIN = RESOURCE_PREFIX;

    private static final String CONNECTOR = RESOURCE_PREFIX + "-connector";
    private static final String CONNECTOR_TLS = RESOURCE_PREFIX + "-connector+tls";
    private static final String SERVER_KEY_STORE = RESOURCE_PREFIX + "-server-key-store";
    private static final String SERVER_KEY_MANAGER = RESOURCE_PREFIX + "-server-key-manager";
    private static final String SERVER_TRUST_STORE = RESOURCE_PREFIX + "-server-trust-store";
    private static final String SERVER_TRUST_MANAGER = RESOURCE_PREFIX + "-server-trust-manager";
    private static final String SERVER_SSL_CONTEXT = RESOURCE_PREFIX + "-server-ssl-context";

    private static final String KEY_STORE_KEYPASS = "clientPassword";

    private static final String REMOTE = "remote";
    private static final String REMOTE_TLS = "remote+tls";

    private static final File CERTS_DIR = new File(new File("").getAbsoluteFile().getAbsolutePath()
            + File.separatorChar + "target" + File.separatorChar + "test-classes" + File.separatorChar
            + "ejb3" + File.separatorChar + "ssl");

    private static final String SERVER_KEY_STORE_PATH = new File(CERTS_DIR.getAbsoluteFile(), "jbossClient.keystore")
            .getAbsolutePath();
    private static final String SERVER_TRUST_STORE_PATH = new File(CERTS_DIR.getAbsoluteFile(), "jbossClient.truststore")
            .getAbsolutePath();

    private static final String USERS_PATH = new File(ElytronRemoteOutboundConnectionTestCase.class.getResource("users.properties").
            getFile()).getAbsolutePath();
    private static final String ROLES_PATH = new File(ElytronRemoteOutboundConnectionTestCase.class.getResource("roles.properties").
            getFile()).getAbsolutePath();

    @ArquillianResource
    private static ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    public static final String DEFAULT_JBOSSAS = "default-jbossas";

    @Deployment(name = MODULE_NAME, managed = false, testable = false)
    @TargetsContainer(DEFAULT_JBOSSAS)
    public static Archive<?> deployStateless() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addClasses(StatelessBeanRemote.class, StatelessBean.class);
        return jar;
    }

    @BeforeClass
    public static void prepare() {
        log.trace("*** javax.net.ssl.trustStore=" + System.getProperty("javax.net.ssl.trustStore"));
        log.trace("*** javax.net.ssl.trustStorePassword=" + System.getProperty("javax.net.ssl.trustStorePassword"));
        log.trace("*** javax.net.ssl.keyStore=" + System.getProperty("javax.net.ssl.keyStore"));
        log.trace("*** javax.net.ssl.keyStorePassword=" + System.getProperty("javax.net.ssl.keyStorePassword"));
        System.setProperty("jboss.ejb.client.properties.skip.classloader.scan", "true");
    }

    @Before
    public void prepareServerOnce() {
        if (!serverConfigDone) {
            // prepare server config and then restart
            log.trace("*** preparing server configuration");
            log.trace("*** starting server");
            container.start(DEFAULT_JBOSSAS);
            final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
            log.trace("*** will configure server now");
            configureServerSideForInboundSSLRemoting(client);
            log.trace("*** restarting server");
            container.stop(DEFAULT_JBOSSAS);
            container.start(DEFAULT_JBOSSAS);
            serverConfigDone = true;
        } else {
            log.trace("*** Server already prepared, skipping config procedure");
        }
    }

    private static void configureServerSideForInboundSSLRemoting(ModelControllerClient serverSideMCC) {
        applyUpdate(serverSideMCC, getAddPropertiesRealmOp(PROPERTIES_REALM, ROLES_PATH, USERS_PATH, true));
        applyUpdate(serverSideMCC, getAddElytronSecurityDomainOp(SECURITY_DOMAIN, PROPERTIES_REALM));
        applyUpdate(serverSideMCC, getAddSaslAuthenticationFactoryOp(AUTHENTICATION_FACTORY, SECURITY_DOMAIN));
        applyUpdate(serverSideMCC, getAddEjbApplicationSecurityDomainOp(APPLICATION_SECURITY_DOMAIN, SECURITY_DOMAIN));
        applyUpdate(serverSideMCC, getAddSocketBindingOp(INBOUND_SOCKET_BINDING, REMOTING_PORT));
        applyUpdate(serverSideMCC, getAddSocketBindingOp(INBOUND_SOCKET_BINDING_TLS, TLS_REMOTING_PORT));
        applyUpdate(serverSideMCC, getAddKeyStoreOp(SERVER_KEY_STORE, SERVER_KEY_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(serverSideMCC, getAddKeyManagerOp(SERVER_KEY_MANAGER, SERVER_KEY_STORE, KEY_STORE_KEYPASS));
        applyUpdate(serverSideMCC, getAddKeyStoreOp(SERVER_TRUST_STORE, SERVER_TRUST_STORE_PATH, KEY_STORE_KEYPASS));
        applyUpdate(serverSideMCC, getAddTrustManagerOp(SERVER_TRUST_MANAGER, SERVER_TRUST_STORE));
        applyUpdate(serverSideMCC, getAddServerSSLContextOp(SERVER_SSL_CONTEXT, SERVER_KEY_MANAGER, SERVER_TRUST_MANAGER));
        applyUpdate(serverSideMCC, getAddConnectorOp(CONNECTOR_TLS, INBOUND_SOCKET_BINDING_TLS, AUTHENTICATION_FACTORY, SERVER_SSL_CONTEXT, REMOTE_TLS));
        applyUpdate(serverSideMCC, getAddConnectorOp(CONNECTOR, INBOUND_SOCKET_BINDING, AUTHENTICATION_FACTORY, SERVER_SSL_CONTEXT, REMOTE));
        executeBlockingReloadServerSide(serverSideMCC);
    }


    private static void applyUpdate(final ModelControllerClient client, ModelNode update) {
        applyUpdate(client, update, false);
    }

    private static void applyUpdate(final ModelControllerClient client, ModelNode update, boolean allowReload) {
        if (allowReload) {
            update.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        }
        System.out.println("Executing operation:\n" + update.toString());
        ModelNode result;
        try {
            result = client.execute(new OperationBuilder(update).build());
            System.out.println(result);
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
        ModelNode op = Util.createAddOperation(getElytronSecurityDomainAddress(domainName));
        ModelNode realm = new ModelNode();
        realm.get(REALM).set(realmName);
        realm.get("role-decoder").set("groups-to-roles");
        op.get("realms").setEmptyList().add(realm);
        op.get("default-realm").set(realmName);
        op.get("permission-mapper").set("default-permission-mapper");
        return op;
    }

    private static PathAddress getSaslAuthenticationFactoryAddress(String factoryName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "elytron")
                .append("sasl-authentication-factory", factoryName);
    }

    private static ModelNode getAddSaslAuthenticationFactoryOp(String factoryName, String securityDomainName) {
        ModelNode op = Util.createAddOperation(getSaslAuthenticationFactoryAddress(factoryName));
        op.get("sasl-server-factory").set("configured");
        op.get("security-domain").set(securityDomainName);
        ModelNode realmConfig = new ModelNode();
        realmConfig.get("realm-name").set("local");
        ModelNode digestMechanism = new ModelNode();
        digestMechanism.get("mechanism-name").set("DIGEST-MD5");
        digestMechanism.get("mechanism-realm-configurations").setEmptyList().add(realmConfig);
        op.get("mechanism-configurations").setEmptyList().add(digestMechanism);
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

    @AfterClass
    public static void tearDown() throws Exception {
        System.setProperty("jboss.ejb.client.properties.skip.classloader.scan", "false");
    }

    private Properties getEjbClientContextProperties(String protocol, int port) throws IOException {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY,
                "org.wildfly.naming.client.WildFlyInitialContextFactory");
        props.put(Context.PROVIDER_URL, String.format("%s://%s:%d", protocol, TestSuiteEnvironment.getServerAddressNode1(), port));
        props.put(Context.SECURITY_PRINCIPAL, "ejbRemoteTests");
        props.put(Context.SECURITY_CREDENTIALS, "ejbRemoteTestsPassword");
        return props;
    }

    private static PathAddress getConnectorAddress(String connectorName) {
        return PathAddress.pathAddress()
                .append(SUBSYSTEM, "remoting")
                .append("connector", connectorName);
    }

    private static ModelNode getEjbConnectorOp(String operationName, String connectorName) {
        ModelNode operation = Util.createOperation(operationName, PathAddress.pathAddress(
                PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "ejb3"),
                PathElement.pathElement(ModelDescriptionConstants.SERVICE, ModelDescriptionConstants.REMOTE)
        ));
        operation.get(ModelDescriptionConstants.NAME).set("connectors");
        operation.get(ModelDescriptionConstants.VALUE).set(connectorName);
        return operation;
    }

    private static ModelNode getAddConnectorOp(String connectorName, String socketBindingName, String factoryName,
                                               String serverSSLContextName, String protocol) {
        ModelNode addConnectorOp = Util.createAddOperation(getConnectorAddress(connectorName));
        addConnectorOp.get(SOCKET_BINDING).set(socketBindingName);
        addConnectorOp.get(SASL_AUTHENTICATION_FACTORY).set(factoryName);
        if (serverSSLContextName != null && !serverSSLContextName.isEmpty()) {
            addConnectorOp.get(SSL_CONTEXT).set(serverSSLContextName);
        }
        addConnectorOp.get(PROTOCOL).set(protocol);
        return Operations.CompositeOperationBuilder.create()
                .addStep(addConnectorOp)
                .addStep(getEjbConnectorOp("list-add", connectorName))
                .build().getOperation();
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

    @Test
    public void testRemote() throws Exception {
        log.trace("**** deploying deployment with stateless beans");
        deployer.deploy(MODULE_NAME);
        log.trace("**** creating InitialContext");
        InitialContext ctx = new InitialContext(getEjbClientContextProperties("remote", 4446));
        try {
            log.trace("**** looking up StatelessBean through JNDI");
            StatelessBeanRemote bean = (StatelessBeanRemote)
                    ctx.lookup("ejb:/" + MODULE_NAME + "/" + StatelessBean.class.getSimpleName() + "!" + StatelessBeanRemote.class.getCanonicalName());
            log.trace("**** About to perform synchronous call on stateless bean");
            String response = bean.sayHello();
            log.trace("**** The answer is: " + response);
            Assert.assertEquals("Remote invocation of Jakarta Enterprise Beans were not successful", StatelessBeanRemote.ANSWER, response);
            deployer.undeploy(MODULE_NAME);
        } finally {
            ctx.close();
        }
    }

    @Test
    public void testRemoteTls() throws Exception {
        log.trace("**** deploying deployment with stateless beans");
        deployer.deploy(MODULE_NAME);
        log.trace("**** creating InitialContext");
        InitialContext ctx = new InitialContext(getEjbClientContextProperties("remote+tls", 4447));
        try {
            log.trace("**** looking up StatelessBean through JNDI");
            StatelessBeanRemote bean = (StatelessBeanRemote)
                    ctx.lookup("ejb:/" + MODULE_NAME + "/" + StatelessBean.class.getSimpleName() + "!" + StatelessBeanRemote.class.getCanonicalName());
            log.trace("**** About to perform synchronous call on stateless bean");
            String response = bean.sayHello();
            log.trace("**** The answer is: " + response);
            Assert.assertEquals("Remote invocation of Jakarta Enterprise Beans were not successful", StatelessBeanRemote.ANSWER, response);
            deployer.undeploy(MODULE_NAME);
        } finally {
            ctx.close();
        }
    }
}
