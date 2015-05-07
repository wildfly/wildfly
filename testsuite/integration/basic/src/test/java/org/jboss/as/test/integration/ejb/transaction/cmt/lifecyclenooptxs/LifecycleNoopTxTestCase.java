/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.transaction.cmt.lifecyclenooptxs;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.RetryTaskExecutor;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.IOException;
import java.util.Hashtable;
import java.util.concurrent.Callable;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 * This test covers no-op transactions on missing lifecycle enterprise bean methods.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({LifecycleNoopTxTestCase.TxStatisticsSetupTask.class,
        LifecycleNoopTxTestCase.PassivationTestCaseSetup.class})
public class LifecycleNoopTxTestCase extends ContainerResourceMgmtTestBase {

    private static final String APP_NAME = "";
    private static final String DISTINCT_NAME = "";
    private static final String MODULE_NAME = "lifecycle-noop-tx-test";

    private static Context context;

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(LifecycleNoopTxTestCase.class.getPackage());
        return ejbJar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(props);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void testStateless() throws Exception {

        // check initial stats
        int stats = readNumberOfCommittedTxs();

        // call a SLSB without any lifecycle methods defined
        final StatelessRemoteView stateless = (StatelessRemoteView) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + StatelessSimple.class.getSimpleName() + "!" + StatelessRemoteView.class.getName());
        stateless.doNothing();
        int newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 1", stats + 1, newStats);
        stats = newStats;

