/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.remote.client.api.tx;

import com.arjuna.ats.internal.jbossatx.jta.jca.XATerminator;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.ContextTransactionSynchronizationRegistry;
import org.wildfly.transaction.client.LocalTransactionContext;
import org.wildfly.transaction.client.provider.jboss.JBossLocalTransactionProvider;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.net.URI;
import java.net.URISyntaxException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

/**
 * @author Jaikiran Pai
 * @author Flavia Rainone
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({GracefulTxnShutdownSetup.class})
public class HTTPEJBClientXidTransactionTestCase {

    private static final Logger logger = Logger.getLogger(HTTPEJBClientXidTransactionTestCase.class);

    private static final String APP_NAME = "ejb-remote-client-api-xidtx-test";

    private static final String MODULE_NAME = "ejb";

    private static TransactionManager txManager;

    private static TransactionSynchronizationRegistry txSyncRegistry;

    @ArquillianResource
    private ManagementClient managementClient;

    /**
     * Creates an EJB deployment
     *
     * @return
     */
    @Deployment
    public static Archive<?> createDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(HTTPEJBClientXidTransactionTestCase.class.getPackage());
        jar.addAsManifestResource(HTTPEJBClientXidTransactionTestCase.class.getPackage(), "persistence.xml", "persistence.xml");

        ear.addAsModule(jar);

        return ear;
    }


    private URI getHttpUri() throws URISyntaxException {
        URI webUri = managementClient.getWebUri();
        return new URI("http", webUri.getUserInfo(), webUri.getHost(), webUri.getPort(), "/wildfly-services", "", "");
    }

    private static AuthenticationContext old;

    /**
     * Create and setup the remoting connection
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeTestClass() throws Exception {
        // These system properties are required or else we end up picking up JTS transaction manager,
        // which is not what we want
        final JTAEnvironmentBean jtaEnvironmentBean = jtaPropertyManager.getJTAEnvironmentBean();
        jtaEnvironmentBean.setTransactionManagerClassName(TransactionManagerImple.class.getName());
        jtaEnvironmentBean.setTransactionSynchronizationRegistryClassName(TransactionSynchronizationRegistryImple.class.getName());
        final TransactionManager narayanaTm = jtaEnvironmentBean.getTransactionManager();
        final TransactionSynchronizationRegistry narayanaTsr = jtaEnvironmentBean.getTransactionSynchronizationRegistry();
        final XATerminator xat = new XATerminator();
        final JBossLocalTransactionProvider.Builder builder = JBossLocalTransactionProvider.builder();
        builder.setXATerminator(xat).setExtendedJBossXATerminator(xat);
        builder.setTransactionManager(narayanaTm);
        builder.setTransactionSynchronizationRegistry(narayanaTsr);
        LocalTransactionContext.getContextManager().setGlobalDefault(new LocalTransactionContext(builder.build()));
        txManager = ContextTransactionManager.getInstance();
        txSyncRegistry = ContextTransactionSynchronizationRegistry.getInstance();
        // setup the tx manager and tx sync registry


        AuthenticationConfiguration config = AuthenticationConfiguration.EMPTY.useName("user1").usePassword("password1");
        AuthenticationContext context = AuthenticationContext.empty().with(MatchRule.ALL, config);
        old = AuthenticationContext.captureCurrent();
        AuthenticationContext.getContextManager().setGlobalDefault(context);
    }

    @AfterClass
    public static void after() {
        AuthenticationContext.getContextManager().setGlobalDefault(old);
    }

    /**
     * Tests that a CMT stateless bean method, with Mandatory tx attribute, invocation works as expected
     * when the transaction is remotely started on the client side using a client side transaction manager
     *
     * @throws Exception
     */
    @Test
    public void testSLSBMandatoryTx() throws Exception {
        final StatelessEJBLocator<CMTRemote> cmtRemoteBeanLocator = new StatelessEJBLocator<CMTRemote>(CMTRemote.class, APP_NAME, MODULE_NAME, CMTBean.class.getSimpleName(), "", Affinity.forUri(getHttpUri()));
        final CMTRemote cmtRemoteBean = EJBClient.createProxy(cmtRemoteBeanLocator);

        // start the transaction
        txManager.begin();
        // invoke the bean
        cmtRemoteBean.mandatoryTxOp();
        // end the tx
        txManager.commit();
    }

    /**
     * Tests various transaction scenarios managed on the client side via the client side transaction manager
     *
     * @throws Exception
     */
    @Test
    public void testClientTransactionManagement() throws Exception {
        final StatelessEJBLocator<RemoteBatch> batchBeanLocator = new StatelessEJBLocator<RemoteBatch>(RemoteBatch.class, APP_NAME, MODULE_NAME, BatchCreationBean.class.getSimpleName(), "", Affinity.forUri(getHttpUri()));
        final RemoteBatch batchBean = EJBClient.createProxy(batchBeanLocator);

        final StatelessEJBLocator<BatchRetriever> batchRetrieverLocator = new StatelessEJBLocator<BatchRetriever>(BatchRetriever.class, APP_NAME, MODULE_NAME, BatchFetchingBean.class.getSimpleName(), "", Affinity.forUri(getHttpUri()));
        final BatchRetriever batchRetriever = EJBClient.createProxy(batchRetrieverLocator);

        final String batchName = "Simple Batch";
        // create a batch
        txManager.begin();
        try {
            batchBean.createBatch(batchName);
        } catch (Exception e) {
            txManager.rollback();
            throw e;
        }
        txManager.commit();

        // fetch the batch and make sure it contains the right state
        final Batch batchAfterCreation = batchRetriever.fetchBatch(batchName);
        Assert.assertNotNull("Batch was null after creation", batchAfterCreation);
        Assert.assertNull("Unexpected steps in batch, after creation", batchAfterCreation.getStepNames());

        // add step1 to the batch
        final String step1 = "Simple step1";
        txManager.begin();
        try {
            batchBean.step1(batchName, step1);
        } catch (Exception e) {
            txManager.rollback();
            throw e;
        }
        txManager.commit();

        String successFullyCompletedSteps = step1;

        // fetch the batch and make sure it contains the right state
        final Batch batchAfterStep1 = batchRetriever.fetchBatch(batchName);
        Assert.assertNotNull("Batch after step1 was null", batchAfterStep1);
        Assert.assertEquals("Unexpected steps in batch, after step1", successFullyCompletedSteps, batchAfterStep1.getStepNames());


        // now add a failing step2
        final String appExceptionStep2 = "App exception Step 2";
        txManager.begin();
        try {
            batchBean.appExceptionFailingStep2(batchName, appExceptionStep2);
            Assert.fail("Expected an application exception");
        } catch (SimpleAppException sae) {
            // expected
            txManager.rollback();
        }

        final Batch batchAfterAppExceptionStep2 = batchRetriever.fetchBatch(batchName);
        Assert.assertNotNull("Batch after app exception step2 was null", batchAfterAppExceptionStep2);
        Assert.assertEquals("Unexpected steps in batch, after app exception step2", successFullyCompletedSteps, batchAfterAppExceptionStep2.getStepNames());

        // now add a successful step2
        final String step2 = "Simple Step 2";
        txManager.begin();
        try {
            batchBean.successfulStep2(batchName, step2);
        } catch (Exception e) {
            txManager.rollback();
            throw e;
        }
        // don't yet commit and try and retrieve the batch
        final Batch batchAfterStep2BeforeCommit = batchRetriever.fetchBatch(batchName);
        Assert.assertNotNull("Batch after step2, before commit was null", batchAfterStep2BeforeCommit);
        Assert.assertEquals("Unexpected steps in batch, after step2 before commit", successFullyCompletedSteps, batchAfterStep2BeforeCommit.getStepNames());

        // now commit
        txManager.commit();
        // keep track of successfully completely steps
        successFullyCompletedSteps = successFullyCompletedSteps + "," + step2;

        // now retrieve and check the batch
        final Batch batchAfterStep2 = batchRetriever.fetchBatch(batchName);
        Assert.assertNotNull("Batch after step2 was null", batchAfterStep2);
        Assert.assertEquals("Unexpected steps in batch, after step2", successFullyCompletedSteps, batchAfterStep2.getStepNames());

        // now add independent Step3 (i.e. the bean method has a REQUIRES_NEW semantics, so that the
        // client side tx doesn't play a role)
        final String step3 = "Simple Step 3";
        txManager.begin();
        batchBean.independentStep3(batchName, step3);
        // rollback (but it shouldn't end up rolling back step3 because that was done in server side independent tx)
        txManager.rollback();
        // keep track of successfully completely steps
        successFullyCompletedSteps = successFullyCompletedSteps + "," + step3;

        // now retrieve and check the batch
        final Batch batchAfterStep3 = batchRetriever.fetchBatch(batchName);
        Assert.assertNotNull("Batch after step3 was null", batchAfterStep3);
        Assert.assertEquals("Unexpected steps in batch, after step3", successFullyCompletedSteps, batchAfterStep3.getStepNames());

        // now add step4 but don't commit
        final String step4 = "Simple Step 4";
        txManager.begin();
        batchBean.step4(batchName, step4);

        // now add a system exception throwing step
        final String sysExceptionStep2 = "Sys exception step2";
        try {
            batchBean.systemExceptionFailingStep2(batchName, sysExceptionStep2);
            Assert.fail("Expected a system exception");
        } catch (Exception e) {
            // expected
            //Assert.assertEquals("Unexpected transaction state", Status.STATUS_ROLLEDBACK, userTransaction.getStatus());
            txManager.rollback();
        }

        // now retrieve and check the batch
        final Batch batchAfterSysException = batchRetriever.fetchBatch(batchName);
        Assert.assertNotNull("Batch after system exception was null", batchAfterSysException);
        Assert.assertEquals("Unexpected steps in batch, after system exception", successFullyCompletedSteps, batchAfterSysException.getStepNames());

    }


    /**
     * Calls for a preexistent transaction are allowed and calls for a non-preexistent transaction are not allowed
     * on server suspension.
     *
     * @throws Exception
     */
    @Test
    public void testServerSuspension() throws Exception {
        final StatelessEJBLocator<CMTRemote> cmtRemoteBeanLocator = new StatelessEJBLocator<CMTRemote>(
                CMTRemote.class, APP_NAME, MODULE_NAME, CMTBean.class.getSimpleName(), "", Affinity.forUri(getHttpUri()));
        final CMTRemote cmtRemoteBean = EJBClient.createProxy(cmtRemoteBeanLocator);

        // begin a transaction, and make sure that the server now works normally
        txManager.begin();
        try {
            // invoke the bean
            cmtRemoteBean.mandatoryTxOp();
        } finally {
            // end the tx
            txManager.commit();
        }

        // begin the transaction
        txManager.begin();
        try {
            // invoke the bean
            cmtRemoteBean.mandatoryTxOp();

            ModelNode op = new ModelNode();
            op.get(OP).set("suspend");
            managementClient.getControllerClient().execute(op);

            txManager.commit();
        } catch (Exception e) {
            try {
                txManager.rollback();
            } catch (Exception exc) {
            }
            throw e;
        } finally {
            // resume server
            ModelNode op = new ModelNode();
            op.get(OP).set("resume");
            managementClient.getControllerClient().execute(op);
        }

        try {
            // begin a transaction
            txManager.begin();

            long fin = System.currentTimeMillis() + TimeoutUtil.adjust(5000);
            while (true) {
                try {
                    // can invoke bean
                    cmtRemoteBean.mandatoryTxOp();
                    break;
                } catch (Exception e) {
                    if (System.currentTimeMillis() > fin) {
                        throw e;
                    }
                }
                Thread.sleep(300);
            }

            // suspend server
            ModelNode op = new ModelNode();
            op.get(OP).set("suspend");
            managementClient.getControllerClient().execute(op);

            // FIXME check with remoting team why this transaction is not recognized as active in EJBSuspendHandlerService
            // can continue invoking bean with current transaction
            //cmtRemoteBean.mandatoryTxOp();
        } catch (Exception e) {
            e.printStackTrace();
            // resume server
            ModelNode op = new ModelNode();
            op.get(OP).set("resume");
            managementClient.getControllerClient().execute(op);
            throw e;
        } finally {
            // rollback current transaction
            txManager.commit();
        }

        // still cannot begin a new transaction
        txManager.begin();
        try {
            cmtRemoteBean.mandatoryTxOp();
            Assert.fail("Exception expected, server is shutdown");
        } catch (Exception expected) {
            // expected
        } finally {
            txManager.rollback();
        }

        // resume server
        ModelNode op = new ModelNode();
        op.get(OP).set("resume");
        managementClient.getControllerClient().execute(op);

        try {
            // begin a transaction, and make sure that the server now works normally
            txManager.begin();
            long fin = System.currentTimeMillis() + TimeoutUtil.adjust(5000);
            while (true) {
                try {
                    // can invoke bean
                    cmtRemoteBean.mandatoryTxOp();
                    break;
                } catch (Exception e) {
                    if (System.currentTimeMillis() > fin) {
                        throw e;
                    }
                }
                Thread.sleep(300);
            }
            // end the tx
            txManager.commit();
        } catch (Exception e) {
            txManager.rollback();
            throw e;
        }
    }
}
