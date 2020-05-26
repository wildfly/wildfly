/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.ejb.client.outbound.connection.transaction.preparehalt;

import com.arjuna.ats.arjuna.recovery.RecoveryDriver;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.transactions.RemoteLookups;
import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingletonRemote;
import org.jboss.as.test.manualmode.ejb.client.outbound.connection.EchoOnServerOne;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.PropertyPermission;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createFilePermission;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Testing transaction recovery on cases where WildFly calls remote EJB on the other WildFly instance.
 * The transaction context is propagated along this remote call and then some failure or JVM crash happens.
 * When the failure (e.g. simulation of intermittent erroneous state) or when servers are restarted
 * (in case the JVM had been crashed) then the transaction recovery must make the system data consistent again.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(TransactionPropagationPrepareHaltTestCase.TransactionStatusManagerSetup.class)
public class TransactionPropagationPrepareHaltTestCase {
    private static final Logger log = Logger.getLogger(TransactionPropagationPrepareHaltTestCase.class);

    private static final String CLIENT_SERVER_NAME = "jbossas-with-remote-outbound-connection-non-clustered";
    private static final String SERVER_SERVER_NAME = "jbossas-non-clustered";
    private static final String CLIENT_DEPLOYMENT = "txn-prepare-halt-client";
    private static final String SERVER_DEPLOYMENT = "txn-prepare-halt-server";

    private static final ModelNode ADDRESS_TRANSACTIONS
            = new ModelNode().add("subsystem", "transactions");
    private static final ModelNode ADDRESS_SOCKET_BINDING
            = new ModelNode().add(ClientConstants.SOCKET_BINDING_GROUP, "standard-sockets");
    static {
        ADDRESS_TRANSACTIONS.protect();
        ADDRESS_SOCKET_BINDING.protect();
    }