        // call a SLSB with PostConstruct method
        final StatelessRemoteView statelessPostConstruct = (StatelessRemoteView) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + StatelessPostConstruct.class.getSimpleName() + "!" + StatelessRemoteView.class.getName());
        statelessPostConstruct.doNothing();
        newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 2", stats + 2, newStats);
        stats = newStats;

        // call a SLSB with PreDestroy method
        final StatelessRemoteView statelessPreDestroy = (StatelessRemoteView) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + StatelessPreDestroy.class.getSimpleName() + "!" + StatelessRemoteView.class.getName());
        statelessPreDestroy.doNothing();
        newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 2", stats + 2, newStats);

    }

    @Test
    public void testSingleton() throws Exception {

        // check initial stats
        int stats = readNumberOfCommittedTxs();

        // call a Singleton without any lifecycle methods defined
        final StatelessRemoteView singleton = (StatelessRemoteView) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + SingletonSimple.class.getSimpleName() + "!" + StatelessRemoteView.class.getName());
        singleton.doNothing();
        int newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 2", stats + 2, newStats);
        stats = newStats;

        // call a Singleton with PostConstruct method
        final StatelessRemoteView singletonPostConstruct = (StatelessRemoteView) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + SingletonPostConstruct.class.getSimpleName() + "!" + StatelessRemoteView.class.getName());
        singletonPostConstruct.doNothing();
        newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 2", stats + 2, newStats);

    }

    @Test
    public void testSingletonIntercepted() throws Exception {

        // check initial stats
        int stats = readNumberOfCommittedTxs();

        // call a Singleton without any lifecycle methods defined but intercepted
        // with an interceptor containing its own PostConstruct and PreDestroy
        final StatelessRemoteView singleton = (StatelessRemoteView) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + SingletonSimple.class.getSimpleName() + "!" + StatelessRemoteView.class.getName());
        singleton.doNothing();
        int newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 1", stats + 1, newStats);

    }

    @Test
    public void testStateful() throws Exception {

        // check initial stats
        int stats = readNumberOfCommittedTxs();

        // call a SFSB without any lifecycle methods defined
        final StatefulRemoteView stateful = (StatefulRemoteView) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + StatefulSimple.class.getSimpleName() + "!" + StatefulRemoteView.class.getName() + "?stateful");
        stateful.doNothing();
        stateful.remove();
        int newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 2", stats + 2, newStats);
        stats = newStats;

        // call a SFSB with PostConstruct method
        final StatefulRemoteView statefulPostConstruct = (StatefulRemoteView) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + StatefulPostConstruct.class.getSimpleName() + "!" + StatefulRemoteView.class.getName() + "?stateful");
        statefulPostConstruct.doNothing();
        newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 2", stats + 2, newStats);
        stats = newStats;

        statefulPostConstruct.doNothing();
        statefulPostConstruct.remove();
        newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 2", stats + 2, newStats);
        stats = newStats;

        // call a SFSB with PreDestroy method
        StatefulRemoteView statefulPreDestroy = (StatefulRemoteView) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + StatefulPreDestroy.class.getSimpleName() + "!" + StatefulRemoteView.class.getName() + "?stateful");
        statefulPreDestroy.doNothing();
        newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 1", stats + 1, newStats);
        stats = newStats;

        statefulPreDestroy.remove();
        newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 2", stats + 2, newStats);

    }

    @Test
    public void testStatefulIntercepted() throws Exception {

        // check initial stats
        int stats = readNumberOfCommittedTxs();

        // call a SFSB without any lifecycle methods defined but intercepted
        // with an interceptor containing its own PostConstruct and PreDestroy
        final StatefulRemoteView stateful = (StatefulRemoteView) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + StatefulIntercepted.class.getSimpleName() + "!" + StatefulRemoteView.class.getName() + "?stateful");
        stateful.doNothing();
        int newStats = readNumberOfCommittedTxs();
        // tx number should be increased by 2 since both PostConstruct methods of the bean and the interceptor should run within the same tx
        Assert.assertEquals("number of committed txs should be increased by 2", stats + 2, newStats);

    }

    @Test
    public void testStatefulPassivated() throws Exception {

        // check initial stats
        int stats = readNumberOfCommittedTxs();

        // call a SFSB with PrePassivate and PostActivate methods
        final StatefulRemoteView simplePassivated = (StatefulRemoteView) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + StatefulPassivated.class.getSimpleName() + "!" + StatefulRemoteView.class.getName() + "?stateful");
        simplePassivated.doNothing();
        int newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 1", stats + 1, newStats);
        stats = newStats;

        // get another instance to force the previous one to be passivated
        final StatefulRemoteView simplePassivated2 = (StatefulRemoteView) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + StatefulPassivated.class.getSimpleName() + "!" + StatefulRemoteView.class.getName() + "?stateful");
        simplePassivated2.doNothing();
        newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 1", stats + 2, newStats);
        stats = newStats;

        simplePassivated2.remove();
        newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 1", stats + 1, newStats);
        stats = newStats;

        simplePassivated.remove();
        newStats = readNumberOfCommittedTxs();
        Assert.assertEquals("number of committed txs should be increased by 1", stats + 2, newStats);

    }

    private int readNumberOfCommittedTxs() throws IOException, MgmtOperationException {
        final ModelNode op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).set(getTxBaseAddress());
        op.get(NAME).set("number-of-committed-transactions");

        final ModelNode result = executeOperation(op);
        return result.asInt();
    }

    private static ModelNode getTxBaseAddress() {
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "transactions");
        address.protect();
        return address;
    }

    static class TxStatisticsSetupTask extends AbstractMgmtServerSetupTask {

        private boolean origTxStatValue;

        @Override
        protected void doSetup(final ManagementClient managementClient) throws Exception {
            final ModelNode op = new ModelNode();
            op.get(OP).set(READ_ATTRIBUTE_OPERATION);
            op.get(OP_ADDR).set(getTxBaseAddress());
            op.get(NAME).set("statistics-enabled");
            final ModelNode result = executeOperation(op);
            origTxStatValue = result.asBoolean();

            if (!origTxStatValue) {
                final ModelNode writeOp = new ModelNode();
                writeOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
                writeOp.get(OP_ADDR).set(getTxBaseAddress());
                writeOp.get(NAME).set("statistics-enabled");
                writeOp.get(VALUE).set("true");
                executeOperation(writeOp);

                // reload the server
                reload(managementClient);
            }
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            if (!origTxStatValue) {
                final ModelNode op = new ModelNode();
                op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
                op.get(OP_ADDR).set(getTxBaseAddress());
                op.get(NAME).set("statistics-enabled");
                op.get(VALUE).set(origTxStatValue);
                executeOperation(op);
            }
        }

        /**
         * Provides reload operation on server
         *
         * @throws Exception
         */
        private void reload(final ManagementClient managementClient) throws Exception {
            final ModelNode op = createOpNode(null, "reload");
            executeOperation(op);

            RetryTaskExecutor<Boolean> rte = new RetryTaskExecutor<>();
            rte.retryTask(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    if (managementClient.isServerInRunningState()) {
                        return true;
                    }
                    throw new Exception("Server is not up yet");
                }
            });
        }
    }

    static class PassivationTestCaseSetup extends AbstractMgmtServerSetupTask {

        private static ModelNode getPassivationStoreAddress() {
            final ModelNode address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("passivation-store", "infinispan");
            address.protect();
            return address;
        }

        @Override
        protected void doSetup(final ManagementClient managementClient) throws Exception {
            // update the file passivation store attributes
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            operation.get(OP_ADDR).set(getPassivationStoreAddress());
            operation.get(NAME).set("max-size");
            operation.get(VALUE).set(1);
            executeOperation(operation);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            // reset the file passivation store attributes
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
            operation.get(OP_ADDR).set(getPassivationStoreAddress());
            operation.get(NAME).set("max-size");
            executeOperation(operation);
        }
    }

}
