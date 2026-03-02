/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote;

import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.PropertyPermission;

import jakarta.ejb.NoSuchEJBException;

import static org.junit.jupiter.api.Assertions.*;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.IncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatelessTransactionBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatelessTransactionNoResourceBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingletonRemote;
import org.jboss.as.test.integration.transactions.RemoteLookups;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * <p>
 * This is a set of tests which verify how transaction affinity on standalone EJB client
 * works when client communicates with cluster.
 * When transaction is started then all EJB calls has to be strongly bound to the server
 * that the first call was run at.<br/>
 * A note: the subordinate transaction is not enlisted until it's used. It means the
 * <code>txn.begin()</code> does define affinity it's defined at time the bean call is run.
 * </p>
 * <p>
 * The client uses three different ways to define destination of the communication,
 * a.k.a. to define URL of the cluster where it aims to connect to. The test runs the three options:
 * <ul>
 *   <li><code>wildfly-config.xml</code></li>
 *   <li>during InitialContext creation, using <code>remote+http</code> protocol</li>
 *   <li>during InitialContext creation, using <code>http</code> protocol
 *       (see <a href="https://github.com/wildfly/wildfly-http-client">wildfly/wildfly-http-client</a></li>
 * </ul>
 * </p>
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class TransactionalRemoteStatelessTestCase extends AbstractClusteringTestCase {
    private static final String MODULE_NAME = TransactionalRemoteStatelessTestCase.class.getSimpleName();

    private int node0Port = 8080;
    private int node1Port = 8180;

    private TransactionCheckerSingletonRemote checkerNode0, checkerNode1;

    private static enum InitialContextLookupType {
        REMOTE_HTTP, HTTP, WILDFLY_CONFIG
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar")
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class)
                .addClasses(StatelessTransactionBean.class, StatelessTransactionNoResourceBean.class)
                .addPackage(org.jboss.as.test.integration.transactions.TestXAResource.class.getPackage())
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                        new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml");
    }

    @BeforeEach
    void setUp() {
        try {
            checkerNode0 = RemoteLookups.lookupEjbStateless(
                    TESTSUITE_NODE0, node0Port, MODULE_NAME,
                    TransactionCheckerSingleton.class, TransactionCheckerSingletonRemote.class);
            checkerNode1 = RemoteLookups.lookupEjbStateless(
                    TESTSUITE_NODE1, node1Port, MODULE_NAME,
                    TransactionCheckerSingleton.class, TransactionCheckerSingletonRemote.class);
        } catch (NamingException | URISyntaxException e) {
            new IllegalStateException(String.format("Cannot find singleton for checking "
                    + "transaction state at '%s:%s'", TESTSUITE_NODE0, node0Port));
        }
        checkerNode0.resetAll();
        checkerNode1.resetAll();

        checkAndCleanTransactionState();
    }

    private void checkAndCleanTransactionState() {
        try {
            InitialContext initCtx = getInitialContext(
                    InitialContextLookupType.WILDFLY_CONFIG, TESTSUITE_NODE0);
            UserTransaction txn = getUserTransaction(initCtx);
            if(txn.getStatus() == Status.STATUS_ACTIVE) {
                txn.rollback();
            }
        } catch (Exception e) {
            log.errorf("Cannot check state and clean transaction to be obtained "
                    + "from server defined in wildfly-config.xml", e);
        }
    }

    @Test
    void affinityCommit_remoteHttp() throws Exception {
        affinityCommit(InitialContextLookupType.REMOTE_HTTP);
    }

    @Test
    void affinityCommit_http() throws Exception {
        affinityCommit(InitialContextLookupType.HTTP);
    }

    @Test
    void affinityCommit_wildflyConfig() throws Exception {
        affinityCommit(InitialContextLookupType.WILDFLY_CONFIG);
    }

    /**
     * Looking-up for two different beans under single transaction context.
     * The both beans should be called at the same node.
     * The node can be arbitrary but it has to be the one defined.
     */
    private void affinityCommit(InitialContextLookupType lookupType) throws Exception {
        InitialContext initCtx = getInitialContext(lookupType, TESTSUITE_NODE0);
        String targetNode = null;
        UserTransaction txn = null;

        try {
            txn = getUserTransaction(initCtx);

            txn.begin();

            Incrementor bean  = getStatelessRemoteBean(initCtx, StatelessTransactionBean.class);
            Result<Integer> result = bean.increment();
            assertEquals(1, result.getValue().intValue());

            targetNode = result.getNode();

            // lookup a different bean at the remote server while using the same context at client
            InitialContext initCtx2 = getInitialContext(lookupType, TESTSUITE_NODE1);
            Incrementor bean2  = getStatelessRemoteBean(initCtx2, StatelessTransactionNoResourceBean.class);
            Result<Integer> resultSecondCall = bean2.increment();
            assertEquals(1, resultSecondCall.getValue().intValue());
            assertEquals(targetNode, resultSecondCall.getNode());

            txn.commit();
        } catch (Exception e) {
            if(txn != null) txn.rollback();
            throw e;
        } finally {
            initCtx.close();
        }

        if(targetNode.equals(NODE_1)) {
            assertEquals(1,
                    checkerNode0.getCommitted(), "Transaction affinity was to '" + NODE_1 + "' the commit should be processed there");
            assertEquals(0, checkerNode0.getRolledback(), "Rollback at '" + NODE_1 + "' should not be called");
            assertEquals(2,
                    checkerNode0.countSynchronizedAfterCommitted(), "Two synchronizations were registered at '" + NODE_1 + "'");
            assertEquals(0, checkerNode1.getCommitted(), "Expected no commit to be run at " + NODE_2);
            assertEquals(0, checkerNode1.getRolledback(), "Expected no rollback to be run at " + NODE_2);
        } else if (targetNode.equals(NODE_2)) {
            assertEquals(0, checkerNode0.getCommitted(), "Expected no commit to be run at " + NODE_1);
            assertEquals(0, checkerNode0.getRolledback(), "Expected no rollback to be run at " + NODE_1);
            assertEquals(1,
                    checkerNode1.getCommitted(), "Transaction affinity was to '" + NODE_2 + "' the commit should be processed there");
            assertEquals(0, checkerNode1.getRolledback(), "Rollback at '" + NODE_2 + "' should not be called");
            assertEquals(2,
                    checkerNode1.countSynchronizedAfterCommitted(), "Two synchronizations were registered at '" + NODE_2 + "'");
        } else {
            fail(String.format("Expecting one of the nodes [%s,%s] should be hit "
                    + "by the bean call but the target node was not expected '%s'",
                    NODE_1, NODE_2, targetNode));
        }
    }

    /**
     * Purpose of the test is to verify that <code>remote+http</code> {@link InitialContext}
     * lookup uses the URL hostname which is defined in the JNDI properties
     * during the context creation. When non-existing URL is provided
     * then the failure is expected.
     */
    @Test
    void directLookupFailure() throws Exception {
        InitialContext initCtx = getInitialContext(InitialContextLookupType.REMOTE_HTTP, "non-existing-hostname");

        try {
            Incrementor bean  = getStatelessRemoteBean(initCtx, StatelessTransactionBean.class);
            bean.increment();
            fail("Expected a Exception as the bean is not deployed at 'node-3'"
                    + "while the initial context points the lookup there");
        } catch (org.jboss.ejb.client.RequestSendFailedException expected) {
            // expected as the deployment was removed from the node
        }
    }

    @Test
    void rollback_remoteHttp() throws Exception {
        rollback(InitialContextLookupType.REMOTE_HTTP);
    }

    @Test
    void rollback_http() throws Exception {
        rollback(InitialContextLookupType.HTTP);
    }

    @Test
    void rollback_wildflyConfig() throws Exception {
        rollback(InitialContextLookupType.WILDFLY_CONFIG);
    }

    /**
     * Checking if the rollback is commanded to the remote node
     * when the remotely looked-up transaction is rolled-back on the client side.
     */
    private void rollback (InitialContextLookupType lookupType) throws Exception {
        InitialContext initCtx = getInitialContext(lookupType, TESTSUITE_NODE0);
        String targetNode = null;
        UserTransaction txn = null;

        try {
            Incrementor bean  = getStatelessRemoteBean(initCtx, StatelessTransactionBean.class);
            txn = getUserTransaction(initCtx);

            txn.begin();

            Result<Integer> result = bean.increment();
            assertEquals(1, result.getValue().intValue());
            targetNode = result.getNode();

            txn.rollback();
        } catch (Exception e) {
            if(txn != null) txn.rollback();
            throw e;
        } finally {
            initCtx.close();
        }

        if(targetNode.equals(NODE_1)) {
            assertEquals(0, checkerNode0.getCommitted(), "Commit at '" + NODE_1 + "' should not be called");
            assertEquals(1, checkerNode0.getRolledback(), "Transaction affinity was to '" + NODE_1 + "' the rollback should be processed there");
            assertEquals(1, checkerNode0.countSynchronizedAfterRolledBack(), "A synchronization was registered at '" + NODE_1 + "'");
            assertEquals(0, checkerNode1.getCommitted(), "Expected no commit to be run at " + NODE_2);
            assertEquals(0, checkerNode1.getRolledback(), "Expected no rollback to be run at " + NODE_2);
        } else if (targetNode.equals(NODE_2)) {
            assertEquals(0, checkerNode0.getCommitted(), "Expected no commit to be run at " + NODE_1);
            assertEquals(0, checkerNode0.getRolledback(), "Expected no rollback to be run at " + NODE_1);
            assertEquals(0, checkerNode1.getCommitted(), "Commit at '" + NODE_2 + "' should not be called");
            assertEquals(1, checkerNode1.getRolledback(), "Transaction affinity was to '" + NODE_2 + "' the rollback should be processed there");
            assertEquals(1, checkerNode1.countSynchronizedAfterRolledBack(), "A synchronization was registered at '" + NODE_2 + "'");
        } else {
            fail(String.format("Expecting one of the nodes [%s,%s] should be hit "
                    + "by the bean call but the target node was not expected '%s'",
                    NODE_1, NODE_2, targetNode));
        }
    }

    /**
     * Rollback has to be run when commanded at the client side
     * despite there is no real resource enlisted to the transaction at the server side.
     */
    @Test
    void rollbackWithNoXAResourceEnlistment() throws Exception {
        InitialContext initCtx = getInitialContext(InitialContextLookupType.REMOTE_HTTP, TESTSUITE_NODE0);
        Result<Integer> result = null;
        UserTransaction txn = null;

        try {
            Incrementor bean  = getStatelessRemoteBean(initCtx, StatelessTransactionNoResourceBean.class);
            txn = getUserTransaction(initCtx);

            txn.begin();

            result = bean.increment();
            assertEquals(1, result.getValue().intValue());

            txn.rollback();
        } catch (Exception e) {
            if(txn != null) txn.rollback();
            throw e;
        } finally {
            initCtx.close();
        }

        if(result.getNode().equals(NODE_1)) {
            assertFalse(checkerNode0.isSynchronizedBefore(), "Transaction was rolled-back at " + NODE_1 +
                    "Synchronization.beforeCompletion can't be called");
            assertEquals(1, checkerNode0.countSynchronizedAfterRolledBack(), "Transaction was rolled-back at " + NODE_1 +
                    ", after synchronization callback needs to be called");
            assertFalse(checkerNode1.isSynchronizedBefore(), "Expected no beforeCompletion to be run at " + NODE_2);
            assertFalse(checkerNode1.isSynchronizedAfter(), "Expected no afterCompletion to be run at " + NODE_2);
        } else if (result.getNode().equals(NODE_2)) {
            assertFalse(checkerNode0.isSynchronizedBefore(), "Expected no beforeCompletion to be run at " + NODE_1);
            assertFalse(checkerNode0.isSynchronizedAfter(), "Expected no afterCompletion to be run at " + NODE_1);
            assertFalse(checkerNode1.isSynchronizedBefore(), "Transaction was rolled-back at " + NODE_2 +
                    "Synchronization.beforeCompletion can't be called");
            assertEquals(1, checkerNode1.countSynchronizedAfterRolledBack(), "Transaction was rolled-back at " + NODE_2 +
                    ", after synchronization callback needs to be called");
        } else {
            fail(String.format("Expecting one of the nodes [%s,%s] should be hit "
                    + "by the bean call but the target node was not expected '%s'",
                    NODE_1, NODE_2, result.getNode()));
        }
    }

    /**
     * This is a similar test to
     * {@link TransactionalRemoteStatefulEJBFailoverTestCase#test(ManagementClient, ManagementClient)}
     * but this test works with a stateless bean.
     */
    @Test
    void affinityNodeFailure() throws Exception {
        InitialContext initCtx = getInitialContext(InitialContextLookupType.REMOTE_HTTP, TESTSUITE_NODE0);
        String targetNode = null;
        UserTransaction txn = null;

        try {
            txn = getUserTransaction(initCtx);

            txn.begin();

            Incrementor bean  = getStatelessRemoteBean(initCtx, StatelessTransactionBean.class);
            Result<Integer> result = bean.increment();
            int count = 1;
            assertEquals(count++, result.getValue().intValue());

            targetNode = result.getNode();

            undeploy(this.findDeployment(targetNode));

            try {
                result = bean.increment();
                fail("Expected a NoSuchEJBException as transaction affinity needs to be maintained");
            } catch (NoSuchEJBException | AssertionError expected) {
                // expected as the deployment was removed from the node
            }
        } finally {
            if(txn != null) txn.rollback();
            initCtx.close();
        }
    }

    private Incrementor getStatelessRemoteBean(InitialContext ctx, Class<? extends Incrementor> beanClass) {
        return getRemoteBean(ctx, beanClass, false);
    }

    private Incrementor getRemoteBean(InitialContext ctx, Class<? extends Incrementor> beanClass, boolean isStateful) {
        String lookupBeanUrl = "ejb:/"  + MODULE_NAME +"/" + beanClass.getSimpleName() + "!" + Incrementor.class.getName();
        if(isStateful) lookupBeanUrl += "?stateful";

        try {
            return (Incrementor) ctx.lookup(lookupBeanUrl);
        } catch (NamingException ne) {
            throw new IllegalStateException(String.format("Cannot find ejb bean '%s' while "
                    + "looking up for name '%s'", beanClass, lookupBeanUrl), ne);
        }
    }

    private UserTransaction getUserTransaction(InitialContext ctx) {
        String lookupUserTransaction = "txn:RemoteUserTransaction";
        try {
            return (UserTransaction) ctx.lookup(lookupUserTransaction);
        } catch (NamingException ne) {
            throw new IllegalStateException(String.format("Cannot look up '%s' at context: %s",
                    ctx, lookupUserTransaction), ne);
        }
    }

    private InitialContext getInitialContext(InitialContextLookupType lookupType, String nodeHostName) {
        final Hashtable<String, String> jndiProperties = new Hashtable<>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        if(nodeHostName.contains(":") && !nodeHostName.contains("[")) {
            nodeHostName = "[" + nodeHostName + "]"; // escaping host and port for IPv6
        }

        String lookupProviderUrl = "<see wildfly-config.xml>";
        switch(lookupType) {
            case HTTP:
                lookupProviderUrl = String.format("http://%s:%s/wildfly-services", nodeHostName, node0Port);
                jndiProperties.put(Context.PROVIDER_URL, lookupProviderUrl);
                // see server/configuration/application-users.properties
                jndiProperties.put(Context.SECURITY_PRINCIPAL, "user1");
                jndiProperties.put(Context.SECURITY_CREDENTIALS, "password1");
                break;
            case REMOTE_HTTP:
                lookupProviderUrl = String.format("remote+http://%s:%s", nodeHostName, node0Port);
                jndiProperties.put(Context.PROVIDER_URL, lookupProviderUrl);
                break;
            case WILDFLY_CONFIG:
            default:
                // leaving as it is, remote host config taken from the client's wildfly-config.xml
                log.debugf("Not using value '%s' of nodeHostName parameter, "
                        + "wildfly-config.xml defines the URL on its own", nodeHostName);
        }

        try {
            return new InitialContext(jndiProperties);
        } catch (NamingException ne) {
            throw new IllegalStateException(String.format(
                    "Cannot create InitialContext with url provider '%s'",
                    lookupProviderUrl), ne);
        }
    }
}