    @Deployment(name = CLIENT_DEPLOYMENT, testable = false)
    @TargetsContainer(CLIENT_SERVER_NAME)
    public static Archive<?> clientDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, CLIENT_DEPLOYMENT + ".jar")
            .addClasses(TransactionalRemote.class, ClientBean.class, ClientBeanRemote.class)
            .addPackage(TestXAResource.class.getPackage())
            .addAsManifestResource(EchoOnServerOne.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml")
            .addAsManifestResource(createPermissionsXmlAsset(
                new RuntimePermission("exitVM", "none"),
                createFilePermission("read,write", "basedir",
                    Arrays.asList("target", "jbossas-with-remote-outbound-connection", "standalone", "data", "ejb-xa-recovery")),
                createFilePermission("read,write", "basedir",
                    Arrays.asList("target", "jbossas-with-remote-outbound-connection", "standalone", "data", "ejb-xa-recovery", "-"))
                ), "permissions.xml")
            .addAsManifestResource(new StringAsset("Dependencies: org.jboss.jts\n"), "MANIFEST.MF");
        return jar;
    }

    @Deployment(name = SERVER_DEPLOYMENT, testable = false)
    @TargetsContainer(SERVER_SERVER_NAME)
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SERVER_DEPLOYMENT + ".jar")
            .addClasses(TransactionalBean.class, TransactionalRemote.class)
            .addPackages(true, TestXAResource.class.getPackage())
            .addAsManifestResource(createPermissionsXmlAsset(
                new PropertyPermission("jboss.server.data.dir", "read"),
                createFilePermission("read,write", "basedir", Arrays.asList("target", "wildfly", "standalone", "data")),
                createFilePermission("read,write", "basedir", Arrays.asList("target", "wildfly", "standalone", "data", "-"))
            ), "permissions.xml")
            .addAsManifestResource(new StringAsset("Dependencies: org.jboss.jts\n"), "MANIFEST.MF");
        return jar;
    }

    static class TransactionStatusManagerSetup extends CLIServerSetupTask {
        public TransactionStatusManagerSetup() {
            this.builder
                    .node(CLIENT_SERVER_NAME)
                    .setup("/subsystem=transactions:write-attribute(name=recovery-listener, value=true)")
                    .reloadOnSetup(true);
        }
    }

    @ArquillianResource
    private ContainerController container;

    @ContainerResource(CLIENT_SERVER_NAME)
    private ManagementClient managementClientClient;

    @ContainerResource(SERVER_SERVER_NAME)
    private ManagementClient managementClientServer;

    @Before
    public void setUp() throws URISyntaxException, NamingException {
        this.container.start(SERVER_SERVER_NAME);
        this.container.start(CLIENT_SERVER_NAME);
        // clean the singleton bean which stores TestXAResource calls
        RemoteLookups.lookupEjbStateless(managementClientServer, SERVER_DEPLOYMENT,
                TransactionCheckerSingleton.class, TransactionCheckerSingletonRemote.class).resetAll();
    }

    @After
    public void tearDown() {
        try {
             container.stop(CLIENT_SERVER_NAME);
        } finally {
             container.stop(SERVER_SERVER_NAME);
        }
    }

    /**
     * <p>Reproducer for JBEAP-19408</p>
     * <p>
     * Test scenario:
     * <ol>
     *   <li>client WildFly starts the transactions and calls remote EJB bean at server WildFly</li>
     *   <li>{@link ClientBean} method finishes and transaction manager starts 2PC</li>
     *   <li>client WildFly asks the server WildFly to <code>prepare</code> as part of the 2PC procesing</li>
     *   <li>server WildFly prepares but after the call is returned the client WildFly JVM crashes</li>
     *   <li>client WildFly restarts</li>
     *   <li>client WildFly runs transaction recovery which uses orphan detection
     *       to rollback the prepared data at server WildFly</li>
     * </ol>
     * </p>
     */
    @Test
    public void prepareCrashOnClient() throws Exception {
        try {
            ClientBeanRemote bean = RemoteLookups.lookupEjbStateless(managementClientClient, CLIENT_DEPLOYMENT,
                    ClientBean.class, ClientBeanRemote.class);
            bean.twoPhaseCommitCrashAtClient(SERVER_DEPLOYMENT);
            Assert.fail("Test expects transaction rollback outcome instead of commit");
        } catch (Throwable expected) {
            log.debugf(expected,"Exception expected as the client server '%s' should be crashed", CLIENT_SERVER_NAME);
        }

        try {
            container.kill(CLIENT_SERVER_NAME);
        } catch (Exception expected) {
            // container was JVM crashed by the test, this call let the arquillian know that the container is not running
            log.debugf(expected,"Server %s can't be killed as the server was halt by test already", CLIENT_SERVER_NAME);
        }
        container.start(CLIENT_SERVER_NAME);

        String transactionSocketBinding = readAttribute(managementClientClient, ADDRESS_TRANSACTIONS,"socket-binding").asString();
        ModelNode addressSocketBinding = ADDRESS_SOCKET_BINDING.clone();
        addressSocketBinding.add(ClientConstants.SOCKET_BINDING, transactionSocketBinding);
        String host = readAttribute(managementClientClient, addressSocketBinding, "bound-address").asString();
        int port = readAttribute(managementClientClient, addressSocketBinding, "bound-port").asInt();

        TransactionCheckerSingletonRemote checker = RemoteLookups.lookupEjbStateless(managementClientServer, SERVER_DEPLOYMENT,
                TransactionCheckerSingleton.class, TransactionCheckerSingletonRemote.class);
        Assert.assertEquals("Expecting the transaction was interrrupted and no rollback was called yet",
                0, checker.getRolledback());

        log.debugf("Transaction recovery will be run for application server at %s:%s", host, port);
        Assert.assertTrue("Expecting recovery #1 being run without any issue", runTransactionRecovery(host, port));
        Assert.assertTrue("Expecting recovery #2 being run without any issue", runTransactionRecovery(host, port));

        Assert.assertEquals("Expecting the rollback to be called on the server during recovery",
             1, checker.getRolledback());
    }

    private boolean runTransactionRecovery(String host, int port) {
        RecoveryDriver recoveryDriver = new RecoveryDriver(port, host);
        try {
            return recoveryDriver.synchronousVerboseScan(TimeoutUtil.adjust(60 * 1000), 5);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot run transaction recovery synchronous scan for " + host + ":" + port, e);
        }
    }

    private ModelNode readAttribute(final ManagementClient managementClient, ModelNode address, String name) throws IOException, MgmtOperationException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(ClientConstants.READ_ATTRIBUTE_OPERATION);
        operation.get(ModelDescriptionConstants.INCLUDE_DEFAULTS).set("true");
        operation.get(ModelDescriptionConstants.RESOLVE_EXPRESSIONS).set("true");
        operation.get(ClientConstants.NAME).set(name);
        return ManagementOperations.executeOperation(managementClient.getControllerClient(), operation);
    }
}
