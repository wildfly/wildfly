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

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.transactions.RecoveryExecutor;
import org.jboss.as.test.integration.transactions.RemoteLookups;
import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingletonRemote;
import org.jboss.as.test.manualmode.ejb.client.outbound.connection.EchoOnServerOne;
import org.jboss.as.test.shared.CLIServerSetupTask;
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

import javax.ejb.EJBException;
import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;

import java.io.File;
import java.io.FilePermission;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.PropertyPermission;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createFilePermission;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Testing transaction recovery on cases where WildFly calls remote Jakarta Enterprise Beans on the other WildFly instance.
 * The transaction context is propagated along this remote call and then some failure or JVM crash happens.
 * When the failure (e.g. simulation of intermittent erroneous state) or when servers are restarted
 * (in case the JVM had been crashed) then the transaction recovery must make the system data consistent again.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(TransactionPropagationFailureTestCase.TransactionStatusManagerSetup.class)
public class TransactionPropagationFailureTestCase {
    private static final Logger log = Logger.getLogger(TransactionPropagationFailureTestCase.class);

    private static final String CLIENT_SERVER_NAME = "jbossas-with-remote-outbound-connection-non-clustered";
    private static final String SERVER_SERVER_NAME = "jbossas-non-clustered";
    private static final String CLIENT_DEPLOYMENT = "txn-prepare-halt-client";
    private static final String SERVER_DEPLOYMENT = "txn-prepare-halt-server";

