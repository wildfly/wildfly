/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb.remote;

import java.util.Hashtable;
import java.util.PropertyPermission;

import javax.ejb.NoSuchEJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
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
import org.jboss.as.test.integration.transactions.TransactionTestLookupUtil;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
@RunWith(Arquillian.class)
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

    @Before
    public void setUp() {
        try {
            checkerNode0 = TransactionTestLookupUtil.lookupEjbStateless(
                    TESTSUITE_NODE0, node0Port, MODULE_NAME,
                    TransactionCheckerSingleton.class, TransactionCheckerSingletonRemote.class);
            checkerNode1 = TransactionTestLookupUtil.lookupEjbStateless(
                    TESTSUITE_NODE1, node1Port, MODULE_NAME,
                    TransactionCheckerSingleton.class, TransactionCheckerSingletonRemote.class);
        } catch (NamingException e) {
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
    public void affinityCommit_remoteHttp() throws Exception {
        affinityCommit(InitialContextLookupType.REMOTE_HTTP);
    }

    @Test
    public void affinityCommit_http() throws Exception {
        affinityCommit(InitialContextLookupType.HTTP);
    }

    @Test
    public void affinityCommit_wildflyConfig() throws Exception {
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
            Assert.assertEquals(1, result.getValue().intValue());

            targetNode = result.getNode();

            // lookup a different bean at the remote server while using the same context at client
            InitialContext initCtx2 = getInitialContext(lookupType, TESTSUITE_NODE1);
            Incrementor bean2  = getStatelessRemoteBean(initCtx2, StatelessTransactionNoResourceBean.class);
            Result<Integer> resultSecondCall = bean2.increment();
            Assert.assertEquals(1, resultSecondCall.getValue().intValue());
            Assert.assertEquals(targetNode, resultSecondCall.getNode());

            txn.commit();
        } catch (Exception e) {
            if(txn != null) txn.rollback();
            throw e;
        } finally {
            initCtx.close();
        }

        if(targetNode.equals(NODE_1)) {
            Assert.assertEquals("Transaction affinity was to '" + NODE_1 + "' the commit should be processed there",
                    1, checkerNode0.getCommitted());
            Assert.assertEquals("Rollback at '" + NODE_1 + "' should not be called", 0, checkerNode0.getRolledback());
            Assert.assertEquals("Two synchronizations were registered at '" + NODE_1 + "'",
                    2, checkerNode0.countSynchronizedAfterCommitted());
            Assert.assertEquals("Expected no commit to be run at " + NODE_2, 0, checkerNode1.getCommitted());
            Assert.assertEquals("Expected no rollback to be run at " + NODE_2, 0, checkerNode1.getRolledback());
        } else if (targetNode.equals(NODE_2)) {
            Assert.assertEquals("Expected no commit to be run at " + NODE_1, 0, checkerNode0.getCommitted());
            Assert.assertEquals("Expected no rollback to be run at " + NODE_1, 0, checkerNode0.getRolledback());
            Assert.assertEquals("Transaction affinity was to '" + NODE_2 + "' the commit should be processed there",
                    1, checkerNode1.getCommitted());
            Assert.assertEquals("Rollback at '" + NODE_2 + "' should not be called", 0, checkerNode1.getRolledback());
            Assert.assertEquals("Two synchronizations were registered at '" + NODE_2 + "'",
                    2, checkerNode1.countSynchronizedAfterCommitted());
        } else {
            Assert.fail(String.format("Expecting one of the nodes [%s,%s] should be hit "
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
    public void directLookupFailure () throws Exception {
        InitialContext initCtx = getInitialContext(InitialContextLookupType.REMOTE_HTTP, "non-existing-hostname");

        try {
            Incrementor bean  = getStatelessRemoteBean(initCtx, StatelessTransactionBean.class);
            bean.increment();
            Assert.fail("Expected a Exception as the bean is not deployed at 'node-3'"
                    + "while the initial context points the lookup there");
        } catch (org.jboss.ejb.client.RequestSendFailedException expected) {
            // expected as the deployment was removed from the node
        }
    }

    @Test
    public void rollback_remoteHttp() throws Exception {
        rollback(InitialContextLookupType.REMOTE_HTTP);
    }

    @Test
    public void rollback_http() throws Exception {
        rollback(InitialContextLookupType.HTTP);
    }

    @Test
    public void rollback_wildflyConfig() throws Exception {
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
            Assert.assertEquals(1, result.getValue().intValue());
            targetNode = result.getNode();

            txn.rollback();
        } catch (Exception e) {
            if(txn != null) txn.rollback();
            throw e;
        } finally {
            initCtx.close();
        }

        if(targetNode.equals(NODE_1)) {
            Assert.assertEquals("Commit at '" + NODE_1 + "' should not be called", 0, checkerNode0.getCommitted());
            Assert.assertEquals("Transaction affinity was to '" + NODE_1 + "' the rollback should be processed there", 1, checkerNode0.getRolledback());
            Assert.assertEquals("A synchronization was registered at '" + NODE_1 + "'", 1, checkerNode0.countSynchronizedAfterRolledBack());
            Assert.assertEquals("Expected no commit to be run at " + NODE_2, 0, checkerNode1.getCommitted());
            Assert.assertEquals("Expected no rollback to be run at " + NODE_2, 0, checkerNode1.getRolledback());
        } else if (targetNode.equals(NODE_2)) {
            Assert.assertEquals("Expected no commit to be run at " + NODE_1, 0, checkerNode0.getCommitted());
            Assert.assertEquals("Expected no rollback to be run at " + NODE_1, 0, checkerNode0.getRolledback());
            Assert.assertEquals("Commit at '" + NODE_2 + "' should not be called", 0, checkerNode1.getCommitted());
            Assert.assertEquals("Transaction affinity was to '" + NODE_2 + "' the rollback should be processed there", 1, checkerNode1.getRolledback());
            Assert.assertEquals("A synchronization was registered at '" + NODE_2 + "'", 1, checkerNode1.countSynchronizedAfterRolledBack());
        } else {
            Assert.fail(String.format("Expecting one of the nodes [%s,%s] should be hit "
                    + "by the bean call but the target node was not expected '%s'",
                    NODE_1, NODE_2, targetNode));
        }
    }

    /**
     * Rollback has to be run when commanded at the client side
     * despite there is no real resource enlisted to the transaction at the server side.
     */
    @Test
    public void rollbackWithNoXAResourceEnlistment () throws Exception {
        InitialContext initCtx = getInitialContext(InitialContextLookupType.REMOTE_HTTP, TESTSUITE_NODE0);
        Result<Integer> result = null;
        UserTransaction txn = null;

        try {
            Incrementor bean  = getStatelessRemoteBean(initCtx, StatelessTransactionNoResourceBean.class);
            txn = getUserTransaction(initCtx);

            txn.begin();

            result = bean.increment();
            Assert.assertEquals(1, result.getValue().intValue());

            txn.rollback();
        } catch (Exception e) {
            if(txn != null) txn.rollback();
            throw e;
        } finally {
            initCtx.close();
        }

        if(result.getNode().equals(NODE_1)) {
            Assert.assertFalse("Transaction was rolled-back at " + NODE_1 +
                    "Synchronization.beforeCompletion can't be called", checkerNode0.isSynchronizedBefore());
            Assert.assertEquals("Transaction was rolled-back at " + NODE_1 +
                    ", after synchronization callback needs to be called", 1, checkerNode0.countSynchronizedAfterRolledBack());
            Assert.assertFalse("Expected no beforeCompletion to be run at " + NODE_2, checkerNode1.isSynchronizedBefore());
            Assert.assertFalse("Expected no afterCompletion to be run at " + NODE_2, checkerNode1.isSynchronizedAfter());
        } else if (result.getNode().equals(NODE_2)) {
            Assert.assertFalse("Expected no beforeCompletion to be run at " + NODE_1, checkerNode0.isSynchronizedBefore());
            Assert.assertFalse("Expected no afterCompletion to be run at " + NODE_1, checkerNode0.isSynchronizedAfter());
            Assert.assertFalse("Transaction was rolled-back at " + NODE_2 +
                    "Synchronization.beforeCompletion can't be called", checkerNode1.isSynchronizedBefore());
            Assert.assertEquals("Transaction was rolled-back at " + NODE_2 +
                    ", after synchronization callback needs to be called", 1, checkerNode1.countSynchronizedAfterRolledBack());
        } else {
            Assert.fail(String.format("Expecting one of the nodes [%s,%s] should be hit "
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
    public void affinityNodeFailure () throws Exception {
        InitialContext initCtx = getInitialContext(InitialContextLookupType.REMOTE_HTTP, TESTSUITE_NODE0);
        String targetNode = null;
        UserTransaction txn = null;

        try {
            txn = getUserTransaction(initCtx);

            txn.begin();

            Incrementor bean  = getStatelessRemoteBean(initCtx, StatelessTransactionBean.class);
            Result<Integer> result = bean.increment();
            int count = 1;
            Assert.assertEquals(count++, result.getValue().intValue());

            targetNode = result.getNode();

            undeploy(this.findDeployment(targetNode));

            try {
                result = bean.increment();
                Assert.fail("Expected a NoSuchEJBException as transaction affinity needs to be maintained");
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
