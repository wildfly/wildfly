/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.loginmodules;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.ejb.EJBAccessException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSecurityRealmsServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.config.realm.Authentication;
import org.jboss.as.test.integration.security.common.config.realm.RealmKeystore;
import org.jboss.as.test.integration.security.common.config.realm.SecurityRealm;
import org.jboss.as.test.integration.security.common.config.realm.ServerIdentity;
import org.jboss.as.test.integration.security.common.ejb3.Hello;
import org.jboss.as.test.integration.security.common.ejb3.HelloBean;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketbox.util.KeyStoreUtil;

/**
 * A testcase for {@link org.jboss.as.security.remoting.RemotingLoginModule}. This test covers scenario, when an EJB clients use
 * certificate for authentication.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({RemotingLoginModuleTestCase.FilesSetup.class, //
        RemotingLoginModuleTestCase.SecurityRealmsSetup.class, //
        RemotingLoginModuleTestCase.RemotingSetup.class, //
        RemotingLoginModuleTestCase.SecurityDomainsSetup.class //
})
@RunAsClient
public class RemotingLoginModuleTestCase {
    private static Logger LOGGER = Logger.getLogger(RemotingLoginModuleTestCase.class);

    private static final String TEST_NAME = "remoting-lm-test";

    /**
     * The LOOKUP_NAME
     */
    private static final String HELLOBEAN_LOOKUP_NAME = "/" + TEST_NAME + "/" + HelloBean.class.getSimpleName() + "!"
            + Hello.class.getName();

    private static final String KEYSTORE_PASSWORD = "123456";
    private static final String SERVER_NAME = "server";
    private static final String CLIENT_AUTHORIZED_NAME = "client";
    private static final String CLIENT_NOT_AUTHORIZED_NAME = "clientNotAuthorized";
    private static final String CLIENT_NOT_TRUSTED_NAME = "clientNotTrusted";
    private static final String KEYSTORE_SUFFIX = ".keystore";

    private static final File WORK_DIR = new File("workdir-" + TEST_NAME);
    private static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, "server.keystore");
    private static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, "server.truststore");
    private static final File CLIENTS_TRUSTSTORE_FILE = new File(WORK_DIR, "clients.truststore");
    private static final File USERS_FILE = new File(WORK_DIR, "users.properties");
    private static final File ROLES_FILE = new File(WORK_DIR, "roles.properties");

    private static final int REMOTING_PORT_TEST = 14447;

    private static final PathAddress ADDR_SOCKET_BINDING = PathAddress.pathAddress()
            .append(SOCKET_BINDING_GROUP, "standard-sockets").append(SOCKET_BINDING, TEST_NAME);
    private static final PathAddress ADDR_REMOTING_CONNECTOR = PathAddress.pathAddress().append(SUBSYSTEM, "remoting")
            .append("connector", TEST_NAME);

    @ArquillianResource
    private ManagementClient mgmtClient;

    // Public methods --------------------------------------------------------

    /**
     * Creates a deployment application for this test.
     *
     * @return
     * @throws IOException
     */
    @Deployment
    public static JavaArchive createDeployment() throws IOException {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, TEST_NAME + ".jar");
        jar.addClasses(HelloBean.class, Hello.class);
        jar.addAsManifestResource(Utils.getJBossEjb3XmlAsset(TEST_NAME), "jboss-ejb3.xml");
        return jar;
    }

    /**
     * Tests that an authorized user has access to an EJB method.
     *
     * @throws Exception
     */
    @Test
    public void testAuthorizedClient() throws Exception {
        final Properties env = configureEjbClient(CLIENT_AUTHORIZED_NAME);
        InitialContext ctx = new InitialContext(env);
        final Hello helloBean = (Hello) ctx.lookup(HELLOBEAN_LOOKUP_NAME);
        assertEquals(HelloBean.HELLO_WORLD, helloBean.sayHelloWorld());
        ctx.close();
    }

    /**
     * Tests if role check is done correctly for authenticated user.
     *
     * @throws Exception
     */
    @Test
    public void testNotAuthorizedClient() throws Exception {
        final Properties env = configureEjbClient(CLIENT_NOT_AUTHORIZED_NAME);
        InitialContext ctx = new InitialContext(env);
        final Hello helloBean = (Hello) ctx.lookup(HELLOBEAN_LOOKUP_NAME);
        try {
            helloBean.sayHelloWorld();
            fail("The EJB call should fail for unauthorized client.");
        } catch (EJBAccessException e) {
            //OK
        }
        ctx.close();
    }

    /**
     * Tests if client access is denied for untrusted clients.
     *
     * @throws Exception
     */
    @Test
    public void testNotTrustedClient() throws Exception {
        final Properties env = configureEjbClient(CLIENT_NOT_TRUSTED_NAME);
        InitialContext ctx = new InitialContext(env);
        try {
            ctx.lookup(HELLOBEAN_LOOKUP_NAME);
            fail("The JNDI lookup should fail for untrusted client.");
        } catch (NamingException e) {
            //OK
        }
        ctx.close();
    }

    // Private methods -------------------------------------------------------

    /**
     * Configure {@link SSLContext} and create EJB client properties.
     *
     * @param clientName
     * @return
     * @throws Exception
     */
    private Properties configureEjbClient(String clientName) throws Exception {
        // create new SSLContext based on client keystore and truststore and use this SSLContext instance as a default for this test
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(KeyStoreUtil.getKeyStore(getClientKeystoreFile(clientName), KEYSTORE_PASSWORD.toCharArray()),
                KEYSTORE_PASSWORD.toCharArray());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(KeyStoreUtil.getKeyStore(CLIENTS_TRUSTSTORE_FILE, KEYSTORE_PASSWORD.toCharArray()));

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        SSLContext.setDefault(sslContext);

        final Properties env = new Properties();
        env.put("java.naming.factory.initial", "org.jboss.naming.remote.client.InitialContextFactory");
        env.put("java.naming.provider.url", "remote://" + mgmtClient.getMgmtAddress() + ":" + REMOTING_PORT_TEST);
        env.put("jboss.naming.client.ejb.context", "true");
        env.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
        env.put(Context.SECURITY_PRINCIPAL, "admin");
        env.put(Context.SECURITY_CREDENTIALS, "testing");

        // SSL related config parameters
        env.put("jboss.naming.client.remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "true");
        env.put("jboss.naming.client.connect.options.org.xnio.Options.SSL_STARTTLS", "true");
        return env;
    }

    /**
     * Returns {@link File} instance representing keystore of client with given name.
     *
     * @param clientName
     * @return
     */
    private static File getClientKeystoreFile(String clientName) {
        return new File(WORK_DIR, clientName + KEYSTORE_SUFFIX);
    }

    // Embedded classes ------------------------------------------------------

    /**
     * A {@link ServerSetupTask} instance which creates security domains for this test case.
     *
     * @author Josef Cacek
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        /**
         * Returns SecurityDomains configuration for this testcase.
         *
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {
            //<security-domain name="xxx" cache-type="default">
            //    <authentication>
            //        <login-module code="Remoting" flag="optional">
            //            <module-option name="password-stacking" value="useFirstPass"/>
            //        </login-module>
            //        <login-module code="RealmUsersRoles" flag="required">
            //            <module-option name="password-stacking" value="useFirstPass"/>
            //            <module-option name="usersProperties" value="file:///${jboss.server.config.dir}/users.properties"/>
            //            <module-option name="rolesProperties" value="file:///${jboss.server.config.dir}/roles.properties"/>
            //        </login-module>
            //    </authentication>
            //</security-domain>
            final SecurityModule.Builder loginModuleBuilder = new SecurityModule.Builder().putOption("password-stacking",
                    "useFirstPass");
            final SecurityDomain sd = new SecurityDomain.Builder()
                    .name(TEST_NAME)
                    .loginModules(
                            loginModuleBuilder.name("Remoting").flag("optional").build(), //
                            loginModuleBuilder.name("RealmUsersRoles").flag("required")
                                    .putOption("usersProperties", USERS_FILE.getAbsolutePath())
                                    .putOption("rolesProperties", ROLES_FILE.getAbsolutePath()).build()) //
                    .build();
            return new SecurityDomain[]{sd};
        }
    }

    /**
     * A {@link ServerSetupTask} instance which creates security realms for this test case.
     *
     * @author Josef Cacek
     */
    static class SecurityRealmsSetup extends AbstractSecurityRealmsServerSetupTask {

        /**
         * Returns SecurityRealms configuration for this testcase.
         */
        @Override
        protected SecurityRealm[] getSecurityRealms() {
            // <server-identities>
            //    <ssl>
            //         <keystore path="server.keystore" keystore-password="123456"/>
            //     </ssl>
            // </server-identities>
            // <authentication>
            //    <truststore path="server.truststore" keystore-password="123456"/>
            // </authentication>
            RealmKeystore.Builder keyStoreBuilder = new RealmKeystore.Builder().keystorePassword(KEYSTORE_PASSWORD);
            final SecurityRealm realm = new SecurityRealm.Builder()
                    .name(TEST_NAME)
                    .serverIdentity(
                            new ServerIdentity.Builder().ssl(
                                    keyStoreBuilder.keystorePath(SERVER_KEYSTORE_FILE.getAbsolutePath()).build()).build())
                    .authentication(
                            new Authentication.Builder().truststore(
                                    keyStoreBuilder.keystorePath(SERVER_TRUSTSTORE_FILE.getAbsolutePath()).build()).build())
                    .build();
            return new SecurityRealm[]{realm};
        }
    }

    /**
     * A {@link ServerSetupTask} instance which creates remoting mappings for this test case.
     */
    static class RemotingSetup implements ServerSetupTask {

        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            final List<ModelNode> updates = new LinkedList<ModelNode>();
            LOGGER.trace("Adding new socket binding and remoting connector");
            // /socket-binding-group=standard-sockets/socket-binding=remoting-xxx:add(port=14447)
            ModelNode socketBindingModelNode = Util.createAddOperation(ADDR_SOCKET_BINDING);
            socketBindingModelNode.get(PORT).set(REMOTING_PORT_TEST);
            socketBindingModelNode.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(socketBindingModelNode);

            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode steps = compositeOp.get(STEPS);

            // /subsystem=remoting/connector=remoting-xx:add(security-realm=xx, socket-binding=yy)
            final ModelNode remotingConnectorModelNode = Util.createAddOperation(ADDR_REMOTING_CONNECTOR);
            remotingConnectorModelNode.get("security-realm").set(TEST_NAME);
            remotingConnectorModelNode.get("socket-binding").set(TEST_NAME);
            remotingConnectorModelNode.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            steps.add(remotingConnectorModelNode);

            updates.add(compositeOp);
            Utils.applyUpdates(updates, managementClient.getControllerClient());

        }

        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            final List<ModelNode> updates = new ArrayList<ModelNode>();

            // /subsystem=remoting/connector=remoting-xx:remove()
            ModelNode op = Util.createRemoveOperation(ADDR_REMOTING_CONNECTOR);
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            // /socket-binding-group=standard-sockets/socket-binding=remoting-xxx:remove()
            op = Util.createRemoveOperation(ADDR_SOCKET_BINDING);
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            Utils.applyUpdates(updates, managementClient.getControllerClient());
        }
    }

    /**
     * A {@link ServerSetupTask} instance which creates keystores and property files for this test case. It also back-ups
     * original SSLContext and sets it back in {@link #tearDown(ManagementClient, String)} method.
     */
    static class FilesSetup implements ServerSetupTask {

        private SSLContext origSSLContext;

        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            WORK_DIR.mkdir();
            FileUtils.touch(USERS_FILE);
            FileUtils.write(ROLES_FILE, "CN\\=" + CLIENT_AUTHORIZED_NAME + "=" + HelloBean.ROLE_ALLOWED);
            createKeystoreTruststore(SERVER_NAME, SERVER_KEYSTORE_FILE, CLIENTS_TRUSTSTORE_FILE);
            createKeystoreTruststore(CLIENT_AUTHORIZED_NAME, null, SERVER_TRUSTSTORE_FILE);
            createKeystoreTruststore(CLIENT_NOT_AUTHORIZED_NAME, null, SERVER_TRUSTSTORE_FILE);
            createKeystoreTruststore(CLIENT_NOT_TRUSTED_NAME, null, null);

            //backup SSLContext
            origSSLContext = SSLContext.getDefault();
        }

        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            SSLContext.setDefault(origSSLContext);
            FileUtils.deleteQuietly(WORK_DIR);
        }

        private void createKeystoreTruststore(String name, File keystoreFile, File truststoreFile)
                throws IllegalStateException, IOException, GeneralSecurityException {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            final KeyPair keyPair = keyPairGenerator.generateKeyPair();
            final X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();
            final long now = System.currentTimeMillis();
            v3CertGen.setNotBefore(new Date(now - 1000L * 60 * 60 * 24 * 30));
            v3CertGen.setNotAfter(new Date(now + 1000L * 60 * 60 * 24 * 365));
            final X509Principal dn = new X509Principal("CN=" + name);
            v3CertGen.setIssuerDN(dn);
            v3CertGen.setSubjectDN(dn);
            v3CertGen.setPublicKey(keyPair.getPublic());
            v3CertGen.setSignatureAlgorithm("SHA256withRSA");
            final SecureRandom sr = new SecureRandom();
            v3CertGen.setSerialNumber(BigInteger.ONE);
            X509Certificate certificate = v3CertGen.generate(keyPair.getPrivate(), sr);

            //save keystore to a new file
            final KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null, null);
            keystore.setKeyEntry(name, keyPair.getPrivate(), KEYSTORE_PASSWORD.toCharArray(),
                    new java.security.cert.Certificate[]{certificate});
            if (keystoreFile == null) {
                keystoreFile = getClientKeystoreFile(name);
            }
            final OutputStream ksOut = new FileOutputStream(keystoreFile);
            keystore.store(ksOut, KEYSTORE_PASSWORD.toCharArray());
            ksOut.close();

            //if requested, save truststore
            if (truststoreFile != null) {
                final KeyStore truststore;
                //if the truststore exists already, use it
                if (truststoreFile.exists()) {
                    truststore = KeyStoreUtil.getKeyStore(truststoreFile, KEYSTORE_PASSWORD.toCharArray());
                } else {
                    truststore = KeyStore.getInstance("JKS");
                    truststore.load(null, null);
                }
                truststore.setCertificateEntry(name, certificate);
                final OutputStream tsOut = new FileOutputStream(truststoreFile);
                truststore.store(tsOut, KEYSTORE_PASSWORD.toCharArray());
                tsOut.close();
            }
        }
    }
}