    private static final String CLIENT_SERVER_JBOSS_HOME = "jbossas-with-remote-outbound-connection";
    private static final String WFTC_DATA_DIRECTORY_NAME = "ejb-xa-recovery";

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
                    Arrays.asList("target", CLIENT_SERVER_JBOSS_HOME, "standalone", "data", WFTC_DATA_DIRECTORY_NAME)),
                createFilePermission("read,write", "basedir",
                    Arrays.asList("target", CLIENT_SERVER_JBOSS_HOME, "standalone", "data", WFTC_DATA_DIRECTORY_NAME, "-")),
                new FilePermission(System.getProperty("jboss.home") + File.separatorChar + "standalone" + File.separatorChar + "tmp" + File.separatorChar + "auth" + File.separatorChar + "-", "read")
                ), "permissions.xml")
            .addAsManifestResource(new StringAsset("Dependencies: org.jboss.jts\n"), "MANIFEST.MF");
        return jar;
    }

    @Deployment(name = SERVER_DEPLOYMENT, testable = false)
    @TargetsContainer(SERVER_SERVER_NAME)
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SERVER_DEPLOYMENT + ".jar")
            .addClasses(TransactionalBean.class, TransactionalRemote.class, TestCommitFailureXAResource.class)
            .addPackages(true, TestXAResource.class.getPackage())
            .addAsManifestResource(createPermissionsXmlAsset(
                new PropertyPermission("jboss.server.data.dir", "read"),
                // PersistentTestXAResource requires the following permissions
                createFilePermission("read", "basedir", Arrays.asList("target", "wildfly", "standalone", "data")),
                createFilePermission("read,write", "basedir", Arrays.asList("target", "wildfly", "standalone", "data", "PersistentTestXAResource"))
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
     *   <li>client WildFly starts the transactions and calls remote Jakarta Enterprise Beans bean at server WildFly</li>
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
            // container was JVM crashed by the test, this call let the arquillian know that the container is not running
            container.kill(CLIENT_SERVER_NAME);
        } catch (Exception ignore) {
            // ignoring the kill issue as the server process will be killed at the end (Arquillian uses Process.destroyForcibly)
            // at this time the Arquillian container inner state could be 'KILLED_FAILED' which is still fine to restart the app server
            log.debugf("Arquillian kill of " + CLIENT_SERVER_NAME + " failed, but the process is killed and we can ignore it", ignore);
        }
        // starting the container (JVM crashed by call Runtime.getRuntime().halt), test will be running transaction recovery on restarted server
        container.start(CLIENT_SERVER_NAME);

        RecoveryExecutor recoveryExecutor = new RecoveryExecutor(managementClientClient);

        TransactionCheckerSingletonRemote serverSingletonChecker = RemoteLookups.lookupEjbStateless(managementClientServer,
                SERVER_DEPLOYMENT, TransactionCheckerSingleton.class, TransactionCheckerSingletonRemote.class);
        Assert.assertEquals("Expecting the transaction was interrupted and no rollback was called yet",
                0, serverSingletonChecker.getRolledback());

        Assert.assertTrue("Expecting recovery #1 being run without any issue", recoveryExecutor.runTransactionRecovery());
        Assert.assertTrue("Expecting recovery #2 being run without any issue", recoveryExecutor.runTransactionRecovery());

        Assert.assertEquals("Expecting the rollback to be called on the server during recovery",
             1, serverSingletonChecker.getRolledback());
        assertEmptyClientWftcDataDirectory();
    }

    /**
     * <p>Reproducer for JBEAP-19435</p>
     * <p>
     * Test scenario:
     *   <ol>
     *     <li>client WildFly starts the transactions and calls remote Jakarta Enterprise Beans bean at server WildFly,
     *         there are two resources enlisted on the client side and one resource is enlisted on the other server</li>
     *     <li>{@link ClientBean} method finishes and transaction manager starts 2PC</li>
     *     <li>client WildFly asks the server WildFly to <code>prepare</code> and <code>commit</code></li>
     *     <li>server WildFly prepares but the <code>commit</code> of XAResource fails with intermittent failure on the server</li>
     *     <li>client WildFly runs transaction recovery and expects the unfinished commit is finished with success
     *         and just after the commit is processed the WFTC registry is empty</li>
     *   </ol>
     * </p>
     */
    @Test
    public void recoveryCommitRetryOnFailure() throws Exception {
        ClientBeanRemote bean = RemoteLookups.lookupEjbStateless(managementClientClient, CLIENT_DEPLOYMENT,
                ClientBean.class, ClientBeanRemote.class);
        // ClientBean#twoPhaseIntermittentCommitFailureOnServer -> TransactionalBean#intermittentCommitFailure
        bean.twoPhaseIntermittentCommitFailureOnServer(SERVER_DEPLOYMENT);

        RecoveryExecutor recoveryExecutor = new RecoveryExecutor(managementClientClient);
        TransactionCheckerSingletonRemote serverSingletonChecker = RemoteLookups.lookupEjbStateless(managementClientServer,
                SERVER_DEPLOYMENT, TransactionCheckerSingleton.class, TransactionCheckerSingletonRemote.class);

        Assert.assertEquals("Expecting the transaction was interrupted and commit was called on resources at the server yet",
                0, serverSingletonChecker.getCommitted());

        // the second call of the recovery execution is needed to manage the race condition
        // if the recovery scan is already in progress started by periodic transaction recovery
        // the test needs to ensure that recovery cycle was finished as whole but we need to check the state
        // after the first recovery cycle the test checks that the WFTC data is deleted immediately after the transaction is committed.
        Assert.assertTrue("Expecting recovery being run without any issue", recoveryExecutor.runTransactionRecovery());
        if(serverSingletonChecker.getCommitted() == 0) {
            Assert.assertTrue("Expecting recovery #2 being run without any issue", recoveryExecutor.runTransactionRecovery());
        }

        Assert.assertEquals("Expecting the commit to be called on the server during recovery",
                1, serverSingletonChecker.getCommitted());
        assertEmptyClientWftcDataDirectory();
    }

    /**
     * <p>
     * Test scenario:
     *   <ol>
     *     <li>client WildFly starts the transactions and calls remote Jakarta Enterprise Beans bean at server WildFly,
     *         there is one resource enlisted on the client side but two resources are enlisted on the other server</li>
     *     <li>{@link ClientBean} method finishes and transaction manager starts 1PC as only one resource was enslited
     *         on the client side which is the owner of the transaction</li>
     *     <li>client WildFly asks the server WildFly to <code>commit</code> as 1PC is used</li>
     *     <li>server WildFly <code>commit</code> of XAResource fails with intermittent failure on the server
     *         which for 1PC emits the heuristic exceptions, as an exception which requires manual fixing</li>
     *     <li>the test pretend an administrator to call ':recover' JBoss CLI call on transaction participants
     *         and the transaction is moved from 'heuristic' state to 'prepared' state</li>
     *     <li>the transaction recovery should be able to fix the transaction in the 'prepared' state state and commit it</li>
     *   </ol>
     * </p>
     */
    @Test
    public void recoveryOnePhaseCommitRetryOnFailure() throws Exception {
        ClientBeanRemote bean = RemoteLookups.lookupEjbStateless(managementClientClient, CLIENT_DEPLOYMENT,
                ClientBean.class, ClientBeanRemote.class);
        try {
            // ClientBean#onePhaseIntermittentCommitFailureOnServer -> TransactionalBean#intermittentCommitFailureTwoPhase
            bean.onePhaseIntermittentCommitFailureOnServer(SERVER_DEPLOYMENT);
            Assert.fail("Test expects the method call to fail with EJBException but no exception was thrown.");
        } catch (EJBException ejbe) {
            if (!(ejbe.getCausedByException() instanceof HeuristicMixedException)) {
                log.errorf(ejbe,"Wrong exception type was obtained on 1PC remote Jakarta Enterprise Beans call. Expected %s to be caught..",
                        HeuristicMixedException.class.getName());
                Assert.fail("Expecting the remote 1PC Jakarta Enterprise Beans call fails with HeuristicMixedException but it did not happen" +
                        " and the " + ejbe + " was caught instead.");
            }
        }

        RecoveryExecutor recoveryExecutor = new RecoveryExecutor(managementClientClient);
        TransactionCheckerSingletonRemote serverSingletonChecker = RemoteLookups.lookupEjbStateless(managementClientServer,
                SERVER_DEPLOYMENT, TransactionCheckerSingleton.class, TransactionCheckerSingletonRemote.class);

        Assert.assertEquals("Expecting the transaction was interrupted on one resource but the second " +
                        "should be still committed at the server", 1, serverSingletonChecker.getCommitted());

        //  running recovery on heuristic transaction should not recover it
        Assert.assertTrue("Expecting recovery being run without any issue", recoveryExecutor.runTransactionRecovery());
        Assert.assertEquals("Expecting no other resource to be committed when recovery is run as the transaction" +
                        " finished with heuristics and manual intervention is needed", 1, serverSingletonChecker.getCommitted());

        // running WFLY :recover operation on all participants of the transactions in the Narayana object store
        recoveryExecutor.cliRecoverAllTransactions();
        // after :recover is executed the heuristic state is changed to 'prepared'. The recovery should commit it later
        Assert.assertTrue("Expecting the recovery after cli :recover operation to be run without any issue",
                recoveryExecutor.runTransactionRecovery());
        if(serverSingletonChecker.getCommitted() == 1) { // ensuring one whole recovery cycle to be executed before the final check
            Assert.assertTrue("Expecting the #2 recovery after cli :recover operation to be run without any issue",
                    recoveryExecutor.runTransactionRecovery());
        }

        Assert.assertEquals("Expecting both resources on the server were committed",
                2, serverSingletonChecker.getCommitted());
        assertEmptyClientWftcDataDirectory();
    }

    private void assertEmptyClientWftcDataDirectory() {
        Path wftcDataDirectory = Paths.get("target", CLIENT_SERVER_JBOSS_HOME, "standalone", "data", WFTC_DATA_DIRECTORY_NAME);
        Assert.assertTrue("Expecting existence of WFTC data directory at " + wftcDataDirectory,
                wftcDataDirectory.toFile().isDirectory());
        String[] wftcFiles = wftcDataDirectory.toFile().list();
        Assert.assertEquals("WFTC data directory at " + wftcDataDirectory
                + " is not empty but it should be. There are files: " + Arrays.asList(wftcFiles), 0, wftcFiles.length);
    }
}
